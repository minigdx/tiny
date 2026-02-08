import { VirtualFileSystem, VFSEntry } from '../vfs';

export type FileTreeAction = 'open' | 'delete' | 'rename';

export interface FileTreeEvent {
  action: FileTreeAction;
  path: string;
}

type FileTreeListener = (event: FileTreeEvent) => void;

interface TreeNode {
  name: string;
  path: string;
  isFolder: boolean;
  children: TreeNode[];
  entry?: VFSEntry;
}

/**
 * Renders the file tree in the sidebar from the VFS contents.
 * Supports folder collapsing, file selection, creation, renaming, and deletion.
 */
export class FileTree {
  private container: HTMLElement;
  private vfs: VirtualFileSystem;
  private listeners: FileTreeListener[] = [];
  private activePath: string | null = null;
  private collapsedFolders = new Set<string>();
  private unsubscribeVFS: (() => void) | null = null;

  constructor(container: HTMLElement, vfs: VirtualFileSystem) {
    this.container = container;
    this.vfs = vfs;
    this.unsubscribeVFS = vfs.onChange(() => this.render());
    this.render();
  }

  onAction(callback: FileTreeListener): () => void {
    this.listeners.push(callback);
    return () => {
      const idx = this.listeners.indexOf(callback);
      if (idx >= 0) this.listeners.splice(idx, 1);
    };
  }

  setActive(path: string | null): void {
    this.activePath = path;
    this.render();
  }

