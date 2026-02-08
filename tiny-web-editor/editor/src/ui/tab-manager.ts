import { EditorView, keymap, drawSelection, highlightActiveLine, lineNumbers } from '@codemirror/view';
import { EditorState, Extension } from '@codemirror/state';
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
import { StreamLanguage } from '@codemirror/language';
import { lua } from '@codemirror/legacy-modes/mode/lua';
import { json } from '@codemirror/legacy-modes/mode/javascript';
import { searchKeymap } from '@codemirror/search';
import { VirtualFileSystem, VFSEntry } from '../vfs';

/** Retro dark theme for CodeMirror matching the editor palette. */
import { EditorView as EV } from '@codemirror/view';

const retroTheme = EV.theme({
  '&': {
    backgroundColor: '#1a1a26',
    color: '#c8c8e0',
    fontSize: '13px',
    height: '100%',
  },
  '.cm-content': {
    fontFamily: '"JetBrains Mono", "Fira Code", "Source Code Pro", "Consolas", monospace',
    caretColor: '#6a6aff',
  },
  '.cm-cursor': {
    borderLeftColor: '#6a6aff',
  },
  '&.cm-focused .cm-selectionBackground, .cm-selectionBackground': {
    backgroundColor: '#3a3a5c !important',
  },
  '.cm-activeLine': {
    backgroundColor: '#24243a',
  },
  '.cm-gutters': {
    backgroundColor: '#12121a',
    color: '#8080a0',
    borderRight: '1px solid #3a3a5c',
  },
  '.cm-activeLineGutter': {
    backgroundColor: '#24243a',
    color: '#c8c8e0',
  },
  '.cm-matchingBracket': {
    backgroundColor: '#4a4abf40',
    outline: '1px solid #6a6aff',
  },
}, { dark: true });

/** Syntax highlighting colors */
import { HighlightStyle, syntaxHighlighting } from '@codemirror/language';
import { tags } from '@lezer/highlight';

const retroHighlight = syntaxHighlighting(HighlightStyle.define([
  { tag: tags.keyword, color: '#6a6aff' },
  { tag: tags.comment, color: '#6a8060', fontStyle: 'italic' },
  { tag: tags.string, color: '#50c878' },
  { tag: tags.number, color: '#f0a030' },
  { tag: tags.bool, color: '#f0a030' },
  { tag: tags.null, color: '#f0a030' },
  { tag: tags.operator, color: '#c8c8e0' },
  { tag: tags.variableName, color: '#e0c080' },
  { tag: tags.function(tags.variableName), color: '#80b0ff' },
  { tag: tags.definition(tags.variableName), color: '#e0c080' },
  { tag: tags.propertyName, color: '#e0c080' },
  { tag: tags.typeName, color: '#80b0ff' },
  { tag: tags.punctuation, color: '#8080a0' },
]));

interface Tab {
  path: string;
  state: EditorState;
  modified: boolean;
}

type TabEventType = 'change' | 'save' | 'close' | 'switch';

interface TabEvent {
  type: TabEventType;
  path: string;
}

type TabListener = (event: TabEvent) => void;

/**
 * Manages editor tabs, each backed by a CodeMirror 6 instance.
 * Synchronizes with the VFS for content.
 */
export class TabManager {
  private tabBarEl: HTMLElement;
  private editorContainerEl: HTMLElement;
  private vfs: VirtualFileSystem;
  private tabs: Tab[] = [];
  private activeTab: Tab | null = null;
  private view: EditorView | null = null;
  private listeners: TabListener[] = [];
  private debounceTimers = new Map<string, number>();

  constructor(tabBarEl: HTMLElement, editorContainerEl: HTMLElement, vfs: VirtualFileSystem) {
    this.tabBarEl = tabBarEl;
    this.editorContainerEl = editorContainerEl;
    this.vfs = vfs;
  }

  onEvent(callback: TabListener): () => void {
    this.listeners.push(callback);
    return () => {
      const idx = this.listeners.indexOf(callback);
      if (idx >= 0) this.listeners.splice(idx, 1);
    };
  }

