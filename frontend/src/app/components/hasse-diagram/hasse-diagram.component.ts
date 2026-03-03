import { Component, Input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-hasse-diagram',
  standalone: true,
  template: `
    <div class="hasse-container" [innerHTML]="safeSvg"></div>
  `,
  styles: [`
    .hasse-container {
      display: flex;
      justify-content: center;
      padding: 16px;
      overflow: auto;
    }
    :host ::ng-deep svg {
      max-width: 100%;
      height: auto;
    }
  `],
})
export class HasseDiagramComponent {
  safeSvg: SafeHtml = '';

  constructor(private sanitizer: DomSanitizer) {}

  @Input()
  set svgHtml(value: string) {
    this.safeSvg = this.sanitizer.bypassSecurityTrustHtml(value || '');
  }
}
