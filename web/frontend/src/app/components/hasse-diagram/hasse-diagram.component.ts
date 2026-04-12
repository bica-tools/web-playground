import { Component, Input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-hasse-diagram',
  standalone: true,
  template: `
    <figure class="hasse-container" role="img" [attr.aria-label]="ariaLabel">
      <div [innerHTML]="safeSvg"></div>
    </figure>
  `,
  styles: [`
    .hasse-container {
      display: flex;
      justify-content: center;
      padding: 16px;
      overflow: auto;
      margin: 0;
    }
    :host ::ng-deep svg {
      max-width: 100%;
      height: auto;
    }
  `],
})
export class HasseDiagramComponent {
  safeSvg: SafeHtml = '';
  ariaLabel = 'Hasse diagram of session type state space';

  constructor(private sanitizer: DomSanitizer) {}

  @Input()
  set svgHtml(value: string) {
    this.safeSvg = this.sanitizer.bypassSecurityTrustHtml(value || '');
  }

  @Input()
  set label(value: string) {
    if (value) {
      this.ariaLabel = `Hasse diagram: ${value}`;
    }
  }
}
