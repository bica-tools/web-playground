import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-explore',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="explore-hero">
      <h1>Explore</h1>
      <p>Learn about session types, lattice theory, and the research programme</p>
    </section>
    <div class="hub-grid">
      @for (card of cards; track card.route) {
        <a class="hub-card" [routerLink]="card.route">
          <div class="hub-icon">{{ card.icon }}</div>
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
          <span class="hub-link">{{ card.linkText }} &rarr;</span>
        </a>
      }
    </div>
  `,
  styleUrl: './explore.component.scss',
})
export class ExploreComponent {
  readonly cards = [
    { icon: '\u25B7', title: 'Visual Introduction', desc: 'A scroll-driven visual essay explaining session types and why they form lattices. 6 interactive scenes.', route: '/intro', linkText: 'Start the tour' },
    { icon: '\u25C9', title: 'Research Map', desc: 'Interactive force-directed graph showing how 25 results connect across 7 categories.', route: '/map', linkText: 'Explore the graph' },
    { icon: '\u2702', title: 'Tutorials', desc: 'Step-by-step guides from parsing your first session type to verifying lattice properties.', route: '/tutorials', linkText: 'Start learning' },
    { icon: '\u2261', title: 'Documentation', desc: 'Theory reference covering session types, reticulates, state spaces, lattices, and morphisms.', route: '/documentation', linkText: 'Read the docs' },
    { icon: '?', title: 'FAQ', desc: 'Frequently asked questions about concepts, tools, benchmarks, and the research programme.', route: '/faq', linkText: 'Find answers' },
  ];
}
