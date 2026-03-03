import { Component, OnInit, inject } from '@angular/core';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';
import { ApiService } from '../../services/api.service';
import { TutorialSummaryDto, TutorialDto } from '../../models/api.models';

@Component({
  selector: 'app-tutorials',
  standalone: true,
  imports: [CodeBlockComponent],
  template: `
    <header class="page-header">
      <h1>Tutorials</h1>
      <p>Step-by-step guides to session types, reticulates, and tooling.</p>
    </header>

    <div class="tut-layout">
      <!-- Sticky sidebar -->
      <aside class="tut-sidebar">
        <nav class="sidebar-nav">
          <h3>Contents</h3>
          <ul>
            @for (tut of tutorials; track tut.id) {
              <li [class.active]="activeTutorialId === tut.id && activeStepIndex === -1">
                <a (click)="selectTutorial(tut.id)">{{ tut.number }}. {{ tut.title }}</a>
              </li>
              @if (activeTutorialId === tut.id && activeTutorial) {
                @for (step of activeTutorial.steps; track step.title; let i = $index) {
                  <li class="sub" [class.active]="activeStepIndex === i">
                    <a (click)="scrollToStep(i)">{{ step.title }}</a>
                  </li>
                }
              }
            }
          </ul>
        </nav>
      </aside>

      <!-- Main content -->
      <div class="tut-content">
        @if (loading) {
          <div class="loading">Loading...</div>
        } @else if (activeTutorial) {
          <section class="tutorial-section">
            <h2>Tutorial {{ activeTutorial.number }}: {{ activeTutorial.title }}</h2>
            <p class="subtitle">{{ activeTutorial.subtitle }}</p>

            @for (step of activeTutorial.steps; track step.title; let i = $index) {
              <div class="tutorial-step" [id]="'step-' + i">
                <h3>{{ step.title }}</h3>
                <p [innerHTML]="step.prose"></p>
                @if (step.code) {
                  <app-code-block [code]="step.code" [label]="step.codeLabel || ''"></app-code-block>
                }
              </div>
            }

            <div class="tutorial-nav">
              @if (activeTutorial.number > 1) {
                <a class="nav-prev" (click)="selectTutorialByNumber(activeTutorial.number - 1)">&larr; Previous</a>
              }
              <span class="nav-spacer"></span>
              @if (activeTutorial.number < tutorials.length) {
                <a class="nav-next" (click)="selectTutorialByNumber(activeTutorial.number + 1)">Next &rarr;</a>
              }
            </div>
          </section>
        } @else {
          <div class="empty-state">
            <p>Select a tutorial from the sidebar to get started.</p>
          </div>
        }
      </div>
    </div>
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

    /* Sidebar layout */
    .tut-layout {
      display: flex;
      gap: 32px;
      align-items: flex-start;
    }

    .tut-sidebar {
      position: sticky;
      top: 80px;
      width: 260px;
      flex-shrink: 0;
      max-height: calc(100vh - 100px);
      overflow-y: auto;
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
    .sidebar-nav li.sub a {
      padding-left: 24px;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.55);
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
    .sidebar-nav li.active.sub a {
      font-weight: 500;
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
      margin: 16px 0 40px;
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
    }
    .tutorial-nav a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
      font-size: 15px;
      cursor: pointer;
    }
    .tutorial-nav a:hover {
      text-decoration: underline;
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
export class TutorialsComponent implements OnInit {
  private api = inject(ApiService);

  tutorials: TutorialSummaryDto[] = [];
  activeTutorial: TutorialDto | null = null;
  activeTutorialId: string | null = null;
  activeStepIndex = -1;
  loading = false;

  ngOnInit(): void {
    this.api.getTutorials().subscribe((list) => {
      this.tutorials = list;
      if (list.length > 0) {
        this.selectTutorial(list[0].id);
      }
    });
  }

  selectTutorial(id: string): void {
    if (this.activeTutorialId === id && this.activeTutorial) {
      return;
    }
    this.activeTutorialId = id;
    this.activeTutorial = null;
    this.activeStepIndex = -1;
    this.loading = true;
    this.api.getTutorial(id).subscribe((tut) => {
      this.activeTutorial = tut;
      this.loading = false;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  selectTutorialByNumber(num: number): void {
    const tut = this.tutorials.find((t) => t.number === num);
    if (tut) {
      this.selectTutorial(tut.id);
    }
  }

  scrollToStep(index: number): void {
    this.activeStepIndex = index;
    const el = document.getElementById('step-' + index);
    el?.scrollIntoView({ behavior: 'smooth' });
  }
}
