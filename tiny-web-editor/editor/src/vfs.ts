/** Types for the Virtual File System */

export type VFSFileType = 'lua' | 'image' | 'map' | 'data' | 'config';

export interface VFSEntry {
  path: string;
  type: VFSFileType;
  content: string | null;
  blob: Blob | null;
  buffer: ArrayBuffer | null;
  modified: number;
}

export interface VFSEvent {
  type: 'put' | 'delete';
  path: string;
}

export type VFSListener = (event: VFSEvent) => void;

export interface SerializedVFS {
  files: Array<{
    path: string;
    type: VFSFileType;
    content: string | null;
    blobData: ArrayBuffer | null;
    modified: number;
  }>;
}

/**
 * In-memory Virtual File System.
 * Central data store for all project files — text (Lua scripts) and binary (images, maps).
 * Emits change events so the UI and persistence layers can react.
 */
export class VirtualFileSystem {
  private files = new Map<string, VFSEntry>();
  private listeners: VFSListener[] = [];

  /** Add or update a file in the VFS. */
  put(path: string, partial: Partial<VFSEntry>): void {
    const normalized = this.normalizePath(path);
    const existing = this.files.get(normalized);
    const entry: VFSEntry = {
      path: normalized,
      type: partial.type ?? existing?.type ?? this.inferType(normalized),
      content: partial.content !== undefined ? partial.content : (existing?.content ?? null),
      blob: partial.blob !== undefined ? partial.blob : (existing?.blob ?? null),
      buffer: partial.buffer !== undefined ? partial.buffer : (existing?.buffer ?? null),
      modified: Date.now(),
    };
    this.files.set(normalized, entry);
    this.emit({ type: 'put', path: normalized });
  }

  /** Retrieve a file entry by path. */
  get(path: string): VFSEntry | undefined {
    return this.files.get(this.normalizePath(path));
  }

  /** Delete a file from the VFS. Returns true if the file existed. */
  delete(path: string): boolean {
    const normalized = this.normalizePath(path);
    const existed = this.files.delete(normalized);
    if (existed) {
      this.emit({ type: 'delete', path: normalized });
    }
    return existed;
  }

  /** List files, optionally filtered by a path prefix. */
  list(prefix?: string): VFSEntry[] {
    const entries: VFSEntry[] = [];
    for (const [p, entry] of this.files) {
      if (!prefix || p.startsWith(prefix)) {
        entries.push(entry);
      }
    }
    return entries.sort((a, b) => a.path.localeCompare(b.path));
  }

  /** Check if a file exists. */
  has(path: string): boolean {
    return this.files.has(this.normalizePath(path));
  }

  /** Remove all files. */
  clear(): void {
    const paths = Array.from(this.files.keys());
    this.files.clear();
    for (const path of paths) {
      this.emit({ type: 'delete', path });
    }
  }

  /** Register a change listener. Returns an unsubscribe function. */
  onChange(callback: VFSListener): () => void {
    this.listeners.push(callback);
    return () => {
      const idx = this.listeners.indexOf(callback);
      if (idx >= 0) this.listeners.splice(idx, 1);
    };
  }

  /** Serialize all files for persistence. */
  async serialize(): Promise<SerializedVFS> {
    const files: SerializedVFS['files'] = [];
    for (const [, entry] of this.files) {
      let blobData: ArrayBuffer | null = null;
      if (entry.blob) {
        blobData = await entry.blob.arrayBuffer();
      } else if (entry.buffer) {
        blobData = entry.buffer;
      }
      files.push({
        path: entry.path,
        type: entry.type,
        content: entry.content,
        blobData,
        modified: entry.modified,
      });
    }
    return { files };
  }

  /** Restore files from serialized data. */
  async deserialize(data: SerializedVFS): Promise<void> {
    this.files.clear();
    for (const f of data.files) {
      const blob = f.blobData ? new Blob([f.blobData]) : null;
      const buffer = f.blobData ? f.blobData : null;
      this.files.set(f.path, {
        path: f.path,
        type: f.type,
        content: f.content,
        blob,
        buffer,
        modified: f.modified,
      });
    }
    // Emit a single put event per file to trigger UI updates
    for (const f of data.files) {
      this.emit({ type: 'put', path: f.path });
    }
  }

  /** Rename a file. Returns true if the source existed. */
  rename(oldPath: string, newPath: string): boolean {
    const normalizedOld = this.normalizePath(oldPath);
    const normalizedNew = this.normalizePath(newPath);
    const entry = this.files.get(normalizedOld);
    if (!entry) return false;
    this.files.delete(normalizedOld);
    entry.path = normalizedNew;
    entry.type = this.inferType(normalizedNew);
    entry.modified = Date.now();
    this.files.set(normalizedNew, entry);
    this.emit({ type: 'delete', path: normalizedOld });
    this.emit({ type: 'put', path: normalizedNew });
    return true;
  }

  /** Get all unique folder paths. */
  getFolders(): string[] {
    const folders = new Set<string>();
    for (const path of this.files.keys()) {
      const parts = path.split('/');
      for (let i = 1; i < parts.length; i++) {
        folders.add(parts.slice(0, i).join('/'));
      }
    }
    return Array.from(folders).sort();
  }

  private normalizePath(path: string): string {
    return path.replace(/^\/+/, '').replace(/\/+/g, '/');
  }

  private inferType(path: string): VFSFileType {
    const ext = path.split('.').pop()?.toLowerCase() ?? '';
    switch (ext) {
      case 'lua': return 'lua';
      case 'png':
      case 'jpg':
      case 'jpeg':
      case 'gif':
      case 'bmp':
        return 'image';
      case 'json':
      case 'ldtk':
      case 'map':
        return 'map';
      default: return 'data';
    }
  }

  private emit(event: VFSEvent): void {
    for (const listener of this.listeners) {
      listener(event);
    }
  }
}

/** Create a VFS pre-populated with a demo project. */
export function createDemoVFS(): VirtualFileSystem {
  const vfs = new VirtualFileSystem();

  vfs.put('_tiny.json', {
    type: 'config',
    content: JSON.stringify({
      name: 'my-game',
      width: 256,
      height: 256,
      zoom: 2,
      palette: [
        '#FFF9B3', '#B9C5CC', '#4774B3', '#144B66',
        '#8FB347', '#2E994E', '#F29066', '#E65050',
        '#707D7C', '#293C40', '#170B1A', '#0A010D',
        '#570932', '#871E2E', '#FFBF40', '#CC1424',
      ],
      scripts: ['scripts/main.lua'],
      sprites: [],
      levels: [],
      sounds: [],
    }, null, 2),
  });

  vfs.put('scripts/main.lua', {
    type: 'lua',
    content: `-- Main game script
-- This is the entry point for your Tiny game.

function _init()
    -- Called once when the game starts
end

function _update()
    -- Called every frame for game logic
end

function _draw()
    -- Called every frame for rendering
    cls()
    print("Hello, Tiny!", 80, 120, 5)
end
`,
  });

  return vfs;
}