  /** Open a file in a tab. If already open, switch to it. */
  open(path: string): void {
    const existing = this.tabs.find(t => t.path === path);
    if (existing) {
      this.switchTo(existing);
      return;
    }

    const entry = this.vfs.get(path);
    if (!entry) return;

    // Only open text files in the editor
    if (entry.type === 'image') {
      this.showImagePreview(entry);
      return;
    }

    const content = entry.content ?? '';
    const extensions = this.getExtensions(path);

    const state = EditorState.create({
      doc: content,
      extensions,
    });

    const tab: Tab = { path, state, modified: false };
    this.tabs.push(tab);
    this.switchTo(tab);
  }

  /** Close a tab by path. */
  close(path: string): void {
    const idx = this.tabs.findIndex(t => t.path === path);
    if (idx < 0) return;

    const tab = this.tabs[idx];
    // Save content before closing
    if (tab.modified) {
      this.saveTab(tab);
    }

    this.tabs.splice(idx, 1);

    if (this.activeTab === tab) {
      if (this.tabs.length > 0) {
        const newIdx = Math.min(idx, this.tabs.length - 1);
        this.switchTo(this.tabs[newIdx]);
      } else {
        this.activeTab = null;
        this.destroyView();
        this.showPlaceholder();
      }
    }

    this.renderTabBar();
    this.emit({ type: 'close', path });
  }

  /** Save the active tab's content to VFS. */
  saveActive(): void {
    if (this.activeTab) {
      this.saveTab(this.activeTab);
    }
  }

  /** Save all open tabs to VFS. */
  saveAll(): void {
    for (const tab of this.tabs) {
      if (tab.modified) {
        this.saveTab(tab);
      }
    }
  }

  /** Get the currently active file path. */
  getActivePath(): string | null {
    return this.activeTab?.path ?? null;
  }

  /** Navigate the editor to a specific line in a file. */
  goToLine(path: string, line: number): void {
    this.open(path);
    if (this.view) {
      const lineInfo = this.view.state.doc.line(Math.min(line, this.view.state.doc.lines));
      this.view.dispatch({
        selection: { anchor: lineInfo.from },
        scrollIntoView: true,
      });
      this.view.focus();
    }
  }

  /** Update editor content when VFS changes externally (e.g., import). */
  updateFromVFS(path: string): void {
    const tab = this.tabs.find(t => t.path === path);
    if (!tab) return;
    const entry = this.vfs.get(path);
    if (!entry || !entry.content) return;

    const newState = EditorState.create({
      doc: entry.content,
      extensions: this.getExtensions(path),
    });
    tab.state = newState;
    tab.modified = false;

    if (this.activeTab === tab && this.view) {
      this.view.setState(newState);
    }
    this.renderTabBar();
  }

  /** Handle tab for a file that was deleted from VFS. */
  handleFileDeleted(path: string): void {
    this.close(path);
  }

  /** Handle tab for a file that was renamed in VFS. */
  handleFileRenamed(oldPath: string, newPath: string): void {
    const tab = this.tabs.find(t => t.path === oldPath);
    if (tab) {
      tab.path = newPath;
      this.renderTabBar();
    }
  }

  destroy(): void {
    this.destroyView();
    this.tabs = [];
    this.activeTab = null;
    this.tabBarEl.innerHTML = '';
  }

  private switchTo(tab: Tab): void {
    // Save state of current view
    if (this.activeTab && this.view) {
      this.activeTab.state = this.view.state;
    }

    this.activeTab = tab;
    this.destroyView();

    // Remove placeholder
    const placeholder = this.editorContainerEl.querySelector('.editor-placeholder');
    placeholder?.remove();

    const imagePreview = this.editorContainerEl.querySelector('.image-preview');
    imagePreview?.remove();

    this.view = new EditorView({
      state: tab.state,
      parent: this.editorContainerEl,
    });

    this.renderTabBar();
    this.view.focus();
    this.emit({ type: 'switch', path: tab.path });
  }

  private saveTab(tab: Tab): void {
    if (this.activeTab === tab && this.view) {
      tab.state = this.view.state;
    }
    const content = tab.state.doc.toString();
    this.vfs.put(tab.path, { content });
    tab.modified = false;
    this.renderTabBar();
    this.emit({ type: 'save', path: tab.path });
  }

