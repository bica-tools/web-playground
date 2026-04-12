import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../services/api.service';
import {
  AgentTypeDto,
  ProgrammeStatusDto,
  PublicationDto,
  StepEvaluationDto,
} from '../../models/api.models';

@Component({
  selector: 'app-monitoring',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatProgressBarModule,
    MatExpansionModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
  ],
  template: `
    <div class="monitoring-container">
      <h1 class="page-title">Monitoring Dashboard</h1>
      <p class="page-subtitle">Agent types, programme status, and step evaluations</p>

      <!-- ════════ Agent Types Section ════════ -->
      <section class="section" aria-label="Agent types">
        <h2 class="section-title">Agent Types</h2>

        @if (loadingAgents()) {
          <div class="loading-row">
            <mat-spinner diameter="32"></mat-spinner>
            <span>Loading agents...</span>
          </div>
        }

        @if (agentsError()) {
          <div class="error-banner">
            <mat-icon>error_outline</mat-icon>
            {{ agentsError() }}
          </div>
        }

        @if (agents().length > 0) {
          <div class="agents-grid">
            @for (agent of agents(); track agent.name) {
              <mat-card class="agent-card">
                <mat-card-header>
                  <mat-card-title>{{ agent.name }}</mat-card-title>
                  <mat-card-subtitle>
                    <mat-chip-set aria-label="Agent protocol">
                      <mat-chip [class.chip-mcp]="agent.protocol === 'MCP'"
                                [class.chip-a2a]="agent.protocol === 'A2A'"
                                highlighted>
                        {{ agent.protocol }}
                      </mat-chip>
                      <mat-chip>{{ agent.transport }}</mat-chip>
                    </mat-chip-set>
                  </mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <p class="agent-description">{{ agent.description }}</p>
                  <div class="session-type-block">
                    <code>{{ agent.sessionType }}</code>
                  </div>
                </mat-card-content>
              </mat-card>
            }
          </div>
        }
      </section>

      <!-- ════════ Programme Status Section ════════ -->
      <section class="section" aria-label="Programme status">
        <h2 class="section-title">Programme Status</h2>

        @if (loadingStatus()) {
          <div class="loading-row">
            <mat-spinner diameter="32"></mat-spinner>
            <span>Loading programme status...</span>
          </div>
        }

        @if (statusError()) {
          <div class="error-banner">
            <mat-icon>error_outline</mat-icon>
            {{ statusError() }}
          </div>
        }

        @if (status()) {
          <!-- Summary cards -->
          <div class="summary-grid">
            <div class="summary-card">
              <div class="summary-value">{{ status()!.totalSteps }}</div>
              <div class="summary-label">Total Steps</div>
            </div>
            <div class="summary-card">
              <div class="summary-value">{{ status()!.acceptedSteps }}</div>
              <div class="summary-label">Accepted</div>
            </div>
            <div class="summary-card">
              <div class="summary-value">{{ status()!.totalModules }}</div>
              <div class="summary-label">Modules</div>
            </div>
            <div class="summary-card">
              <div class="summary-value">{{ status()!.totalTests }}</div>
              <div class="summary-label">Tests</div>
            </div>
          </div>

          <!-- Progress bar -->
          <div class="progress-section">
            <div class="progress-header">
              <span class="progress-label">Accepted Steps</span>
              <span class="progress-value">{{ status()!.acceptedSteps }} / {{ status()!.totalSteps }}</span>
            </div>
            <mat-progress-bar
              mode="determinate"
              [value]="acceptedPercent()"
              aria-label="Accepted steps progress">
            </mat-progress-bar>
          </div>

          <!-- Steps table with expansion -->
          <mat-accordion multi>
            @for (step of status()!.steps; track step.stepNumber) {
              <mat-expansion-panel (opened)="onStepOpened(step)">
                <mat-expansion-panel-header>
                  <mat-panel-title>
                    <span class="step-number">{{ step.stepNumber }}</span>
                    <span class="step-title">{{ step.title }}</span>
                  </mat-panel-title>
                  <mat-panel-description>
                    <span class="grade-chip" [class]="gradeClass(step.grade)">
                      {{ step.grade }}
                    </span>
                    <span class="score-text">{{ step.score }}%</span>
                    @if (step.accepted) {
                      <mat-icon class="accepted-icon">check_circle</mat-icon>
                    }
                  </mat-panel-description>
                </mat-expansion-panel-header>

                <div class="step-details">
                  <div class="step-meta">
                    <span><strong>Grade:</strong> {{ step.grade }}</span>
                    <span><strong>Score:</strong> {{ step.score }}%</span>
                    <span><strong>Accepted:</strong> {{ step.accepted ? 'Yes' : 'No' }}</span>
                  </div>

                  @if (step.fixes && step.fixes.length > 0) {
                    <div class="fixes-section">
                      <h4>Fixes Required</h4>
                      <ul class="fixes-list">
                        @for (fix of step.fixes; track fix) {
                          <li>{{ fix }}</li>
                        }
                      </ul>
                    </div>
                  } @else {
                    <p class="no-fixes">No fixes required.</p>
                  }

                  <button mat-stroked-button
                          (click)="evaluateStep(step.stepNumber)"
                          [disabled]="evaluatingStep() === step.stepNumber">
                    @if (evaluatingStep() === step.stepNumber) {
                      <mat-spinner diameter="16"></mat-spinner>
                    } @else {
                      Re-evaluate
                    }
                  </button>
                </div>
              </mat-expansion-panel>
            }
          </mat-accordion>
        }
      </section>

      <!-- ════════ Publication Calendar Section ════════ -->
      <section class="section" aria-label="Publication calendar">
        <h2 class="section-title">Publication Calendar</h2>

        @if (loadingPublications()) {
          <div class="loading-row">
            <mat-spinner diameter="32"></mat-spinner>
            <span>Loading publications...</span>
          </div>
        }

        @if (publicationsError()) {
          <div class="error-banner">
            <mat-icon>error_outline</mat-icon>
            {{ publicationsError() }}
          </div>
        }

        @if (publications().length > 0) {
          <!-- Next deadline countdown -->
          @if (nextDeadline()) {
            <div class="countdown-banner" [class]="countdownUrgencyClass()">
              <mat-icon>schedule</mat-icon>
              <span class="countdown-text">
                <strong>Next deadline:</strong> {{ nextDeadline()!.deadline }}
                ({{ nextDeadline()!.id }} — {{ nextDeadline()!.title | slice:0:50 }})
                — <strong>{{ daysUntilNextDeadline() }}</strong> days remaining
              </span>
            </div>
          }

          <!-- Publications table -->
          <div class="pub-table-wrapper">
            <table class="pub-table">
              <thead>
                <tr>
                  <th>Deadline</th>
                  <th>ID</th>
                  <th>Title</th>
                  <th>Venue</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                @for (pub of sortedPublications(); track pub.id) {
                  <tr [class]="priorityRowClass(pub.priority)">
                    <td class="pub-deadline">{{ pub.deadline }}</td>
                    <td class="pub-id">{{ pub.id }}</td>
                    <td class="pub-title">{{ pub.title }}</td>
                    <td class="pub-venue">{{ pub.venues[0] || '' }}</td>
                    <td>
                      <span class="status-chip" [class]="statusChipClass(pub.status)">
                        {{ pub.status }}
                      </span>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Legend -->
          <div class="pub-legend">
            <span class="legend-item"><span class="legend-dot priority-immediate"></span> Immediate</span>
            <span class="legend-item"><span class="legend-dot priority-short-term"></span> Short-term</span>
            <span class="legend-item"><span class="legend-dot priority-medium-term"></span> Medium-term</span>
            <span class="legend-item"><span class="legend-dot priority-long-term"></span> Long-term</span>
          </div>
        }
      </section>
    </div>
  `,
  styles: [`
    .monitoring-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 32px 24px;
    }

    .page-title {
      font-size: 2rem;
      font-weight: 700;
      margin: 0 0 4px;
    }

    .page-subtitle {
      color: #666;
      margin: 0 0 32px;
      font-size: 1rem;
    }

    .section {
      margin-bottom: 48px;
    }

    .section-title {
      font-size: 1.4rem;
      font-weight: 600;
      margin: 0 0 16px;
      padding-bottom: 8px;
      border-bottom: 2px solid #e0e0e0;
    }

    .loading-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 24px 0;
      color: #666;
    }

    .error-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: #fdecea;
      color: #b71c1c;
      border-radius: 8px;
      margin-bottom: 16px;
    }

    /* Agent cards */
    .agents-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
      gap: 16px;
    }

    .agent-card {
      border-radius: 12px;
    }

    .agent-description {
      margin: 8px 0;
      color: #555;
      font-size: 0.9rem;
    }

    .session-type-block {
      background: #f5f5f5;
      border-radius: 6px;
      padding: 8px 12px;
      overflow-x: auto;
      margin-top: 8px;
    }

    .session-type-block code {
      font-size: 0.8rem;
      white-space: pre-wrap;
      word-break: break-all;
    }

    .chip-mcp {
      --mdc-chip-elevated-container-color: #e3f2fd;
      --mdc-chip-label-text-color: #1565c0;
    }

    .chip-a2a {
      --mdc-chip-elevated-container-color: #fce4ec;
      --mdc-chip-label-text-color: #c62828;
    }

    /* Summary */
    .summary-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      margin-bottom: 24px;
    }

    .summary-card {
      text-align: center;
      padding: 20px;
      background: #fafafa;
      border-radius: 12px;
      border: 1px solid #e0e0e0;
    }

    .summary-value {
      font-size: 2rem;
      font-weight: 700;
      color: #1a1a2e;
    }

    .summary-label {
      font-size: 0.85rem;
      color: #888;
      margin-top: 4px;
    }

    /* Progress */
    .progress-section {
      margin-bottom: 24px;
    }

    .progress-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
    }

    .progress-label {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .progress-value {
      font-size: 0.9rem;
      color: #666;
    }

    /* Steps accordion */
    .step-number {
      font-family: monospace;
      font-weight: 600;
      margin-right: 12px;
      min-width: 60px;
    }

    .step-title {
      font-weight: 500;
    }

    .grade-chip {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 12px;
      font-size: 0.8rem;
      font-weight: 700;
      margin-right: 8px;
    }

    .grade-a-plus {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .grade-a {
      background: #e3f2fd;
      color: #1565c0;
    }

    .grade-b {
      background: #fff8e1;
      color: #f57f17;
    }

    .grade-c, .grade-f {
      background: #fdecea;
      color: #b71c1c;
    }

    .score-text {
      font-size: 0.85rem;
      color: #666;
      margin-right: 8px;
    }

    .accepted-icon {
      color: #2e7d32;
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .step-details {
      padding: 8px 0;
    }

    .step-meta {
      display: flex;
      gap: 24px;
      margin-bottom: 12px;
      font-size: 0.9rem;
    }

    .fixes-section h4 {
      margin: 0 0 8px;
      font-size: 0.9rem;
    }

    .fixes-list {
      margin: 0;
      padding-left: 20px;
    }

    .fixes-list li {
      margin-bottom: 4px;
      font-size: 0.9rem;
      color: #555;
    }

    .no-fixes {
      color: #2e7d32;
      font-size: 0.9rem;
    }

    /* Publication calendar */
    .countdown-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      border-radius: 8px;
      margin-bottom: 16px;
      font-size: 0.9rem;
    }

    .countdown-urgent {
      background: #fdecea;
      color: #b71c1c;
    }

    .countdown-soon {
      background: #fff8e1;
      color: #e65100;
    }

    .countdown-normal {
      background: #e3f2fd;
      color: #1565c0;
    }

    .pub-table-wrapper {
      overflow-x: auto;
      margin-bottom: 16px;
    }

    .pub-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.9rem;
    }

    .pub-table th {
      text-align: left;
      padding: 10px 12px;
      font-weight: 600;
      border-bottom: 2px solid #e0e0e0;
      color: #555;
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .pub-table td {
      padding: 10px 12px;
      border-bottom: 1px solid #f0f0f0;
    }

    .pub-table tbody tr:hover {
      background: #fafafa;
    }

    .row-immediate {
      border-left: 3px solid #d32f2f;
    }

    .row-short-term {
      border-left: 3px solid #f57c00;
    }

    .row-medium-term {
      border-left: 3px solid #1976d2;
    }

    .row-long-term {
      border-left: 3px solid #9e9e9e;
    }

    .pub-deadline {
      white-space: nowrap;
      font-family: monospace;
      font-size: 0.85rem;
    }

    .pub-id {
      font-family: monospace;
      font-weight: 600;
      white-space: nowrap;
    }

    .pub-title {
      max-width: 400px;
    }

    .pub-venue {
      white-space: nowrap;
      font-size: 0.85rem;
      color: #555;
    }

    .status-chip {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 12px;
      font-size: 0.8rem;
      font-weight: 600;
      white-space: nowrap;
    }

    .status-drafting {
      background: #fff8e1;
      color: #f57f17;
    }

    .status-planned {
      background: #f5f5f5;
      color: #757575;
    }

    .status-submitted {
      background: #e3f2fd;
      color: #1565c0;
    }

    .status-accepted {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .status-published {
      background: #e8f5e9;
      color: #1b5e20;
    }

    .pub-legend {
      display: flex;
      gap: 20px;
      font-size: 0.8rem;
      color: #888;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .legend-dot {
      display: inline-block;
      width: 10px;
      height: 10px;
      border-radius: 2px;
    }

    .priority-immediate {
      background: #d32f2f;
    }

    .priority-short-term {
      background: #f57c00;
    }

    .priority-medium-term {
      background: #1976d2;
    }

    .priority-long-term {
      background: #9e9e9e;
    }

    @media (max-width: 768px) {
      .summary-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      .agents-grid {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class MonitoringComponent implements OnInit {
  readonly agents = signal<AgentTypeDto[]>([]);
  readonly loadingAgents = signal(false);
  readonly agentsError = signal('');

  readonly status = signal<ProgrammeStatusDto | null>(null);
  readonly loadingStatus = signal(false);
  readonly statusError = signal('');

  readonly publications = signal<PublicationDto[]>([]);
  readonly loadingPublications = signal(false);
  readonly publicationsError = signal('');

  readonly evaluatingStep = signal<string | null>(null);

  readonly sortedPublications = computed(() => {
    const pubs = [...this.publications()];
    const priorityOrder: Record<string, number> = {
      immediate: 0, 'short-term': 1, 'medium-term': 2, 'long-term': 3,
    };
    return pubs.sort((a, b) => {
      const pa = priorityOrder[a.priority] ?? 4;
      const pb = priorityOrder[b.priority] ?? 4;
      if (pa !== pb) return pa - pb;
      return a.deadline.localeCompare(b.deadline);
    });
  });

  readonly nextDeadline = computed(() => {
    const pubs = this.sortedPublications();
    return pubs.find(p => p.deadline && p.deadline !== 'rolling') || null;
  });

  readonly daysUntilNextDeadline = computed(() => {
    const next = this.nextDeadline();
    if (!next) return 0;
    const match = next.deadline.match(/(\w+ \d+, \d{4})/);
    if (!match) return 0;
    const target = new Date(match[1]);
    if (isNaN(target.getTime())) return 0;
    const now = new Date();
    return Math.ceil((target.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
  });

  readonly acceptedPercent = computed(() => {
    const s = this.status();
    if (!s || s.totalSteps === 0) return 0;
    return Math.round((s.acceptedSteps / s.totalSteps) * 100);
  });

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.loadAgents();
    this.loadStatus();
    this.loadPublications();
  }

  loadAgents(): void {
    this.loadingAgents.set(true);
    this.agentsError.set('');
    this.api.getAgentTypes().subscribe({
      next: (agents) => {
        this.agents.set(agents);
        this.loadingAgents.set(false);
      },
      error: (err) => {
        this.agentsError.set(err.error?.error || err.message || 'Failed to load agents');
        this.loadingAgents.set(false);
      },
    });
  }

  loadStatus(): void {
    this.loadingStatus.set(true);
    this.statusError.set('');
    this.api.getProgrammeStatus().subscribe({
      next: (status) => {
        this.status.set(status);
        this.loadingStatus.set(false);
      },
      error: (err) => {
        this.statusError.set(err.error?.error || err.message || 'Failed to load status');
        this.loadingStatus.set(false);
      },
    });
  }

  loadPublications(): void {
    this.loadingPublications.set(true);
    this.publicationsError.set('');
    this.api.getPublications().subscribe({
      next: (pubs) => {
        this.publications.set(pubs);
        this.loadingPublications.set(false);
      },
      error: (err) => {
        this.publicationsError.set(err.error?.error || err.message || 'Failed to load publications');
        this.loadingPublications.set(false);
      },
    });
  }

  priorityRowClass(priority: string): string {
    switch (priority) {
      case 'immediate': return 'row-immediate';
      case 'short-term': return 'row-short-term';
      case 'medium-term': return 'row-medium-term';
      case 'long-term': return 'row-long-term';
      default: return '';
    }
  }

  statusChipClass(status: string): string {
    switch (status) {
      case 'drafting': return 'status-chip status-drafting';
      case 'planned': return 'status-chip status-planned';
      case 'submitted': return 'status-chip status-submitted';
      case 'accepted': return 'status-chip status-accepted';
      case 'published': return 'status-chip status-published';
      default: return 'status-chip status-planned';
    }
  }

  countdownUrgencyClass(): string {
    const days = this.daysUntilNextDeadline();
    if (days <= 14) return 'countdown-banner countdown-urgent';
    if (days <= 45) return 'countdown-banner countdown-soon';
    return 'countdown-banner countdown-normal';
  }

  onStepOpened(step: StepEvaluationDto): void {
    // No-op; data already available from programme status
  }

  evaluateStep(stepNumber: string): void {
    this.evaluatingStep.set(stepNumber);
    this.api.evaluateStep(stepNumber).subscribe({
      next: (result) => {
        // Update the step in the status
        const current = this.status();
        if (current) {
          const updatedSteps = current.steps.map((s) =>
            s.stepNumber === stepNumber ? result : s,
          );
          const acceptedCount = updatedSteps.filter((s) => s.accepted).length;
          this.status.set({
            ...current,
            steps: updatedSteps,
            acceptedSteps: acceptedCount,
          });
        }
        this.evaluatingStep.set(null);
      },
      error: () => {
        this.evaluatingStep.set(null);
      },
    });
  }

  gradeClass(grade: string): string {
    switch (grade) {
      case 'A+': return 'grade-a-plus';
      case 'A': return 'grade-a';
      case 'B': return 'grade-b';
      case 'C':
      case 'F': return 'grade-c';
      default: return '';
    }
  }
}
