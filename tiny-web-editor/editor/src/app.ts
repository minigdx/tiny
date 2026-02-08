import { VirtualFileSystem, createDemoVFS } from './vfs';
import { FileTree } from './ui/file-tree';
import { TabManager } from './ui/tab-manager';
import { ConsolePanel } from './ui/console-panel';
import { ProjectStorage, ProjectMeta } from './storage';
import { ZipManager } from './zip';
import { VFSBridge } from './bridge';

/**
 * Main application controller.
 * Wires together VFS, UI components, storage, and engine bridge.
 */
export class EditorApp {
  vfs: VirtualFileSystem;
  private fileTree!: FileTree;
  private tabManager!: TabManager;
  private consolePanel!: ConsolePanel;
  private storage: ProjectStorage;
  private zipManager: ZipManager;
  private bridge: VFSBridge;

  private currentProjectId: string | null = null;
  private currentProjectName: string = 'my-game';
  private autoSaveTimer: number | null = null;
  private isPlaying = false;

  constructor() {
    this.vfs = new VirtualFileSystem();
    this.storage = new ProjectStorage();
    this.zipManager = new ZipManager();
    this.bridge = new VFSBridge(this.vfs);
  }

  async init(): Promise<void> {
    // Open IndexedDB
    await this.storage.open();

    // Initialize UI components
    this.initFileTree();
    this.initTabManager();
    this.initConsolePanel();
    this.initToolbar();
    this.initDragAndDrop();
    this.initKeyboardShortcuts();
    this.initConsoleResize();

    // Install VFS bridge
    this.bridge.install();

    // Check for existing projects
    const projects = await this.storage.listProjects();
    if (projects.length > 0) {
      this.showWelcomeScreen(projects);
    } else {
      this.startNewProject();
    }
  }

  private initFileTree(): void {
    const container = document.getElementById('file-tree')!;
    this.fileTree = new FileTree(container, this.vfs);

    this.fileTree.onAction((event) => {
      switch (event.action) {
        case 'open':
          this.tabManager.open(event.path);
          this.fileTree.setActive(event.path);
          break;
        case 'delete':
          this.tabManager.handleFileDeleted(event.path);
          break;
        case 'rename':
          // File tree already handles VFS rename; just update active
          this.fileTree.setActive(event.path);
          break;
      }
    });

    document.getElementById('btn-new-file')!.addEventListener('click', () => {
      this.fileTree.showNewFileInput('scripts');
    });
  }

  private initTabManager(): void {
    const tabBar = document.getElementById('tab-bar')!;
    const editorContainer = document.getElementById('editor-container')!;
    this.tabManager = new TabManager(tabBar, editorContainer, this.vfs);

    this.tabManager.onEvent((event) => {
      switch (event.type) {
        case 'change':
          this.scheduleSave();
          break;
        case 'switch':
          this.fileTree.setActive(event.path);
          break;
      }
    });
  }

  private initConsolePanel(): void {
    const output = document.getElementById('console-output')!;
    const clearBtn = document.getElementById('btn-clear-console')!;
    this.consolePanel = new ConsolePanel(output, clearBtn);

    this.consolePanel.onClick((file, line) => {
      // Find the file in VFS - it might be a relative path like "scripts/main.lua"
      // or just "main.lua"
      let fullPath = file;
      if (!this.vfs.has(fullPath)) {
        // Try with scripts/ prefix
        const withScripts = `scripts/${file}`;
        if (this.vfs.has(withScripts)) {
          fullPath = withScripts;
        }
      }
      this.tabManager.goToLine(fullPath, line);
      this.fileTree.setActive(fullPath);
    });

    this.consolePanel.startCapture();
  }

  private initToolbar(): void {
    document.getElementById('btn-play')!.addEventListener('click', () => this.play());
    document.getElementById('btn-stop')!.addEventListener('click', () => this.stop());
    document.getElementById('btn-save')!.addEventListener('click', () => this.saveProject());
    document.getElementById('btn-export')!.addEventListener('click', () => this.exportZip());
    document.getElementById('btn-import')!.addEventListener('click', () => this.showImportDialog());
  }

