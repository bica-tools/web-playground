import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { HasseDiagramComponent } from '../../components/hasse-diagram/hasse-diagram.component';
import { ApiService } from '../../services/api.service';
import {
  CompositionResponse,
  ParticipantEntry,
} from '../../models/api.models';

interface ParticipantRow {
  name: string;
  typeString: string;
}

@Component({
  selector: 'app-composition',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    HasseDiagramComponent,
  ],
  template: `
    <div class="page-container">
      <h1>Composition</h1>
      <p class="subtitle">
        Compose multiple participant session types bottom-up.
        Compare free product (all interleavings) vs synchronized product (shared-label sync).
      </p>

      <!-- Participant inputs -->
      <div class="input-section">
        <label>Participants</label>
        @for (row of rows; track $index) {
          <div class="participant-row">
            <input
              [(ngModel)]="row.name"
              placeholder="Name (e.g. Client)"
              class="name-input"
            />
            <input
              [(ngModel)]="row.typeString"
              placeholder="Session type (e.g. &amp;{request: end})"
              class="type-input"
              (keydown.control.enter)="compose()"
              (keydown.meta.enter)="compose()"
            />
            @if (rows.length > 2) {
              <button mat-icon-button (click)="removeRow($index)" title="Remove">
                <mat-icon>close</mat-icon>
              </button>
            }
          </div>
        }
        <button mat-stroked-button (click)="addRow()" class="add-btn">
          <mat-icon>add</mat-icon> Add Participant
        </button>
      </div>

      <!-- Optional global type -->
      <div class="input-section">
        <label for="global-input">Global type (optional, for top-down comparison)</label>
        <input
          id="global-input"
          [(ngModel)]="globalType"
          placeholder="e.g. Client -> Server : {request: Server -> Client : {response: end}}"
          class="type-input full-width"
        />
      </div>

      <div class="input-actions">
        <button mat-raised-button color="primary" (click)="compose()" [disabled]="loading()">
          <mat-icon>merge_type</mat-icon>
          Compose
        </button>
        <span class="hint">Ctrl+Enter</span>
      </div>

      <!-- Quick examples -->
      <div class="examples">
        <span class="examples-label">Examples:</span>
        @for (ex of examples; track ex.name) {
          <button mat-stroked-button (click)="loadExample(ex)" class="example-btn">
            {{ ex.name }}
          </button>
        }
      </div>

      <!-- Loading -->
      @if (loading()) {
        <div class="loading">
          <mat-spinner diameter="40"></mat-spinner>
          <span>Composing...</span>
        </div>
      }

      <!-- Error -->
      @if (error()) {
        <mat-card class="error-card">
          <mat-card-content>
            <mat-icon>error</mat-icon>
            <span>{{ error() }}</span>
          </mat-card-content>
        </mat-card>
      }

      <!-- Results -->
      @if (result(); as r) {
        <mat-tab-group class="results-tabs" animationDuration="0">

          <!-- Overview -->
          <mat-tab label="Overview">
            <div class="tab-content">
              <div class="verdict-grid">
                <div class="verdict-item pass">
                  <mat-icon>group</mat-icon>
                  <span>{{ r.participantCount }} participants</span>
                </div>
                <div class="verdict-item" [class.pass]="r.syncedIsLattice" [class.fail]="!r.syncedIsLattice">
                  <mat-icon>{{ r.syncedIsLattice ? 'check_circle' : 'cancel' }}</mat-icon>
                  <span>Synced lattice</span>
                </div>
                <div class="verdict-item" [class.pass]="r.freeIsLattice" [class.fail]="!r.freeIsLattice">
                  <mat-icon>{{ r.freeIsLattice ? 'check_circle' : 'cancel' }}</mat-icon>
                  <span>Free lattice</span>
                </div>
                <div class="verdict-item info">
                  <mat-icon>compress</mat-icon>
                  <span>{{ (r.reductionRatio * 100) | number:'1.0-0' }}% reduction</span>
                </div>
              </div>

              <h3>Composition Hierarchy</h3>
              <div class="metrics-grid three-col">
                <div class="metric">
                  <span class="metric-label">Free Product</span>
                  <span class="metric-value">{{ r.freeStates }}</span>
                  <span class="metric-detail">states, {{ r.freeTransitions }} transitions</span>
                </div>
                <div class="metric highlight">
                  <span class="metric-label">Synchronized</span>
                  <span class="metric-value">{{ r.syncedStates }}</span>
                  <span class="metric-detail">states, {{ r.syncedTransitions }} transitions</span>
                </div>
                @if (r.globalComparison) {
                  <div class="metric">
                    <span class="metric-label">Global (top-down)</span>
                    <span class="metric-value">{{ r.globalComparison.globalStates }}</span>
                    <span class="metric-detail">states</span>
                  </div>
                }
              </div>

              <!-- Resource estimate -->
              <h3>Resource Estimate</h3>
              <div class="resource-box">
                <p>
                  The <strong>free product</strong> has <strong>{{ r.freeStates }}</strong> states
                  (all interleavings). Synchronization reduces this to <strong>{{ r.syncedStates }}</strong> states
                  — a <strong>{{ ((1 - r.reductionRatio) * 100) | number:'1.0-0' }}%</strong> reduction.
                </p>
                @if (r.freeStates > 0) {
                  <p class="ratio-detail">
                    Ratio: synced/free = {{ r.syncedStates }}/{{ r.freeStates }}
                    = {{ r.reductionRatio | number:'1.3-3' }}
                  </p>
                }
              </div>

              <!-- Compatibility -->
              @if (r.compatibility.length > 0) {
                <h3>Pairwise Compatibility</h3>
                <div class="compat-grid">
                  @for (c of r.compatibility; track c.first + c.second) {
                    <div class="compat-item" [class.pass]="c.compatible" [class.fail]="!c.compatible">
                      <mat-icon>{{ c.compatible ? 'check_circle' : 'cancel' }}</mat-icon>
                      <span>{{ c.first }} / {{ c.second }}</span>
                    </div>
                  }
                </div>
              }

              <!-- Shared labels -->
              @if (r.sharedLabels.length > 0) {
                <h3>Shared Labels</h3>
                <div class="shared-labels">
                  @for (sl of r.sharedLabels; track sl.first + sl.second) {
                    <div class="shared-item">
                      <strong>{{ sl.first }} / {{ sl.second }}:</strong>
                      @if (sl.labels.length > 0) {
                        <span class="label-list">{{ sl.labels.join(', ') }}</span>
                      } @else {
                        <span class="no-labels">none (independent)</span>
                      }
                    </div>
                  }
                </div>
              }
            </div>
          </mat-tab>

          <!-- Participant Lattices -->
          <mat-tab label="Participants">
            <div class="tab-content">
              <mat-accordion>
                @for (p of r.participants; track p.name) {
                  <mat-expansion-panel>
                    <mat-expansion-panel-header>
                      <mat-panel-title>
                        {{ p.name }}
                        @if (p.isLattice) {
                          <mat-icon class="lattice-icon pass-icon">check_circle</mat-icon>
                        }
                      </mat-panel-title>
                      <mat-panel-description>
                        {{ p.states }} states, {{ p.transitions }} transitions
                      </mat-panel-description>
                    </mat-expansion-panel-header>
                    <pre class="pretty-type">{{ p.pretty }}</pre>
                    @if (p.svgHtml) {
                      <app-hasse-diagram [svgHtml]="p.svgHtml"></app-hasse-diagram>
                    }
                  </mat-expansion-panel>
                }
              </mat-accordion>
            </div>
          </mat-tab>

          <!-- Composition diagrams -->
          <mat-tab label="Diagrams">
            <div class="tab-content">
              <h3>Synchronized Product</h3>
              @if (r.syncedSvgHtml) {
                <app-hasse-diagram [svgHtml]="r.syncedSvgHtml"></app-hasse-diagram>
              }

              <h3>Free Product</h3>
              @if (r.freeSvgHtml) {
                <app-hasse-diagram [svgHtml]="r.freeSvgHtml"></app-hasse-diagram>
              }
            </div>
          </mat-tab>

          <!-- Global comparison -->
          @if (r.globalComparison; as gc) {
            <mat-tab label="Global Comparison">
              <div class="tab-content">
                <h3>Top-Down vs Bottom-Up</h3>
                <div class="verdict-grid">
                  <div class="verdict-item" [class.pass]="gc.embeddingExists" [class.fail]="!gc.embeddingExists">
                    <mat-icon>{{ gc.embeddingExists ? 'check_circle' : 'cancel' }}</mat-icon>
                    <span>Embedding</span>
                  </div>
                  <div class="verdict-item" [class.pass]="gc.globalIsLattice" [class.fail]="!gc.globalIsLattice">
                    <mat-icon>{{ gc.globalIsLattice ? 'check_circle' : 'cancel' }}</mat-icon>
                    <span>Global lattice</span>
                  </div>
                  <div class="verdict-item info">
                    <mat-icon>aspect_ratio</mat-icon>
                    <span>{{ gc.overApproximationRatio | number:'1.2-2' }}x over-approx</span>
                  </div>
                </div>

                <div class="metrics-grid">
                  <div class="metric">
                    <span class="metric-value">{{ gc.globalStates }}</span>
                    <span class="metric-label">Global states</span>
                  </div>
                  <div class="metric">
                    <span class="metric-value">{{ r.syncedStates }}</span>
                    <span class="metric-label">Synced states</span>
                  </div>
                  <div class="metric">
                    <span class="metric-value">{{ r.freeStates }}</span>
                    <span class="metric-label">Free states</span>
                  </div>
                </div>

                @if (gc.roleTypeMatches) {
                  <h3>Role Type Matches</h3>
                  <div class="compat-grid">
                    @for (role of objectKeys(gc.roleTypeMatches); track role) {
                      <div class="compat-item"
                           [class.pass]="gc.roleTypeMatches[role]"
                           [class.fail]="!gc.roleTypeMatches[role]">
                        <mat-icon>{{ gc.roleTypeMatches[role] ? 'check_circle' : 'cancel' }}</mat-icon>
                        <span>{{ role }}</span>
                      </div>
                    }
                  </div>
                }
              </div>
            </mat-tab>
          }

        </mat-tab-group>
      }
    </div>
  `,
  styles: [`
    .page-container { max-width: 900px; margin: 0 auto; padding: 2rem; }
    h1 { margin-bottom: 0.25rem; }
    .subtitle { color: #64748b; margin-bottom: 1.5rem; }

    .input-section { margin-bottom: 1rem; }
    .input-section label { display: block; font-weight: 600; margin-bottom: 0.5rem; }

    .participant-row {
      display: flex; gap: 0.5rem; align-items: center; margin-bottom: 0.5rem;
    }
    .name-input {
      width: 150px; font-family: 'JetBrains Mono', monospace; font-size: 0.9rem;
      padding: 0.5rem 0.75rem; border: 1px solid #cbd5e1; border-radius: 8px;
    }
    .type-input {
      flex: 1; font-family: 'JetBrains Mono', monospace; font-size: 0.9rem;
      padding: 0.5rem 0.75rem; border: 1px solid #cbd5e1; border-radius: 8px;
    }
    .full-width { width: 100%; box-sizing: border-box; }
    .add-btn { margin-top: 0.25rem; }

    .input-actions { display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem; }
    .hint { color: #94a3b8; font-size: 0.8rem; }

    .examples { display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: center; margin-bottom: 1.5rem; }
    .examples-label { font-weight: 600; color: #475569; }
    .example-btn { font-size: 0.8rem; }

    .loading { display: flex; align-items: center; gap: 1rem; padding: 2rem 0; }

    .error-card { background: #fef2f2; border: 1px solid #fecaca; margin: 1rem 0; }
    .error-card mat-card-content { display: flex; align-items: center; gap: 0.5rem; color: #dc2626; }

    .results-tabs { margin-top: 1.5rem; }
    .tab-content { padding: 1.5rem 0; }

    .verdict-grid {
      display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1.5rem;
    }
    .verdict-item {
      display: flex; align-items: center; gap: 0.5rem; padding: 0.75rem;
      border-radius: 8px; background: #f8fafc; border: 1px solid #e2e8f0;
    }
    .verdict-item.pass { background: #f0fdf4; border-color: #bbf7d0; color: #16a34a; }
    .verdict-item.fail { background: #fef2f2; border-color: #fecaca; color: #dc2626; }
    .verdict-item.info { background: #eff6ff; border-color: #bfdbfe; color: #2563eb; }

    .metrics-grid {
      display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1.5rem;
    }
    .metrics-grid.three-col { grid-template-columns: repeat(3, 1fr); }
    .metric {
      text-align: center; padding: 1rem; background: #f8fafc;
      border-radius: 8px; border: 1px solid #e2e8f0;
    }
    .metric.highlight { background: #eff6ff; border-color: #bfdbfe; }
    .metric-value { display: block; font-size: 1.5rem; font-weight: 700; color: #1e293b; }
    .metric-label { display: block; font-size: 0.8rem; color: #64748b; }
    .metric-detail { display: block; font-size: 0.75rem; color: #94a3b8; }

    .resource-box {
      background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px;
      padding: 1rem 1.25rem; margin-bottom: 1.5rem;
    }
    .resource-box p { margin: 0.25rem 0; }
    .ratio-detail { color: #64748b; font-size: 0.85rem; }

    .compat-grid {
      display: flex; flex-wrap: wrap; gap: 0.75rem; margin-bottom: 1.5rem;
    }
    .compat-item {
      display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 0.75rem;
      border-radius: 8px; background: #f8fafc; border: 1px solid #e2e8f0;
    }
    .compat-item.pass { background: #f0fdf4; border-color: #bbf7d0; color: #16a34a; }
    .compat-item.fail { background: #fef2f2; border-color: #fecaca; color: #dc2626; }

    .shared-labels { margin-bottom: 1.5rem; }
    .shared-item {
      padding: 0.5rem 0; border-bottom: 1px solid #f1f5f9;
      display: flex; gap: 0.5rem; align-items: baseline;
    }
    .label-list { font-family: 'JetBrains Mono', monospace; font-size: 0.85rem; color: #475569; }
    .no-labels { color: #94a3b8; font-style: italic; }

    .pretty-type {
      font-family: 'JetBrains Mono', monospace; font-size: 0.85rem;
      background: #f8fafc; padding: 1rem; border-radius: 8px;
      border: 1px solid #e2e8f0; overflow-x: auto; white-space: pre-wrap;
    }

    .lattice-icon { margin-left: 0.5rem; font-size: 18px; height: 18px; width: 18px; }
    .pass-icon { color: #16a34a; }

    h3 { margin-top: 1.5rem; margin-bottom: 0.75rem; }

    @media (max-width: 640px) {
      .verdict-grid { grid-template-columns: repeat(2, 1fr); }
      .metrics-grid, .metrics-grid.three-col { grid-template-columns: repeat(2, 1fr); }
      .participant-row { flex-wrap: wrap; }
      .name-input { width: 100%; }
    }
  `],
})
export class CompositionComponent {
  rows: ParticipantRow[] = [
    { name: 'Client', typeString: '' },
    { name: 'Server', typeString: '' },
  ];
  globalType = '';
  loading = signal(false);
  error = signal<string | null>(null);
  result = signal<CompositionResponse | null>(null);

