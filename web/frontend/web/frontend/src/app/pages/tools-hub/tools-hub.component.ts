import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-tools-hub',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="tools-hero">
      <h1>Tools</h1>
      <p>Interactive session type analysis in the browser</p>
    </section>
    <div class="hub-grid">
      @for (tool of tools; track tool.route) {
        <a class="tool-card" [routerLink]="tool.route">
          <h3>{{ tool.title }}</h3>
          <p class="tool-desc">{{ tool.desc }}</p>
          <ul class="tool-features">
            @for (f of tool.features; track f) {
              <li>{{ f }}</li>
            }
          </ul>
          <span class="tool-link">Open {{ tool.title }} &rarr;</span>
        </a>
      }
    </div>
  `,
  styleUrl: './tools-hub.component.scss',
})
export class ToolsHubComponent {
  readonly tools = [
    { title: 'Analyzer', desc: 'Parse, verify, and visualize session types. See the Hasse diagram instantly.', route: '/tools/analyzer', features: ['Live Hasse diagram', 'Lattice verification', 'Quick examples'] },
    { title: 'Global Types', desc: 'Analyze multiparty protocols with role projections.', route: '/tools/global-analyzer', features: ['Role-annotated transitions', 'Per-role Hasse diagrams', 'Projection verification'] },
    { title: 'Test Generator', desc: 'Generate JUnit 5 tests with coverage storyboard.', route: '/tools/test-generator', features: ['Valid paths, violations, incomplete', 'Coverage visualization', 'Frame-by-frame playback'] },
    { title: 'Compare', desc: 'Side-by-side subtyping, duality, and property comparison.', route: '/tools/compare', features: ['Dual Hasse diagrams', 'Gay\u2013Hole subtyping', 'Chomsky classification'] },
    { title: 'Composition', desc: 'Bottom-up multiparty composition via lattice products.', route: '/tools/composition', features: ['Free and synchronized products', 'Compatibility checking', 'Global type comparison'] },
  ];
}