  private initDragAndDrop(): void {
    const app = document.getElementById('app')!;
    let dragCounter = 0;

    app.addEventListener('dragenter', (e) => {
      e.preventDefault();
      dragCounter++;
      if (dragCounter === 1) {
        this.showDropOverlay();
      }
    });

    app.addEventListener('dragleave', () => {
      dragCounter--;
      if (dragCounter === 0) {
        this.hideDropOverlay();
      }
    });

    app.addEventListener('dragover', (e) => {
      e.preventDefault();
    });

    app.addEventListener('drop', async (e) => {
      e.preventDefault();
      dragCounter = 0;
      this.hideDropOverlay();

      const files = e.dataTransfer?.files;
      if (!files || files.length === 0) return;

      for (const file of Array.from(files)) {
        if (file.name.endsWith('.zip')) {
          await this.importZipFile(file);
          return;
        }
        await this.importFile(file);
      }
    });
  }

  private initKeyboardShortcuts(): void {
    document.addEventListener('keydown', (e) => {
      // Ctrl+S: Save
      if (e.ctrlKey && e.key === 's') {
        e.preventDefault();
        this.tabManager.saveActive();
        this.saveProject();
      }

      // F5 or Ctrl+Shift+P: Play/Stop
      if (e.key === 'F5' || (e.ctrlKey && e.shiftKey && e.key === 'P')) {
        e.preventDefault();
        if (this.isPlaying) {
          this.stop();
        } else {
          this.play();
        }
      }

      // Ctrl+Shift+E: Export ZIP
      if (e.ctrlKey && e.shiftKey && e.key === 'E') {
        e.preventDefault();
        this.exportZip();
      }

      // Ctrl+P: Quick file search
      if (e.ctrlKey && !e.shiftKey && e.key === 'p') {
        e.preventDefault();
        this.showQuickOpen();
      }
    });
  }

  private initConsoleResize(): void {
    const consolePanel = document.getElementById('console-panel')!;
    const header = consolePanel.querySelector('.console-header')!;
    let isResizing = false;
    let startY = 0;
    let startHeight = 0;

    header.addEventListener('mousedown', (e: Event) => {
      const me = e as MouseEvent;
      isResizing = true;
      startY = me.clientY;
      startHeight = consolePanel.offsetHeight;
      document.body.style.cursor = 'ns-resize';
      document.body.style.userSelect = 'none';
    });

    document.addEventListener('mousemove', (e: MouseEvent) => {
      if (!isResizing) return;
      const delta = startY - e.clientY;
      const newHeight = Math.max(60, Math.min(window.innerHeight * 0.5, startHeight + delta));
      consolePanel.style.height = `${newHeight}px`;
    });

    document.addEventListener('mouseup', () => {
      if (isResizing) {
        isResizing = false;
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
      }
    });
  }

  // ===== Project Operations =====

  private startNewProject(): void {
    this.hideWelcomeScreen();
    this.vfs.clear();
    const demo = createDemoVFS();
    // Copy demo files to our VFS
    for (const entry of demo.list()) {
      this.vfs.put(entry.path, {
        type: entry.type,
        content: entry.content,
        blob: entry.blob,
        buffer: entry.buffer,
      });
    }
    this.currentProjectId = this.generateId();
    this.currentProjectName = 'my-game';
    this.saveProject();

    // Open the main script by default
    this.tabManager.open('scripts/main.lua');
    this.fileTree.setActive('scripts/main.lua');
  }

  private async loadProject(projectId: string): Promise<void> {
    this.hideWelcomeScreen();
    const loaded = await this.storage.load(projectId, this.vfs);
    if (loaded) {
      this.currentProjectId = projectId;
      const meta = await this.storage.getProject(projectId);
      if (meta) {
        this.currentProjectName = meta.name;
      }
      // Open main script if it exists
      if (this.vfs.has('scripts/main.lua')) {
        this.tabManager.open('scripts/main.lua');
        this.fileTree.setActive('scripts/main.lua');
      }
    } else {
      this.consolePanel.log('error', `Failed to load project: ${projectId}`);
      this.startNewProject();
    }
  }

