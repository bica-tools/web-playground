import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import {
  RETICULATE_MODULES, BICA_PHASES, LEAN_PROOFS, PAPERS, MILESTONES, SUMMARY,
  type ModuleStatus, type PaperStatus, type MilestoneStatus, type LeanStatus,
} from './project-status';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule, MatIconModule, MatChipsModule, MatProgressBarModule, MatTableModule],
  template: `
    <div class="dashboard">
      <h1 class="page-title">Project Dashboard</h1>
      <p class="page-subtitle">Session Types as Algebraic Reticulates — Research Overview</p>

      <!-- Summary cards -->
      <section class="summary-row">
        <mat-card class="stat-card">
          <div class="stat-value">{{ summary.totalPythonTests + summary.totalJavaTests }}</div>
          <div class="stat-label">Total Tests</div>
          <div class="stat-detail">{{ summary.totalPythonTests }} Python · {{ summary.totalJavaTests }} Java</div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-value">{{ summary.totalBenchmarks }}</div>
          <div class="stat-label">Benchmarks</div>
          <div class="stat-detail">{{ summary.benchmarksWithParallel }} with ∥ ({{ pctParallel }}%)</div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-value">{{ summary.leanSorryCount }}</div>
          <div class="stat-label">Lean sorry</div>
          <div class="stat-detail">2 lemmas fully proved</div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-value">{{ papers.length }}</div>
          <div class="stat-label">Papers</div>
          <div class="stat-detail">{{ draftCount }} draft · {{ submittedCount }} submitted</div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-value">{{ summary.generatedTests }}</div>
          <div class="stat-label">Generated Tests</div>
          <div class="stat-detail">JUnit 5 from 34 protocols</div>
        </mat-card>
      </section>

      <!-- Upcoming deadlines -->
      <section class="section">
        <h2>
          <mat-icon>schedule</mat-icon>
          Upcoming Deadlines
        </h2>
        <div class="deadline-cards">
          @for (m of upcomingMilestones; track m.label) {
            <mat-card class="deadline-card" [class.overdue]="daysUntil(m.date) < 0">
              <div class="deadline-days" [class.urgent]="daysUntil(m.date) <= 14 && daysUntil(m.date) >= 0">
                @if (daysUntil(m.date) >= 0) {
                  {{ daysUntil(m.date) }}d
                } @else {
                  overdue
                }
              </div>
              <div class="deadline-info">
                <div class="deadline-label">{{ m.label }}</div>
                <div class="deadline-date">{{ m.date }}</div>
              </div>
            </mat-card>
          }
        </div>
      </section>

      <!-- Reticulate Python -->
      <section class="section">
        <h2>
          <mat-icon>code</mat-icon>
          Reticulate (Python) — {{ reticulateTests }} tests
        </h2>
        <mat-progress-bar mode="determinate" [value]="reticulateProgress"></mat-progress-bar>
        <div class="module-grid">
          @for (mod of reticulateModules; track mod.name) {
            <mat-card class="module-card">
              <div class="module-header">
                <mat-icon class="status-icon complete">check_circle</mat-icon>
                <span class="module-name">{{ mod.name }}</span>
                @if (mod.tests > 0) {
                  <mat-chip-set>
                    <mat-chip>{{ mod.tests }} tests</mat-chip>
                  </mat-chip-set>
                }
              </div>
              <div class="module-desc">{{ mod.description }}</div>
            </mat-card>
          }
        </div>
      </section>

      <!-- BICA Reborn Java -->
      <section class="section">
        <h2>
          <mat-icon>integration_instructions</mat-icon>
          BICA Reborn (Java) — {{ bicaTests }} tests
        </h2>
        <mat-progress-bar mode="determinate" [value]="bicaProgress"></mat-progress-bar>
        <div class="module-grid">
          @for (phase of bicaPhases; track phase.name) {
            <mat-card class="module-card">
              <div class="module-header">
                <mat-icon class="status-icon complete">check_circle</mat-icon>
                <span class="module-name">{{ phase.name }}</span>
                <mat-chip-set>
                  <mat-chip>{{ phase.tests }} tests</mat-chip>
                </mat-chip-set>
              </div>
              <div class="module-desc">{{ phase.description }}</div>
            </mat-card>
          }
        </div>
      </section>

      <!-- Lean 4 -->
      <section class="section">
        <h2>
          <mat-icon>functions</mat-icon>
          Lean 4 Formalization
        </h2>
        <mat-progress-bar mode="determinate" [value]="leanProgress"></mat-progress-bar>
        <div class="module-grid">
          @for (proof of leanProofs; track proof.name) {
            <mat-card class="module-card">
              <div class="module-header">
                @if (proof.status === 'complete') {
                  <mat-icon class="status-icon complete">check_circle</mat-icon>
                } @else if (proof.status === 'in-progress') {
                  <mat-icon class="status-icon in-progress">pending</mat-icon>
                } @else {
                  <mat-icon class="status-icon planned">radio_button_unchecked</mat-icon>
                }
                <span class="module-name">{{ proof.name }}</span>
                @if (proof.sorryCount >= 0) {
                  <mat-chip-set>
                    <mat-chip [class.sorry-zero]="proof.sorryCount === 0">
                      {{ proof.sorryCount }} sorry
                    </mat-chip>
                  </mat-chip-set>
                }
              </div>
              <div class="module-desc">{{ proof.description }}</div>
            </mat-card>
          }
        </div>
      </section>

      <!-- Papers pipeline -->
      <section class="section">
        <h2>
          <mat-icon>article</mat-icon>
          Papers Pipeline
        </h2>
        <div class="papers-kanban">
          @for (col of paperColumns; track col.label) {
            <div class="kanban-column">
              <div class="kanban-header">{{ col.label }}</div>
              @for (paper of col.papers; track paper.shortName) {
                <mat-card class="paper-card">
                  <div class="paper-title">{{ paper.shortName }}</div>
                  <div class="paper-target">{{ paper.target }}</div>
                  @if (paper.deadline) {
                    <div class="paper-deadline"
                         [class.urgent]="daysUntil(paper.deadline) <= 30 && daysUntil(paper.deadline) >= 0">
                      {{ paper.deadline }} ({{ daysUntil(paper.deadline) }}d)
                    </div>
                  }
                  <div class="paper-pages">{{ paper.pages }} pp</div>
                </mat-card>
              }
              @if (col.papers.length === 0) {
                <div class="kanban-empty">—</div>
              }
            </div>
          }
        </div>
      </section>

      <!-- Milestones timeline -->
      <section class="section">
        <h2>
          <mat-icon>flag</mat-icon>
          Milestones
        </h2>
        <div class="timeline">
          @for (m of milestones; track m.label) {
            <div class="timeline-item" [class.done]="m.done">
              <div class="timeline-dot">
                @if (m.done) {
                  <mat-icon>check</mat-icon>
                }
              </div>
              <div class="timeline-content">
                <span class="timeline-label">{{ m.label }}</span>
                <span class="timeline-date">{{ m.date }}</span>
              </div>
            </div>
          }
        </div>
      </section>
    </div>
  `,
  styles: [`
    .dashboard {
      max-width: 1100px;
      margin: 0 auto;
      padding: 32px 24px;
    }
    .page-title {
      font-size: 24px;
      font-weight: 600;
      margin: 0 0 8px;
    }
    .page-subtitle {
      color: rgba(0,0,0,0.6);
      margin: 0 0 32px;
    }

    /* Summary row */
    .summary-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 16px;
      margin-bottom: 40px;
    }
    .stat-card {
      padding: 20px;
      text-align: center;
    }
    .stat-value {
      font-size: 36px;
      font-weight: 700;
      color: var(--mat-sys-primary, #4338ca);
    }
    .stat-label {
      font-size: 14px;
      font-weight: 500;
      margin-top: 4px;
    }
    .stat-detail {
      font-size: 12px;
      color: rgba(0,0,0,0.5);
      margin-top: 4px;
    }

    /* Sections */
    .section {
      margin-bottom: 40px;
    }
    .section h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 20px;
      font-weight: 600;
      margin-bottom: 16px;
    }
    mat-progress-bar {
      margin-bottom: 16px;
      border-radius: 4px;
    }

    /* Module grid */
    .module-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 12px;
    }
    .module-card {
      padding: 16px;
    }
    .module-header {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .module-name {
      font-weight: 600;
      flex: 1;
    }
    .module-desc {
      font-size: 13px;
      color: rgba(0,0,0,0.6);
      margin-top: 8px;
    }
    .status-icon.complete { color: #16a34a; }
    .status-icon.in-progress { color: #d97706; }
    .status-icon.planned { color: rgba(0,0,0,0.3); }

    .sorry-zero {
      background-color: #dcfce7 !important;
      color: #16a34a !important;
    }

    /* Deadlines */
    .deadline-cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 12px;
    }
    .deadline-card {
      padding: 16px;
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .deadline-card.overdue {
      border-left: 4px solid #dc2626;
    }
    .deadline-days {
      font-size: 24px;
      font-weight: 700;
      min-width: 60px;
      text-align: center;
      color: rgba(0,0,0,0.7);
    }
    .deadline-days.urgent {
      color: #dc2626;
    }
    .deadline-info {
      flex: 1;
    }
    .deadline-label {
      font-weight: 500;
      font-size: 14px;
    }
    .deadline-date {
      font-size: 12px;
      color: rgba(0,0,0,0.5);
    }

    /* Papers kanban */
    .papers-kanban {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
    }
    .kanban-column {
      background: rgba(0,0,0,0.03);
      border-radius: 8px;
      padding: 12px;
      min-height: 120px;
    }
    .kanban-header {
      font-weight: 600;
      font-size: 13px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: rgba(0,0,0,0.5);
      margin-bottom: 12px;
      text-align: center;
    }
    .kanban-empty {
      text-align: center;
      color: rgba(0,0,0,0.2);
      padding: 20px;
    }
    .paper-card {
      padding: 12px;
      margin-bottom: 8px;
    }
    .paper-title {
      font-weight: 600;
      font-size: 14px;
    }
    .paper-target {
      font-size: 12px;
      color: rgba(0,0,0,0.5);
      margin-top: 2px;
    }
    .paper-deadline {
      font-size: 12px;
      margin-top: 4px;
      color: rgba(0,0,0,0.6);
    }
    .paper-deadline.urgent {
      color: #dc2626;
      font-weight: 600;
    }
    .paper-pages {
      font-size: 11px;
      color: rgba(0,0,0,0.4);
      margin-top: 2px;
    }

    /* Timeline */
    .timeline {
      position: relative;
      padding-left: 32px;
    }
    .timeline::before {
      content: '';
      position: absolute;
      left: 11px;
      top: 0;
      bottom: 0;
      width: 2px;
      background: rgba(0,0,0,0.12);
    }
    .timeline-item {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      margin-bottom: 16px;
      position: relative;
    }
    .timeline-dot {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: rgba(0,0,0,0.08);
      display: flex;
      align-items: center;
      justify-content: center;
      position: absolute;
      left: -32px;
      z-index: 1;
    }
    .timeline-item.done .timeline-dot {
      background: #16a34a;
      color: white;
    }
    .timeline-dot mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }
    .timeline-content {
      display: flex;
      justify-content: space-between;
      width: 100%;
      padding: 2px 0;
    }
    .timeline-label {
      font-size: 14px;
    }
    .timeline-item.done .timeline-label {
      color: rgba(0,0,0,0.5);
    }
    .timeline-date {
      font-size: 13px;
      color: rgba(0,0,0,0.4);
      white-space: nowrap;
      margin-left: 16px;
    }

    @media (max-width: 768px) {
      .papers-kanban {
        grid-template-columns: 1fr;
      }
      .summary-row {
        grid-template-columns: repeat(2, 1fr);
      }
      .module-grid {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class DashboardComponent {
  readonly reticulateModules = RETICULATE_MODULES;
  readonly bicaPhases = BICA_PHASES;
  readonly leanProofs = LEAN_PROOFS;
  readonly papers = PAPERS;
  readonly milestones = MILESTONES;
  readonly summary = SUMMARY;

  readonly reticulateTests = RETICULATE_MODULES.reduce((s, m) => s + m.tests, 0);
  readonly bicaTests = BICA_PHASES.reduce((s, m) => s + m.tests, 0);
  readonly pctParallel = Math.round((SUMMARY.benchmarksWithParallel / SUMMARY.totalBenchmarks) * 100);

  readonly reticulateProgress = (RETICULATE_MODULES.filter(m => m.status === 'complete').length / RETICULATE_MODULES.length) * 100;
  readonly bicaProgress = (BICA_PHASES.filter(m => m.status === 'complete').length / BICA_PHASES.length) * 100;
  readonly leanProgress = (LEAN_PROOFS.filter(m => m.status === 'complete').length / LEAN_PROOFS.length) * 100;

  readonly draftCount = PAPERS.filter(p => p.status === 'draft').length;
  readonly submittedCount = PAPERS.filter(p => p.status === 'submitted').length;

  readonly upcomingMilestones = MILESTONES.filter(m => !m.done).slice(0, 5);

  readonly paperColumns = [
    { label: 'Draft', papers: PAPERS.filter(p => p.status === 'draft') },
    { label: 'Submitted', papers: PAPERS.filter(p => p.status === 'submitted') },
    { label: 'Under Review', papers: PAPERS.filter(p => p.status === 'under-review') },
    { label: 'Accepted', papers: PAPERS.filter(p => p.status === 'accepted' || p.status === 'published') },
  ];

  daysUntil(dateStr: string): number {
    const target = new Date(dateStr);
    const now = new Date();
    return Math.ceil((target.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
  }
}
