import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer class="footer">
      <div class="footer-content">
        <span>BICA Reborn &mdash; Session Types as Algebraic Reticulates</span>
        <span class="separator">|</span>
        <span>Alexandre Zua Caldeira &mdash; LASIGE/FCUL, University of Lisbon</span>
      </div>
    </footer>
  `,
  styles: [`
    .footer {
      padding: 24px 16px;
      text-align: center;
      color: rgba(0, 0, 0, 0.6);
      font-size: 13px;
      border-top: 1px solid rgba(0, 0, 0, 0.12);
      margin-top: 48px;
    }
    .footer-content {
      max-width: 960px;
      margin: 0 auto;
    }
    .separator {
      margin: 0 8px;
    }
  `],
})
export class FooterComponent {}