  private async saveProject(): Promise<void> {
    if (!this.currentProjectId) return;
    this.tabManager.saveAll();

    try {
      await this.storage.save(this.currentProjectId, this.currentProjectName, this.vfs);
      this.updateSaveStatus('Saved');
    } catch (err) {
      this.consolePanel.log('error', `Save failed: ${err}`);
      this.updateSaveStatus('Save failed');
    }
  }

  private scheduleSave(): void {
    if (this.autoSaveTimer !== null) {
      clearTimeout(this.autoSaveTimer);
    }
    this.updateSaveStatus('Unsaved changes');
    this.autoSaveTimer = window.setTimeout(() => {
      this.autoSaveTimer = null;
      this.saveProject();
    }, 500);
  }

  private updateSaveStatus(text: string): void {
    const el = document.getElementById('save-status');
    if (el) el.textContent = text;
  }

  // ===== Play / Stop =====

  private async play(): Promise<void> {
    if (this.isPlaying) return;

    // Save before playing
    this.tabManager.saveAll();
    await this.saveProject();

    this.isPlaying = true;
    const playBtn = document.getElementById('btn-play')!;
    const stopBtn = document.getElementById('btn-stop')!;
    playBtn.setAttribute('disabled', '');
    stopBtn.removeAttribute('disabled');

    const canvas = document.getElementById('game-canvas')!;
    canvas.classList.remove('hidden');

    this.consolePanel.log('info', 'Starting game...');

    // The engine bridge is already installed.
    // The actual engine integration (Step 9) will call window.__tinyEngine.start()
    if (window.__tinyEngine) {
      try {
        window.__tinyEngine.start('game-canvas');
      } catch (err) {
        this.consolePanel.log('error', `Engine error: ${err}`);
        this.stop();
      }
    } else {
      this.consolePanel.log('warn', 'Engine not loaded. The Kotlin/JS engine module is not available.');
    }
  }

  private stop(): void {
    if (!this.isPlaying) return;

    this.isPlaying = false;
    const playBtn = document.getElementById('btn-play')!;
    const stopBtn = document.getElementById('btn-stop')!;
    playBtn.removeAttribute('disabled');
    stopBtn.setAttribute('disabled', '');

    const canvas = document.getElementById('game-canvas')!;
    canvas.classList.add('hidden');

    if (window.__tinyEngine) {
      try {
        window.__tinyEngine.stop();
      } catch (err) {
        this.consolePanel.log('error', `Stop error: ${err}`);
      }
    }

    this.consolePanel.log('info', 'Game stopped.');
  }

  // ===== ZIP Operations =====

  private async exportZip(): Promise<void> {
    this.tabManager.saveAll();
    try {
      await this.zipManager.exportZip(this.vfs, this.currentProjectName);
      this.consolePanel.log('info', 'Project exported as ZIP.');
    } catch (err) {
      this.consolePanel.log('error', `Export failed: ${err}`);
    }
  }

  private showImportDialog(): void {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.zip';
    input.addEventListener('change', async () => {
      const file = input.files?.[0];
      if (file) {
        await this.importZipFile(file);
      }
    });
    input.click();
  }

  private async importZipFile(file: File | Blob): Promise<void> {
    if (this.vfs.list().length > 0) {
      if (!confirm('Importing will replace the current project. Continue?')) {
        return;
      }
    }

    try {
      const name = await this.zipManager.importZip(file, this.vfs);
      this.currentProjectId = this.generateId();
      this.currentProjectName = name;
      await this.saveProject();
      this.consolePanel.log('info', `Imported project: ${name}`);

      // Open main script if available
      if (this.vfs.has('scripts/main.lua')) {
        this.tabManager.open('scripts/main.lua');
        this.fileTree.setActive('scripts/main.lua');
      }
    } catch (err) {
      this.consolePanel.log('error', `Import failed: ${err}`);
    }
  }

