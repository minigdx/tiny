import { VirtualFileSystem, VFSFileType } from './vfs';

const DB_NAME = 'tiny-editor';
const DB_VERSION = 1;
const STORE_PROJECTS = 'projects';
const STORE_FILES = 'files';

export interface ProjectMeta {
  id: string;
  name: string;
  created: number;
  modified: number;
}

interface StoredFile {
  projectId: string;
  path: string;
  type: VFSFileType;
  content: string | null;
  blobData: Blob | null;
  modified: number;
}

/**
 * Persists projects and their files in IndexedDB.
 * Blob data is stored natively (no base64 conversion).
 */
export class ProjectStorage {
  private db: IDBDatabase | null = null;

  /** Open the IndexedDB database (creates stores on first use). */
  async open(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);

      request.onupgradeneeded = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains(STORE_PROJECTS)) {
          db.createObjectStore(STORE_PROJECTS, { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains(STORE_FILES)) {
          const store = db.createObjectStore(STORE_FILES, { keyPath: ['projectId', 'path'] });
          store.createIndex('byProject', 'projectId', { unique: false });
        }
      };

      request.onsuccess = () => {
        this.db = request.result;
        resolve();
      };

      request.onerror = () => reject(request.error);
    });
  }

  /** Save the entire VFS as a project. */
  async save(projectId: string, projectName: string, vfs: VirtualFileSystem): Promise<void> {
    const db = this.getDb();

    // Save project metadata
    const meta: ProjectMeta = {
      id: projectId,
      name: projectName,
      created: Date.now(),
      modified: Date.now(),
    };

    // Check if project already exists to preserve created date
    const existing = await this.getProject(projectId);
    if (existing) {
      meta.created = existing.created;
    }

    const tx = db.transaction([STORE_PROJECTS, STORE_FILES], 'readwrite');
    const projectStore = tx.objectStore(STORE_PROJECTS);
    const fileStore = tx.objectStore(STORE_FILES);

    projectStore.put(meta);

    // Delete existing files for this project
    const index = fileStore.index('byProject');
    const range = IDBKeyRange.only(projectId);
    const cursor = index.openCursor(range);

    await new Promise<void>((resolve, reject) => {
      cursor.onsuccess = () => {
        const c = cursor.result;
        if (c) {
          c.delete();
          c.continue();
        } else {
          resolve();
        }
      };
      cursor.onerror = () => reject(cursor.error);
    });

    // Add current files
    const entries = vfs.list();
    for (const entry of entries) {
      let blobData: Blob | null = null;
      if (entry.blob) {
        blobData = entry.blob;
      } else if (entry.buffer) {
        blobData = new Blob([entry.buffer]);
      }

      const stored: StoredFile = {
        projectId,
        path: entry.path,
        type: entry.type,
        content: entry.content,
        blobData,
        modified: entry.modified,
      };
      fileStore.put(stored);
    }

    await new Promise<void>((resolve, reject) => {
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  /** Load a project's files into the VFS. Returns true if the project existed. */
  async load(projectId: string, vfs: VirtualFileSystem): Promise<boolean> {
    const db = this.getDb();

    const meta = await this.getProject(projectId);
    if (!meta) return false;

    const tx = db.transaction(STORE_FILES, 'readonly');
    const store = tx.objectStore(STORE_FILES);
    const index = store.index('byProject');

    const files = await new Promise<StoredFile[]>((resolve, reject) => {
      const request = index.getAll(projectId);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });

    vfs.clear();

    for (const file of files) {
      let buffer: ArrayBuffer | null = null;
      if (file.blobData) {
        buffer = await file.blobData.arrayBuffer();
      }

      vfs.put(file.path, {
        type: file.type,
        content: file.content,
        blob: file.blobData,
        buffer,
      });
    }

    return true;
  }

  /** List all saved projects. */
  async listProjects(): Promise<ProjectMeta[]> {
    const db = this.getDb();
    const tx = db.transaction(STORE_PROJECTS, 'readonly');
    const store = tx.objectStore(STORE_PROJECTS);

    return new Promise((resolve, reject) => {
      const request = store.getAll();
      request.onsuccess = () => {
        const projects = (request.result as ProjectMeta[]).sort((a, b) => b.modified - a.modified);
        resolve(projects);
      };
      request.onerror = () => reject(request.error);
    });
  }

  /** Delete a project and all its files. */
  async deleteProject(projectId: string): Promise<void> {
    const db = this.getDb();
    const tx = db.transaction([STORE_PROJECTS, STORE_FILES], 'readwrite');

    tx.objectStore(STORE_PROJECTS).delete(projectId);

    const index = tx.objectStore(STORE_FILES).index('byProject');
    const cursor = index.openCursor(IDBKeyRange.only(projectId));

    await new Promise<void>((resolve, reject) => {
      cursor.onsuccess = () => {
        const c = cursor.result;
        if (c) {
          c.delete();
          c.continue();
        } else {
          resolve();
        }
      };
      cursor.onerror = () => reject(cursor.error);
    });

    await new Promise<void>((resolve, reject) => {
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  /** Get metadata for a single project. */
  async getProject(projectId: string): Promise<ProjectMeta | null> {
    const db = this.getDb();
    const tx = db.transaction(STORE_PROJECTS, 'readonly');
    const store = tx.objectStore(STORE_PROJECTS);

    return new Promise((resolve, reject) => {
      const request = store.get(projectId);
      request.onsuccess = () => resolve(request.result ?? null);
      request.onerror = () => reject(request.error);
    });
  }

  private getDb(): IDBDatabase {
    if (!this.db) throw new Error('Database not opened. Call open() first.');
    return this.db;
  }
}
