import { VirtualFileSystem } from './vfs';

/**
 * Exposes the VFS to the Kotlin/JS engine via window.__tinyVFS.
 * All reads are synchronous since the engine requires synchronous file access.
 * ArrayBuffer data is pre-loaded in VFS entries for this purpose.
 */
export interface TinyVFSBridge {
  readText(path: string): string | null;
  readBinary(path: string): Int8Array | null;
  exists(path: string): boolean;
  list(prefix?: string): string[];
}

declare global {
  interface Window {
    __tinyVFS?: TinyVFSBridge;
    __tinyEngine?: {
      start(canvasId: string): void;
      stop(): void;
    };
  }
}

export class VFSBridge {
  private vfs: VirtualFileSystem;

  constructor(vfs: VirtualFileSystem) {
    this.vfs = vfs;
  }

  /** Install the bridge on window.__tinyVFS. */
  install(): void {
    window.__tinyVFS = {
      readText: (path: string): string | null => {
        const entry = this.vfs.get(path);
        if (!entry) return null;
        return entry.content;
      },

      readBinary: (path: string): Int8Array | null => {
        const entry = this.vfs.get(path);
        if (!entry) return null;

        if (entry.buffer) {
          return new Int8Array(entry.buffer);
        }
        return null;
      },

      exists: (path: string): boolean => {
        return this.vfs.has(path);
      },

      list: (prefix?: string): string[] => {
        return this.vfs.list(prefix).map(e => e.path);
      },
    };
  }

  /** Remove the bridge from window. */
  uninstall(): void {
    delete window.__tinyVFS;
  }
}