  private async importFile(file: File): Promise<void> {
    const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
    let folder = '';
    if (['png', 'jpg', 'jpeg', 'gif', 'bmp'].includes(ext)) {
      folder = 'sprites';
    } else if (['json', 'ldtk', 'map'].includes(ext)) {
      folder = 'maps';
    } else if (ext === 'lua') {
      folder = 'scripts';
    }

    const path = folder ? `${folder}/${file.name}` : file.name;

    if (this.vfs.has(path)) {
      if (!confirm(`File "${path}" already exists. Replace?`)) {
        return;
      }
    }

    const buffer = await file.arrayBuffer();
    const isText = ['lua', 'json', 'ldtk', 'map', 'txt', 'cfg', 'xml', 'sfx'].includes(ext);

    if (isText) {
      const content = new TextDecoder().decode(buffer);
      this.vfs.put(path, { content });
    } else {
      const blob = new Blob([buffer]);
      this.vfs.put(path, { blob, buffer });
    }

    this.consolePanel.log('info', `Imported file: ${path}`);
  }

  // ===== Welcome Screen =====

  private showWelcomeScreen(projects: ProjectMeta[]): void {
    const screen = document.getElementById('welcome-screen')!;
    screen.classList.remove('hidden');

    document.getElementById('btn-new-project')!.addEventListener('click', () => {
      this.startNewProject();
    });

    document.getElementById('btn-import-project')!.addEventListener('click', () => {
      this.showImportDialog();
    });

    const recentEl = document.getElementById('recent-projects')!;
    if (projects.length > 0) {
      recentEl.innerHTML = '<div class="recent-projects-title">Recent Projects</div>';
      for (const project of projects) {
        const item = document.createElement('div');
        item.className = 'recent-project-item';

        const date = new Date(project.modified).toLocaleDateString();
        item.innerHTML = `
          <div>
            <div class="recent-project-name">${this.escapeHtml(project.name)}</div>
            <div class="recent-project-date">${date}</div>
          </div>
          <button class="recent-project-delete" title="Delete project">&times;</button>
        `;

        item.addEventListener('click', (e) => {
          if ((e.target as HTMLElement).classList.contains('recent-project-delete')) {
            return;
          }
          this.loadProject(project.id);
        });

        item.querySelector('.recent-project-delete')!.addEventListener('click', async (e) => {
          e.stopPropagation();
          if (confirm(`Delete project "${project.name}"?`)) {
            await this.storage.deleteProject(project.id);
            item.remove();
            const remaining = await this.storage.listProjects();
            if (remaining.length === 0) {
              recentEl.innerHTML = '';
            }
          }
        });

        recentEl.appendChild(item);
      }
    }
  }

  private hideWelcomeScreen(): void {
    const screen = document.getElementById('welcome-screen')!;
    screen.classList.add('hidden');
  }

  // ===== Quick Open =====

  private showQuickOpen(): void {
    const entries = this.vfs.list();
    const paths = entries.map(e => e.path);

    // Simple prompt-based quick open (will be improved in Step 11)
    const input = prompt('Open file:', '');
    if (!input) return;

    const lower = input.toLowerCase();
    const match = paths.find(p => p.toLowerCase().includes(lower));
    if (match) {
      this.tabManager.open(match);
      this.fileTree.setActive(match);
    }
  }

  // ===== Drag & Drop Overlay =====

  private showDropOverlay(): void {
    let overlay = document.querySelector('.drop-overlay');
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.className = 'drop-overlay';
      overlay.innerHTML = '<div class="drop-overlay-text">Drop files or ZIP here</div>';
      document.body.appendChild(overlay);
    }
  }

  private hideDropOverlay(): void {
    document.querySelector('.drop-overlay')?.remove();
  }

  // ===== Utilities =====

  private generateId(): string {
    return `project-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  }

  private escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}
