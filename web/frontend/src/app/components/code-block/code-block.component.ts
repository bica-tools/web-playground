import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-code-block',
  standalone: true,
  imports: [MatIconModule, MatButtonModule, MatSnackBarModule],
  template: `
    <div class="code-block">
      <div class="code-header">
        <span class="code-label">{{ label }}</span>
        <button mat-icon-button (click)="copy()" aria-label="Copy to clipboard">
          <mat-icon>content_copy</mat-icon>
        </button>
      </div>
      <pre><code>{{ code }}</code></pre>
    </div>
  `,
  styles: [`
    .code-block {
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-radius: 8px;
      overflow: hidden;
      margin: 8px 0;
    }
    .code-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 4px 12px;
      background: rgba(0, 0, 0, 0.04);
      border-bottom: 1px solid rgba(0, 0, 0, 0.12);
    }
    .code-label {
      font-size: 12px;
      font-weight: 500;
      text-transform: uppercase;
      color: rgba(0, 0, 0, 0.6);
    }
    pre {
      margin: 0;
      padding: 16px;
      overflow-x: auto;
      font-size: 13px;
      line-height: 1.5;
      background: #fafafa;
    }
    code {
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
    }
  `],
})
export class CodeBlockComponent {
  @Input() code = '';
  @Input() label = '';

  constructor(private snackBar: MatSnackBar) {}

  copy(): void {
    navigator.clipboard.writeText(this.code).then(() => {
      this.snackBar.open('Copied to clipboard', '', { duration: 2000 });
    });
  }
}
