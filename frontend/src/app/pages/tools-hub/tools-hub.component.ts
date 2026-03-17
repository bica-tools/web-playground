import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface ToolCard {
  title: string;
  route: string;
  description: string;
  features: string[];
}

@Component({
  selector: 'app-tools-hub',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">Tools</h1>
        <p class="hero-subtitle">Interactive session type analysis in the browser</p>
      </div>
    </section>

    <section class="cards-section">
      <div class="card-grid">
        @for (tool of tools; track tool.route) {
          <a class="tool-card" [routerLink]="tool.route">
            <h2 class="card-title">{{ tool.title }}</h2>
            <p class="card-desc">{{ tool.description }}</p>
            <ul class="card-features">
              @for (feat of tool.features; track feat) {
                <li>{{ feat }}</li>
              }
            </ul>
            <span class="card-link">Open &rarr;</span>
          </a>
        }
      </div>
    </section>
  `,
  styleUrl: './tools-hub.component.scss',
})
export class ToolsHubComponent {
  readonly tools: ToolCard[] = [
    {
      title: 'Analyzer',
      route: '/tools/analyzer',
      description: 'Parse, verify, and visualize session types',
      features: ['Live Hasse diagram', 'Lattice verification', 'Quick examples'],
    },
    {
      title: 'Global Types',
      route: '/tools/global-analyzer',
      description: 'Analyze multiparty protocols with role projections',
      features: ['Role-annotated transitions', 'Per-role Hasse diagrams', 'Projection verification'],
    },
    {
      title: 'Test Generator',
      route: '/tools/test-generator',
      description: 'Generate JUnit 5 tests with coverage storyboard',
      features: ['Valid paths + violations + incomplete', 'Coverage visualization', 'Frame-by-frame playback'],
    },
    {
      title: 'Compare',
      route: '/tools/compare',
      description: 'Side-by-side subtyping, duality, and property comparison',
      features: ['Dual Hasse diagrams', 'Gay-Hole subtyping', 'Chomsky classification'],
    },
    {
      title: 'Composition',
      route: '/tools/composition',
      description: 'Bottom-up multiparty composition via lattice products',
      features: ['Free and synchronized products', 'Compatibility checking', 'Global type comparison'],
    },
  ];
}