  /** Show an inline input to create a new file. */
  showNewFileInput(parentFolder: string = ''): void {
    const input = document.createElement('input');
    input.className = 'inline-input';
    input.placeholder = 'filename.lua';
    input.type = 'text';

    const wrapper = document.createElement('div');
    wrapper.style.padding = '2px 8px 2px 20px';
    wrapper.appendChild(input);

    this.container.appendChild(wrapper);
    input.focus();

    const commit = () => {
      const name = input.value.trim();
      wrapper.remove();
      if (name) {
        const path = parentFolder ? `${parentFolder}/${name}` : name;
        const ext = name.split('.').pop()?.toLowerCase() ?? '';
        if (ext === 'lua') {
          this.vfs.put(path, { type: 'lua', content: `-- ${name}\n` });
        } else if (['png', 'jpg', 'gif', 'bmp'].includes(ext)) {
          this.vfs.put(path, { type: 'image' });
        } else if (['json', 'ldtk', 'map'].includes(ext)) {
          this.vfs.put(path, { type: 'map', content: '{}' });
        } else {
          this.vfs.put(path, { type: 'data', content: '' });
        }
        this.emit({ action: 'open', path });
      }
    };

    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') commit();
      if (e.key === 'Escape') wrapper.remove();
    });
    input.addEventListener('blur', commit);
  }

  /** Show inline rename input for a file. */
  showRenameInput(oldPath: string): void {
    const parts = oldPath.split('/');
    const oldName = parts.pop()!;
    const folder = parts.join('/');

    const fileEl = this.container.querySelector(`[data-path="${CSS.escape(oldPath)}"]`);
    if (!fileEl) return;

    const nameEl = fileEl.querySelector('.file-tree-name');
    if (!nameEl) return;

    const input = document.createElement('input');
    input.className = 'inline-input';
    input.value = oldName;
    input.type = 'text';

    nameEl.textContent = '';
    nameEl.appendChild(input);
    input.focus();
    input.select();

    const commit = () => {
      const newName = input.value.trim();
      if (newName && newName !== oldName) {
        const newPath = folder ? `${folder}/${newName}` : newName;
        this.vfs.rename(oldPath, newPath);
        this.emit({ action: 'rename', path: newPath });
      } else {
        this.render();
      }
    };

    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') commit();
      if (e.key === 'Escape') this.render();
    });
    input.addEventListener('blur', commit);
  }

  destroy(): void {
    this.unsubscribeVFS?.();
    this.container.innerHTML = '';
  }

  private render(): void {
    this.container.innerHTML = '';
    const entries = this.vfs.list();
    const tree = this.buildTree(entries);
    this.renderNodes(tree, this.container);
  }

  private buildTree(entries: VFSEntry[]): TreeNode[] {
    const root: TreeNode[] = [];
    const folderMap = new Map<string, TreeNode>();

    const getOrCreateFolder = (folderPath: string): TreeNode => {
      if (folderMap.has(folderPath)) return folderMap.get(folderPath)!;

      const parts = folderPath.split('/');
      const name = parts[parts.length - 1];
      const node: TreeNode = { name, path: folderPath, isFolder: true, children: [] };
      folderMap.set(folderPath, node);

      if (parts.length === 1) {
        root.push(node);
      } else {
        const parentPath = parts.slice(0, -1).join('/');
        const parent = getOrCreateFolder(parentPath);
        parent.children.push(node);
      }

      return node;
    };

    for (const entry of entries) {
      const parts = entry.path.split('/');
      if (parts.length > 1) {
        const folderPath = parts.slice(0, -1).join('/');
        const folder = getOrCreateFolder(folderPath);
        folder.children.push({
          name: parts[parts.length - 1],
          path: entry.path,
          isFolder: false,
          children: [],
          entry,
        });
      } else {
        root.push({
          name: entry.path,
          path: entry.path,
          isFolder: false,
          children: [],
          entry,
        });
      }
    }

    // Sort: folders first, then files, alphabetically
    const sort = (nodes: TreeNode[]) => {
      nodes.sort((a, b) => {
        if (a.isFolder && !b.isFolder) return -1;
        if (!a.isFolder && b.isFolder) return 1;
        return a.name.localeCompare(b.name);
      });
      for (const n of nodes) {
        if (n.isFolder) sort(n.children);
      }
    };
    sort(root);
    return root;
  }

  private renderNodes(nodes: TreeNode[], parent: HTMLElement): void {
    for (const node of nodes) {
      if (node.isFolder) {
        this.renderFolder(node, parent);
      } else {
        this.renderFile(node, parent);
      }
    }
  }

  private renderFolder(node: TreeNode, parent: HTMLElement): void {
    const isCollapsed = this.collapsedFolders.has(node.path);

    const folder = document.createElement('div');
    folder.className = `file-tree-folder${isCollapsed ? ' collapsed' : ''}`;

    const header = document.createElement('div');
    header.className = 'file-tree-folder-header';
    header.innerHTML = `
      <span class="file-tree-folder-arrow">&#9660;</span>
      <span class="file-tree-icon">&#128193;</span>
      <span class="file-tree-name">${this.escapeHtml(node.name)}</span>
    `;
    header.addEventListener('click', () => {
      if (this.collapsedFolders.has(node.path)) {
        this.collapsedFolders.delete(node.path);
      } else {
        this.collapsedFolders.add(node.path);
      }
      this.render();
    });
    folder.appendChild(header);

    const children = document.createElement('div');
    children.className = 'file-tree-folder-children';
    this.renderNodes(node.children, children);
    folder.appendChild(children);

    parent.appendChild(folder);
  }

  private renderFile(node: TreeNode, parent: HTMLElement): void {
    const el = document.createElement('div');
    el.className = `file-tree-file${node.path === this.activePath ? ' active' : ''}`;
    el.setAttribute('data-path', node.path);

    const icon = this.getFileIcon(node.entry?.type);

    el.innerHTML = `
      <span class="file-tree-icon">${icon}</span>
      <span class="file-tree-name">${this.escapeHtml(node.name)}</span>
      <span class="file-tree-actions">
        <button class="file-tree-action" data-action="rename" title="Rename">&#9998;</button>
        <button class="file-tree-action" data-action="delete" title="Delete">&#10005;</button>
      </span>
    `;

    el.addEventListener('click', (e) => {
      const target = e.target as HTMLElement;
      const action = target.getAttribute('data-action');
      if (action === 'delete') {
        if (confirm(`Delete "${node.path}"?`)) {
          this.vfs.delete(node.path);
          this.emit({ action: 'delete', path: node.path });
        }
      } else if (action === 'rename') {
        this.showRenameInput(node.path);
      } else {
        this.emit({ action: 'open', path: node.path });
      }
    });

    el.addEventListener('contextmenu', (e) => {
      e.preventDefault();
      this.showContextMenu(e, node.path);
    });

    parent.appendChild(el);
  }

  private showContextMenu(e: MouseEvent, path: string): void {
    // Remove any existing context menu
    document.querySelectorAll('.context-menu').forEach(m => m.remove());

    const menu = document.createElement('div');
    menu.className = 'context-menu';
    menu.style.left = `${e.clientX}px`;
    menu.style.top = `${e.clientY}px`;

    const items = [
      { label: 'Open', action: () => this.emit({ action: 'open', path }) },
      { label: 'Rename', action: () => this.showRenameInput(path) },
      { label: 'Delete', action: () => {
        if (confirm(`Delete "${path}"?`)) {
          this.vfs.delete(path);
          this.emit({ action: 'delete', path });
        }
      }},
    ];

    for (const item of items) {
      const el = document.createElement('div');
      el.className = 'context-menu-item';
      el.textContent = item.label;
      el.addEventListener('click', () => {
        menu.remove();
        item.action();
      });
      menu.appendChild(el);
    }

    document.body.appendChild(menu);

    const closeMenu = () => {
      menu.remove();
      document.removeEventListener('click', closeMenu);
    };
    // Delay to avoid the current click closing it
    setTimeout(() => document.addEventListener('click', closeMenu), 0);
  }

  private getFileIcon(type?: string): string {
    switch (type) {
      case 'lua': return '&#128220;';
      case 'image': return '&#128444;';
      case 'map': return '&#128506;';
      case 'config': return '&#9881;';
      default: return '&#128196;';
    }
  }

  private escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  private emit(event: FileTreeEvent): void {
    for (const listener of this.listeners) {
      listener(event);
    }
  }
}
