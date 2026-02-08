import JSZip from 'jszip';
import { VirtualFileSystem } from './vfs';

/**
 * Handles ZIP export and import of VFS projects.
 */
export class ZipManager {
  /**
   * Export the VFS contents to a ZIP file and trigger download.
   * Includes a project.json metadata file at the root.
   */
  async exportZip(vfs: VirtualFileSystem, projectName: string): Promise<void> {
    const zip = new JSZip();

    // Add project metadata
    const meta = {
      name: projectName,
      version: '1.0.0',
      exportDate: new Date().toISOString(),
      editor: 'tiny-web-editor',
    };
    zip.file('project.json', JSON.stringify(meta, null, 2));

    // Add all VFS files
    const entries = vfs.list();
    for (const entry of entries) {
      if (entry.content !== null) {
        zip.file(entry.path, entry.content);
      } else if (entry.blob) {
        zip.file(entry.path, entry.blob);
      } else if (entry.buffer) {
        zip.file(entry.path, entry.buffer);
      }
    }

    // Generate and download
    const blob = await zip.generateAsync({ type: 'blob', compression: 'DEFLATE' });
    const date = new Date().toISOString().slice(0, 10);
    const filename = `${projectName}-${date}.zip`;
    this.downloadBlob(blob, filename);
  }

  /**
   * Import a ZIP file into the VFS.
   * Validates that the ZIP contains at least a scripts/main.lua.
   * Returns the project name from project.json if present.
   */
  async importZip(file: File | Blob, vfs: VirtualFileSystem): Promise<string> {
    const zip = await JSZip.loadAsync(file);
    let projectName = 'imported-project';

    // Read project.json if present
    const metaFile = zip.file('project.json');
    if (metaFile) {
      try {
        const metaStr = await metaFile.async('string');
        const meta = JSON.parse(metaStr);
        if (meta.name) projectName = meta.name;
      } catch {
        // Ignore parse errors in project.json
      }
    }

    // Validate structure: need at least one .lua file
    const hasLua = Object.keys(zip.files).some(p => p.endsWith('.lua'));
    if (!hasLua) {
      throw new Error('Invalid project ZIP: no .lua files found');
    }

    // Clear VFS and load files from ZIP
    vfs.clear();

    const promises: Promise<void>[] = [];

    zip.forEach((relativePath, zipEntry) => {
      if (zipEntry.dir) return;
      if (relativePath === 'project.json') return; // Skip metadata file

      const ext = relativePath.split('.').pop()?.toLowerCase() ?? '';
      const isText = ['lua', 'json', 'ldtk', 'map', 'txt', 'cfg', 'ini', 'xml', 'sfx'].includes(ext);

      if (isText) {
        promises.push(
          zipEntry.async('string').then(content => {
            vfs.put(relativePath, { content });
          }),
        );
      } else {
        promises.push(
          zipEntry.async('arraybuffer').then(buffer => {
            const blob = new Blob([buffer]);
            vfs.put(relativePath, { blob, buffer });
          }),
        );
      }
    });

    await Promise.all(promises);
    return projectName;
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
}
