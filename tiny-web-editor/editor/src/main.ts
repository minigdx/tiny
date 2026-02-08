import './styles/main.css';
import { VirtualFileSystem } from './vfs';
import { FileTree } from './ui/file-tree';
import { TabManager } from './ui/tab-manager';
import { ConsolePanel } from './ui/console-panel';
import { ProjectStorage } from './storage';
import { ZipManager } from './zip';
import { VFSBridge } from './bridge';
import { EditorApp } from './app';

const app = new EditorApp();
app.init();

// Expose VFS on window for debugging
(window as any).vfs = app.vfs;
