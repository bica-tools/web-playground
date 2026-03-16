import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-main">
          <span class="footer-brand">BICA Tools</span>
          <span class="footer-sep">&mdash;</span>
          <span>Session Types as Algebraic Reticulates</span>
        </div>
        <div class="footer-meta">
          <span>Alexandre Zua Caldeira &middot; Independent Researcher</span>
          <span class="footer-sep">&middot;</span>
          <a href="https://github.com/bica-tools" target="_blank" rel="noopener">GitHub</a>
          <span class="footer-sep">&middot;</span>
          <span>bica-tools.org</span>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    .footer {
      padding: 24px 16px;
      text-align: center;
      color: rgba(0, 0, 0, 0.5);
      font-size: 13px;
      border-top: 1px solid rgba(0, 0, 0, 0.08);
      margin-top: 48px;
      line-height: 1.8;
    }
    .footer-content {
      max-width: 960px;
      margin: 0 auto;
    }
    .footer-brand {
      font-weight: 600;
      color: rgba(0, 0, 0, 0.65);
    }
    .footer-sep {
      margin: 0 6px;
    }
    .footer-meta {
      margin-top: 4px;
    }
    .footer-meta a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
    }
    .footer-meta a:hover {
      text-decoration: underline;
    }
  `],
})
export class FooterComponent {}
