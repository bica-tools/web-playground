import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface ExploreCard {
  title: string;
  description: string;
  route: string;
  icon: string;
}

@Component({
  selector: 'app-explore',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="explore-hero">
      <h1>Explore</h1>
      <p>Learn about session types and lattice theory</p>
    </section>

    <section class="card-grid">
      @for (card of cards; track card.route) {
        <a class="explore-card" [routerLink]="card.route">
          <span class="card-icon">{{ card.icon }}</span>
          <h2 class="card-title">{{ card.title }}</h2>
          <p class="card-desc">{{ card.description }}</p>
        </a>
      }
    </section>
  `,
  styleUrl: './explore.component.scss',
})
export class ExploreComponent {
  readonly cards: ExploreCard[] = [
    {
      title: 'Visual Intro',
      description: 'A visual introduction to session types in 6 scenes',
      route: '/intro',
      icon: '\u25B6',
    },
    {
      title: 'Research Map',
      description: 'Interactive concept graph showing how 25 results connect',
      route: '/map',
      icon: '\u2B21',
    },
    {
      title: 'Tutorials',
      description: 'Step-by-step guides from basics to advanced topics',
      route: '/tutorials',
      icon: '\u2709',
    },
    {
      title: 'Documentation',
      description: 'Theory reference for session types, lattices, and morphisms',
      route: '/documentation',
      icon: '\u2261',
    },
    {
      title: 'FAQ',
      description: 'Frequently asked questions about concepts, tools, and benchmarks',
      route: '/faq',
      icon: '?',
    },
  ];
}
