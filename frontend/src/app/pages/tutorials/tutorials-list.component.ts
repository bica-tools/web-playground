import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { TutorialSummaryDto } from '../../models/api.models';

@Component({
  selector: 'app-tutorials-list',
  standalone: true,
  imports: [],
  template: `
    <header class="page-header">
      <h1>Tutorials</h1>
      <p>Step-by-step guides to session types, reticulates, and tooling.</p>
    </header>

    @if (tutorials().length === 0) {
      <div class="loading">Loading tutorials...</div>
    } @else {
      <div class="card-grid">
        @for (tut of tutorials(); track tut.id) {
          <div class="tutorial-card" (click)="openTutorial(tut.id)" tabindex="0" (keydown.enter)="openTutorial(tut.id)">
            <span class="card-number">{{ tut.number }}</span>
            <h3 class="card-title">{{ tut.title }}</h3>
            <p class="card-subtitle">{{ tut.subtitle }}</p>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .page-header {
      padding: 24px 0 16px;
    }
    .page-header h1 {
      font-size: 24px;
      font-weight: 500;
      margin: 0 0 8px;
    }
    .page-header p {
      color: rgba(0, 0, 0, 0.6);
      margin: 0;
    }

    .card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;
      padding: 16px 0 40px;
    }

    .tutorial-card {
      position: relative;
      padding: 24px;
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
      background: white;
      overflow: hidden;
    }
    .tutorial-card:hover {
      border-color: var(--brand-primary, #4338ca);
      box-shadow: 0 4px 12px rgba(67, 56, 202, 0.12);
      transform: translateY(-2px);
    }
    .tutorial-card:focus-visible {
      outline: 2px solid var(--brand-primary, #4338ca);
      outline-offset: 2px;
    }

    .card-number {
      position: absolute;
      top: -8px;
      right: 12px;
      font-size: 72px;
      font-weight: 700;
      color: rgba(67, 56, 202, 0.07);
      line-height: 1;
      pointer-events: none;
      user-select: none;
    }

    .card-title {
      font-size: 17px;
      font-weight: 500;
      margin: 0 0 8px;
      color: rgba(0, 0, 0, 0.87);
    }

    .card-subtitle {
      font-size: 14px;
      color: rgba(0, 0, 0, 0.55);
      margin: 0;
      line-height: 1.5;
    }

    .loading {
      padding: 48px 16px;
      text-align: center;
      color: rgba(0, 0, 0, 0.5);
      font-size: 16px;
    }

    @media (max-width: 640px) {
      .card-grid {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class TutorialsListComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);

  tutorials = signal<TutorialSummaryDto[]>([]);

  ngOnInit(): void {
    this.api.getTutorials().subscribe((list) => {
      this.tutorials.set(list);
    });
  }

  openTutorial(id: string): void {
    this.router.navigate(['/tutorials', id]);
  }
}