  examples = [
    {
      name: 'Request-Response',
      participants: [
        { name: 'Client', typeString: '&{request: end}' },
        { name: 'Server', typeString: '&{request: +{OK: end, ERR: end}}' },
      ],
      globalType: 'Client -> Server : {request: Server -> Client : {OK: end, ERR: end}}',
    },
    {
      name: 'Two-Buyer',
      participants: [
        { name: 'Buyer1', typeString: '&{lookup: +{price: &{share: end}}}' },
        { name: 'Buyer2', typeString: '+{price: &{share: +{accept: end, reject: end}}}' },
        { name: 'Seller', typeString: '+{lookup: &{price: +{accept: &{deliver: end}, reject: end}}}' },
      ],
      globalType: '',
    },
    {
      name: 'Two-Phase Commit',
      participants: [
        { name: 'Coord', typeString: '&{prepare: +{yes: &{commit: end}, no: &{abort: end}}}' },
        { name: 'Participant', typeString: '+{prepare: &{yes: +{commit: end}, no: +{abort: end}}}' },
      ],
      globalType: '',
    },
    {
      name: 'Streaming',
      participants: [
        { name: 'Producer', typeString: 'rec X . &{data: X, done: end}' },
        { name: 'Consumer', typeString: 'rec X . +{data: X, done: end}' },
      ],
      globalType: 'rec X . Producer -> Consumer : {data: X, done: end}',
    },
    {
      name: 'Delegation',
      participants: [
        { name: 'Client', typeString: '&{task: +{response: end}}' },
        { name: 'Master', typeString: '+{task: &{delegate: +{result: &{response: end}}}}' },
        { name: 'Worker', typeString: '+{delegate: &{result: end}}' },
      ],
      globalType: '',
    },
  ];

  objectKeys = Object.keys;

  constructor(private api: ApiService) {}

  addRow() {
    this.rows.push({ name: '', typeString: '' });
  }

  removeRow(index: number) {
    this.rows.splice(index, 1);
  }

  loadExample(ex: typeof this.examples[0]) {
    this.rows = ex.participants.map(p => ({ ...p }));
    this.globalType = ex.globalType || '';
    this.compose();
  }

  compose() {
    const participants: ParticipantEntry[] = this.rows
      .filter(r => r.name.trim() && r.typeString.trim())
      .map(r => ({ name: r.name.trim(), typeString: r.typeString.trim() }));

    if (participants.length < 2) {
      this.error.set('At least 2 participants are required');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.api.compose({
      participants,
      globalType: this.globalType.trim() || undefined,
    }).subscribe({
      next: (response) => {
        this.result.set(response);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error || 'Composition failed');
        this.loading.set(false);
      },
    });
  }
}
