import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, forkJoin } from 'rxjs';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';
import { ApiService } from '../../services/api.service';
import { TutorialSummaryDto, TutorialDto } from '../../models/api.models';

@Component({
  selector: 'app-tutorial-detail',
  standalone: true,
  imports: [CodeBlockComponent, RouterLink],
  template: `
    <div class="tut-layout">
      <!-- Sticky sidebar -->
      <aside class="tut-sidebar">
        <nav class="sidebar-nav">
          <a class="back-link" routerLink="/tutorials">&larr; All Tutorials</a>

          @if (tutorial) {
            <h3>Steps</h3>
            <ul>
              @for (step of tutorial.steps; track step.title; let i = $index) {
                <li [class.active]="activeStepIndex === i">
                  <a (click)="scrollToStep(i)">{{ step.title }}</a>
                </li>
              }
            </ul>
          }
        </nav>
      </aside>

      <!-- Main content -->
      <div class="tut-content">
        @if (loading) {
          <div class="loading">Loading...</div>
        } @else if (tutorial) {
          <section class="tutorial-section">
            <h2>Tutorial {{ tutorial.number }}: {{ tutorial.title }}</h2>
            <p class="subtitle">{{ tutorial.subtitle }}</p>

            @for (step of tutorial.steps; track step.title; let i = $index) {
              <div class="tutorial-step" [id]="'step-' + i">
                <h3>{{ step.title }}</h3>
                <p [innerHTML]="step.prose"></p>
                @if (step.code) {
                  <app-code-block [code]="step.code" [label]="step.codeLabel || ''"></app-code-block>
                }
              </div>
            }

            <div class="tutorial-nav">
              @if (prevTutorial) {
                <a class="nav-prev" [routerLink]="['/tutorials', prevTutorial.id]">&larr; {{ prevTutorial.title }}</a>
              }
              <span class="nav-spacer"></span>
              @if (nextTutorial) {
                <a class="nav-next" [routerLink]="['/tutorials', nextTutorial.id]">{{ nextTutorial.title }} &rarr;</a>
              }
            </div>
          </section>
        } @else {
          <div class="empty-state">
            <p>Tutorial not found.</p>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    /* Sidebar layout */
    .tut-layout {
      display: flex;
      gap: 32px;
      align-items: flex-start;
      padding-top: 16px;
    }

    .tut-sidebar {
      position: sticky;
      top: 80px;
      width: 260px;
      flex-shrink: 0;
      max-height: calc(100vh - 100px);
      overflow-y: auto;
    }

    .back-link {
      display: block;
      padding: 8px 12px;
      font-size: 14px;
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
      margin-bottom: 16px;
    }
    .back-link:hover {
      text-decoration: underline;
    }

    .sidebar-nav h3 {
      font-size: 13px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: rgba(0, 0, 0, 0.5);
      margin: 0 0 12px;
      padding: 0 12px;
    }
    .sidebar-nav ul {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .sidebar-nav li {
      margin: 0;
    }
    .sidebar-nav li a {
      display: block;
      padding: 6px 12px;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.7);
      text-decoration: none;
      border-left: 3px solid transparent;
      cursor: pointer;
      transition: all 0.15s;
    }
    .sidebar-nav li a:hover {
      color: var(--brand-primary, #4338ca);
      background: rgba(67, 56, 202, 0.04);
    }
    .sidebar-nav li.active a {
      color: var(--brand-primary, #4338ca);
      border-left-color: var(--brand-primary, #4338ca);
      font-weight: 500;
      background: rgba(67, 56, 202, 0.06);
    }

    .tut-content {
      flex: 1;
      min-width: 0;
    }

    @media (max-width: 900px) {
      .tut-layout {
        flex-direction: column;
      }
      .tut-sidebar {
        position: static;
        width: 100%;
        max-height: none;
        border: 1px solid rgba(0, 0, 0, 0.08);
        border-radius: 8px;
        padding: 12px 0;
        background: rgba(0, 0, 0, 0.01);
      }
    }

    /* Content styles */
    .tutorial-section {
      margin: 0 0 40px;
    }
    .tutorial-section h2 {
      font-size: 22px;
      font-weight: 500;
      margin-bottom: 8px;
    }
    .subtitle {
      color: rgba(0, 0, 0, 0.6);
      line-height: 1.7;
      margin-bottom: 24px;
    }

    .tutorial-step {
      margin-bottom: 24px;
    }
    .tutorial-step h3 {
      font-size: 16px;
      font-weight: 500;
      margin: 0 0 12px;
    }
    .tutorial-step p {
      line-height: 1.7;
      margin: 8px 0;
    }
    .tutorial-step a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
    }
    .tutorial-step a:hover {
      text-decoration: underline;
    }

    .tutorial-nav {
      display: flex;
      align-items: center;
      padding: 24px 0;
      border-top: 1px solid rgba(0, 0, 0, 0.08);
      margin-top: 32px;
      gap: 16px;
    }
    .tutorial-nav a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
      font-size: 14px;
    }
    .tutorial-nav a:hover {
      text-decoration: underline;
    }
    .nav-prev {
      max-width: 45%;
    }
    .nav-next {
      max-width: 45%;
      text-align: right;
    }
    .nav-spacer {
      flex: 1;
    }

    .loading {
      padding: 48px 16px;
      text-align: center;
      color: rgba(0, 0, 0, 0.5);
      font-size: 16px;
    }

    .empty-state {
      padding: 48px 16px;
      text-align: center;
      color: rgba(0, 0, 0, 0.5);
    }
    .empty-state p {
      font-size: 16px;
      margin: 0;
    }
  `],
})
export class TutorialDetailComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private sub: Subscription | null = null;

  tutorial: TutorialDto | null = null;
  allTutorials: TutorialSummaryDto[] = [];
  prevTutorial: TutorialSummaryDto | null = null;
  nextTutorial: TutorialSummaryDto | null = null;
  activeStepIndex = -1;
  loading = false;

  ngOnInit(): void {
    this.sub = this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.loadTutorial(id);
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  private loadTutorial(id: string): void {
    this.loading = true;
    this.tutorial = null;
    this.activeStepIndex = -1;

    forkJoin({
      tutorial: this.api.getTutorial(id),
      list: this.api.getTutorials(),
    }).subscribe({
      next: ({ tutorial, list }) => {
        this.tutorial = tutorial;
        this.allTutorials = list;
        this.computePrevNext(tutorial, list);
        this.loading = false;
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  private computePrevNext(tutorial: TutorialDto, list: TutorialSummaryDto[]): void {
    const idx = list.findIndex((t) => t.id === tutorial.id);
    this.prevTutorial = idx > 0 ? list[idx - 1] : null;
    this.nextTutorial = idx < list.length - 1 ? list[idx + 1] : null;
  }

  scrollToStep(index: number): void {
    this.activeStepIndex = index;
    const el = document.getElementById('step-' + index);
    el?.scrollIntoView({ behavior: 'smooth' });
  }
}
