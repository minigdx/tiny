export type LogLevel = 'info' | 'warn' | 'error';

export interface LogEntry {
  level: LogLevel;
  message: string;
  timestamp: number;
  file?: string;
  line?: number;
}

type ConsoleClickListener = (file: string, line: number) => void;

/**
 * Captures and displays log output, including Lua print() and errors.
 * Supports clickable error locations to navigate to source.
 */
export class ConsolePanel {
  private outputEl: HTMLElement;
  private clearBtn: HTMLElement;
  private entries: LogEntry[] = [];
  private clickListeners: ConsoleClickListener[] = [];
  private originalConsoleLog: typeof console.log;
  private originalConsoleWarn: typeof console.warn;
  private originalConsoleError: typeof console.error;
  private intercepting = false;

  constructor(outputEl: HTMLElement, clearBtn: HTMLElement) {
    this.outputEl = outputEl;
    this.clearBtn = clearBtn;
    this.originalConsoleLog = console.log;
    this.originalConsoleWarn = console.warn;
    this.originalConsoleError = console.error;

    this.clearBtn.addEventListener('click', () => this.clear());
  }

  /** Start intercepting console output. */
  startCapture(): void {
    if (this.intercepting) return;
    this.intercepting = true;

    console.log = (...args: any[]) => {
      this.originalConsoleLog.apply(console, args);
      this.log('info', args.map(a => String(a)).join(' '));
    };

    console.warn = (...args: any[]) => {
      this.originalConsoleWarn.apply(console, args);
      this.log('warn', args.map(a => String(a)).join(' '));
    };

    console.error = (...args: any[]) => {
      this.originalConsoleError.apply(console, args);
      this.log('error', args.map(a => String(a)).join(' '));
    };
  }

  /** Stop intercepting console output. */
  stopCapture(): void {
    if (!this.intercepting) return;
    this.intercepting = false;
    console.log = this.originalConsoleLog;
    console.warn = this.originalConsoleWarn;
    console.error = this.originalConsoleError;
  }

  /** Add a log entry programmatically. */
  log(level: LogLevel, message: string, file?: string, line?: number): void {
    const entry: LogEntry = {
      level,
      message,
      timestamp: Date.now(),
      file,
      line,
    };

    // Try to extract file:line from error messages
    if (!file && !line) {
      const match = message.match(/(\S+\.lua):(\d+)/);
      if (match) {
        entry.file = match[1];
        entry.line = parseInt(match[2], 10);
      }
    }

    this.entries.push(entry);
    this.renderEntry(entry);
    this.scrollToBottom();
  }

  /** Clear all log entries. */
  clear(): void {
    this.entries = [];
    this.outputEl.innerHTML = '';
  }

  /** Register a click handler for error locations. */
  onClick(callback: ConsoleClickListener): () => void {
    this.clickListeners.push(callback);
    return () => {
      const idx = this.clickListeners.indexOf(callback);
      if (idx >= 0) this.clickListeners.splice(idx, 1);
    };
  }

  private renderEntry(entry: LogEntry): void {
    const el = document.createElement('div');
    el.className = `console-line ${entry.level}`;

    const time = new Date(entry.timestamp);
    const timeStr = `${time.getHours().toString().padStart(2, '0')}:${time.getMinutes().toString().padStart(2, '0')}:${time.getSeconds().toString().padStart(2, '0')}`;

    let messageHtml = this.escapeHtml(entry.message);

    // Make file:line references clickable
    if (entry.file && entry.line) {
      const ref = `${entry.file}:${entry.line}`;
      const escapedRef = this.escapeHtml(ref);
      messageHtml = messageHtml.replace(
        escapedRef,
        `<span class="console-clickable" data-file="${this.escapeHtml(entry.file)}" data-line="${entry.line}">${escapedRef}</span>`,
      );
    }

    el.innerHTML = `<span class="console-time">[${timeStr}]</span>${messageHtml}`;

    // Add click handler for clickable refs
    el.querySelectorAll('.console-clickable').forEach(span => {
      span.addEventListener('click', () => {
        const file = span.getAttribute('data-file');
        const line = parseInt(span.getAttribute('data-line') ?? '0', 10);
        if (file && line) {
          for (const listener of this.clickListeners) {
            listener(file, line);
          }
        }
      });
    });

    this.outputEl.appendChild(el);
  }

  private scrollToBottom(): void {
    this.outputEl.scrollTop = this.outputEl.scrollHeight;
  }

  private escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}