  private getExtensions(path: string): Extension[] {
    const ext = path.split('.').pop()?.toLowerCase() ?? '';
    let lang: Extension;
    if (ext === 'lua') {
      lang = StreamLanguage.define(lua);
    } else if (ext === 'json' || ext === 'ldtk') {
      lang = StreamLanguage.define(json);
    } else {
      lang = [];
    }

    return [
      lineNumbers(),
      highlightActiveLine(),
      drawSelection(),
      history(),
      keymap.of([
        ...defaultKeymap,
        ...historyKeymap,
        ...searchKeymap,
        {
          key: 'Mod-s',
          run: () => {
            this.saveActive();
            return true;
          },
        },
      ]),
      lang,
      retroTheme,
      retroHighlight,
      EditorView.updateListener.of((update) => {
        if (update.docChanged && this.activeTab) {
          this.activeTab.modified = true;
          this.renderTabBar();
          this.debounceSave(this.activeTab.path);
        }
      }),
    ];
  }

  private debounceSave(path: string): void {
    const existing = this.debounceTimers.get(path);
    if (existing) clearTimeout(existing);

    const timer = window.setTimeout(() => {
      this.debounceTimers.delete(path);
      const tab = this.tabs.find(t => t.path === path);
      if (tab) {
        this.emit({ type: 'change', path });
      }
    }, 300);
    this.debounceTimers.set(path, timer);
  }

  private renderTabBar(): void {
    this.tabBarEl.innerHTML = '';
    for (const tab of this.tabs) {
      const el = document.createElement('div');
      el.className = `tab${tab === this.activeTab ? ' active' : ''}`;

      const name = tab.path.split('/').pop() ?? tab.path;
      const modified = tab.modified ? '<span class="tab-modified">&bull;</span>' : '';

      el.innerHTML = `
        <span class="tab-label">${this.escapeHtml(name)}</span>
        ${modified}
        <button class="tab-close" title="Close">&times;</button>
      `;

      el.querySelector('.tab-label')?.addEventListener('click', () => {
        this.switchTo(tab);
      });

      el.querySelector('.tab-close')?.addEventListener('click', (e) => {
        e.stopPropagation();
        this.close(tab.path);
      });

      // Clicking the tab itself (not the close button) switches to it
      el.addEventListener('click', (e) => {
        if (!(e.target as HTMLElement).classList.contains('tab-close')) {
          this.switchTo(tab);
        }
      });

      this.tabBarEl.appendChild(el);
    }
  }

  private showPlaceholder(): void {
    const placeholder = document.createElement('div');
    placeholder.className = 'editor-placeholder';
    placeholder.textContent = 'Open a file from the sidebar to start editing';
    this.editorContainerEl.appendChild(placeholder);
  }

  private showImagePreview(entry: VFSEntry): void {
    // Remove any existing editor content
    if (this.activeTab && this.view) {
      this.activeTab.state = this.view.state;
    }
    this.destroyView();
    const placeholder = this.editorContainerEl.querySelector('.editor-placeholder');
    placeholder?.remove();
    const existing = this.editorContainerEl.querySelector('.image-preview');
    existing?.remove();

    const preview = document.createElement('div');
    preview.className = 'image-preview';

    if (entry.blob || entry.buffer) {
      const blob = entry.blob ?? new Blob([entry.buffer!]);
      const url = URL.createObjectURL(blob);
      const img = document.createElement('img');
      img.src = url;
      img.onload = () => {
        const info = document.createElement('div');
        info.className = 'image-preview-info';
        info.textContent = `${entry.path} — ${img.naturalWidth}x${img.naturalHeight} — ${this.formatSize(blob.size)}`;
        preview.appendChild(info);
      };
      preview.appendChild(img);
    } else {
      preview.textContent = 'No image data available';
    }

    this.editorContainerEl.appendChild(preview);
  }

  private formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private destroyView(): void {
    if (this.view) {
      this.view.destroy();
      this.view = null;
    }
  }

  private escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  private emit(event: TabEvent): void {
    for (const listener of this.listeners) {
      listener(event);
    }
  }
}
