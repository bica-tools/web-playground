import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HasseDiagramComponent } from '../../components/hasse-diagram/hasse-diagram.component';
import { ApiService } from '../../services/api.service';
import { CompareResponse } from '../../models/api.models';

@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    HasseDiagramComponent,
  ],
  template: `
    <div class="page-container">
      <h1>Type Comparison</h1>
      <p class="subtitle">
        Compare two session types: subtyping, duality, state spaces, Chomsky classification, and recursion analysis.
      </p>

      <!-- Input -->
      <div class="input-row">
        <div class="input-section">
          <label for="type1-input">Type 1</label>
          <textarea
            id="type1-input"
            [(ngModel)]="type1"
            placeholder="e.g. &{read: end, write: end}"
            rows="3"
            (keydown.control.enter)="compare()"
            (keydown.meta.enter)="compare()"
          ></textarea>
        </div>
        <div class="input-section">
          <label for="type2-input">Type 2</label>
          <textarea
            id="type2-input"
            [(ngModel)]="type2"
            placeholder="e.g. &{read: end}"
            rows="3"
            (keydown.control.enter)="compare()"
            (keydown.meta.enter)="compare()"
          ></textarea>
        </div>
      </div>

      <div class="input-actions">
        <button mat-raised-button color="primary" (click)="compare()" [disabled]="loading()">
          <mat-icon>compare_arrows</mat-icon>
          Compare
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
          <span>Comparing types...</span>
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

          <!-- Subtyping -->
          <mat-tab label="Subtyping">
            <div class="tab-content">
              <div class="comparison-grid">
                <div class="type-col">
                  <h3>Type 1</h3>
                  <pre class="pretty-type">{{ r.pretty1 }}</pre>
                </div>
                <div class="relation-col">
                  <div class="relation-badge" [class]="subtypingClass(r)">
                    {{ r.subtypingRelation }}
                  </div>
                </div>
                <div class="type-col">
                  <h3>Type 2</h3>
                  <pre class="pretty-type">{{ r.pretty2 }}</pre>
                </div>
              </div>

              <div class="verdict-grid four">
                <div class="verdict-item" [class.pass]="r.type1SubtypeOfType2" [class.fail]="!r.type1SubtypeOfType2">
                  <mat-icon>{{ r.type1SubtypeOfType2 ? 'check_circle' : 'cancel' }}</mat-icon>
                  <span>T1 &lt;: T2</span>
                </div>
                <div class="verdict-item" [class.pass]="r.type2SubtypeOfType1" [class.fail]="!r.type2SubtypeOfType1">
                  <mat-icon>{{ r.type2SubtypeOfType1 ? 'check_circle' : 'cancel' }}</mat-icon>
                  <span>T2 &lt;: T1</span>
                </div>
                <div class="verdict-item" [class.pass]="r.type1SubtypeOfType2 && r.type2SubtypeOfType1"
                     [class.fail]="!(r.type1SubtypeOfType2 && r.type2SubtypeOfType1)">
                  <mat-icon>{{ (r.type1SubtypeOfType2 && r.type2SubtypeOfType1) ? 'check_circle' : 'cancel' }}</mat-icon>
                  <span>Equivalent</span>
                </div>
                <div class="verdict-item" [class.pass]="r.areDuals" [class.fail]="!r.areDuals">
                  <mat-icon>{{ r.areDuals ? 'check_circle' : 'cancel' }}</mat-icon>
                  <span>Duals</span>
                </div>
              </div>
            </div>
          </mat-tab>

          <!-- Duality -->
          <mat-tab label="Duality">
            <div class="tab-content">
              <div class="comparison-grid">
                <div class="type-col">
                  <h3>dual(Type 1)</h3>
                  <pre class="pretty-type">{{ r.dual1 }}</pre>
                </div>
                <div class="relation-col">
                  <div class="relation-badge" [class.pass]="r.areDuals" [class.fail]="!r.areDuals">
                    {{ r.areDuals ? 'Are duals' : 'Not duals' }}
                  </div>
                </div>
                <div class="type-col">
                  <h3>dual(Type 2)</h3>
                  <pre class="pretty-type">{{ r.dual2 }}</pre>
                </div>
              </div>

              <p class="info-text">
                Two types are duals if dual(T1) is equivalent to T2.
                In session type theory, duality swaps Branch (&) with Select (+).
              </p>
            </div>
          </mat-tab>

          <!-- State Spaces -->
          <mat-tab label="State Spaces">
            <div class="tab-content">
              <div class="metrics-compare">
                <div class="metric-pair">
                  <span class="metric-label">States</span>
                  <span class="metric-value">{{ r.states1 }}</span>
                  <span class="metric-vs">vs</span>
                  <span class="metric-value">{{ r.states2 }}</span>
                </div>
                <div class="metric-pair">
                  <span class="metric-label">Transitions</span>
                  <span class="metric-value">{{ r.transitions1 }}</span>
                  <span class="metric-vs">vs</span>
                  <span class="metric-value">{{ r.transitions2 }}</span>
                </div>
                <div class="metric-pair">
                  <span class="metric-label">Lattice</span>
                  <span class="metric-value" [class.yes]="r.isLattice1" [class.no]="!r.isLattice1">
                    {{ r.isLattice1 ? 'Yes' : 'No' }}
                  </span>
                  <span class="metric-vs">vs</span>
                  <span class="metric-value" [class.yes]="r.isLattice2" [class.no]="!r.isLattice2">
                    {{ r.isLattice2 ? 'Yes' : 'No' }}
                  </span>
                </div>
                <div class="metric-pair">
                  <span class="metric-label">Chomsky</span>
                  <span class="metric-value">{{ r.chomsky1 }}</span>
                  <span class="metric-vs">vs</span>
                  <span class="metric-value">{{ r.chomsky2 }}</span>
                </div>
              </div>

              <div class="hasse-compare">
                <div class="hasse-col">
                  <h3>Type 1</h3>
                  <app-hasse-diagram [svgHtml]="r.svgHtml1"></app-hasse-diagram>
                </div>
                <div class="hasse-col">
                  <h3>Type 2</h3>
                  <app-hasse-diagram [svgHtml]="r.svgHtml2"></app-hasse-diagram>
                </div>
              </div>
            </div>
          </mat-tab>

          <!-- Recursion -->
          <mat-tab label="Recursion">
            <div class="tab-content">
              <table class="rec-table">
                <thead>
                  <tr>
                    <th>Property</th>
                    <th>Type 1</th>
                    <th>Type 2</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>Recursive</td>
                    <td [class.yes]="r.isRecursive1" [class.no]="!r.isRecursive1">
                      {{ r.isRecursive1 ? 'Yes' : 'No' }}
                    </td>
                    <td [class.yes]="r.isRecursive2" [class.no]="!r.isRecursive2">
                      {{ r.isRecursive2 ? 'Yes' : 'No' }}
                    </td>
                  </tr>
                  <tr>
                    <td>Guarded</td>
                    <td [class.yes]="r.isGuarded1" [class.no]="!r.isGuarded1">
                      {{ r.isGuarded1 ? 'Yes' : 'No' }}
                    </td>
                    <td [class.yes]="r.isGuarded2" [class.no]="!r.isGuarded2">
                      {{ r.isGuarded2 ? 'Yes' : 'No' }}
                    </td>
                  </tr>
                  <tr>
                    <td>Contractive</td>
                    <td [class.yes]="r.isContractive1" [class.no]="!r.isContractive1">
                      {{ r.isContractive1 ? 'Yes' : 'No' }}
                    </td>
                    <td [class.yes]="r.isContractive2" [class.no]="!r.isContractive2">
                      {{ r.isContractive2 ? 'Yes' : 'No' }}
                    </td>
                  </tr>
                  <tr>
                    <td>Tail-recursive</td>
                    <td [class.yes]="r.isTailRecursive1" [class.no]="!r.isTailRecursive1">
                      {{ r.isTailRecursive1 ? 'Yes' : 'No' }}
                    </td>
                    <td [class.yes]="r.isTailRecursive2" [class.no]="!r.isTailRecursive2">
                      {{ r.isTailRecursive2 ? 'Yes' : 'No' }}
                    </td>
                  </tr>
                  <tr>
                    <td>Chomsky level</td>
                    <td>{{ r.chomsky1 }}</td>
                    <td>{{ r.chomsky2 }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </mat-tab>

        </mat-tab-group>
      }
    </div>
  `,
  styles: [`
    .page-container { max-width: 960px; margin: 0 auto; padding: 2rem; }
    h1 { margin-bottom: 0.25rem; }
    .subtitle { color: #64748b; margin-bottom: 1.5rem; }

    .input-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 0.5rem; }
    .input-section label { display: block; font-weight: 600; margin-bottom: 0.5rem; }
    .input-section textarea {
      width: 100%; font-family: 'JetBrains Mono', monospace; font-size: 0.9rem;
      padding: 0.75rem; border: 1px solid #cbd5e1; border-radius: 8px;
      resize: vertical; box-sizing: border-box;
    }
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

    .comparison-grid {
      display: grid; grid-template-columns: 1fr auto 1fr; gap: 1rem;
      align-items: start; margin-bottom: 1.5rem;
    }
    .type-col h3 { margin-top: 0; margin-bottom: 0.5rem; }
    .pretty-type {
      font-family: 'JetBrains Mono', monospace; font-size: 0.85rem;
      background: #f8fafc; padding: 1rem; border-radius: 8px;
      border: 1px solid #e2e8f0; overflow-x: auto; white-space: pre-wrap;
    }
    .relation-col {
      display: flex; align-items: center; justify-content: center;
      padding-top: 2.5rem;
    }
    .relation-badge {
      padding: 0.5rem 1rem; border-radius: 20px; font-weight: 600;
      font-size: 0.85rem; white-space: nowrap;
    }
    .relation-badge.equivalent { background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }
    .relation-badge.subtype { background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; }
    .relation-badge.unrelated { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }
    .relation-badge.pass { background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }
    .relation-badge.fail { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }

    .verdict-grid {
      display: grid; gap: 1rem; margin-bottom: 1.5rem;
    }
    .verdict-grid.four { grid-template-columns: repeat(4, 1fr); }
    .verdict-item {
      display: flex; align-items: center; gap: 0.5rem; padding: 0.75rem;
      border-radius: 8px; background: #f8fafc; border: 1px solid #e2e8f0;
    }
    .verdict-item.pass { background: #f0fdf4; border-color: #bbf7d0; color: #16a34a; }
    .verdict-item.fail { background: #fef2f2; border-color: #fecaca; color: #dc2626; }

    .info-text { color: #64748b; font-size: 0.9rem; margin-top: 1rem; }

    .metrics-compare { margin-bottom: 1.5rem; }
    .metric-pair {
      display: grid; grid-template-columns: 120px 1fr auto 1fr; gap: 0.5rem;
      align-items: center; padding: 0.75rem; border-bottom: 1px solid #e2e8f0;
    }
    .metric-label { font-weight: 600; color: #475569; }
    .metric-value { text-align: center; font-size: 1.1rem; font-weight: 600; }
    .metric-vs { color: #94a3b8; font-size: 0.8rem; text-align: center; }
    .metric-value.yes { color: #16a34a; }
    .metric-value.no { color: #dc2626; }

    .hasse-compare { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
    .hasse-col h3 { margin-top: 0; margin-bottom: 0.75rem; }

    .rec-table {
      width: 100%; border-collapse: collapse; margin-top: 0.5rem;
    }
    .rec-table th, .rec-table td {
      padding: 0.75rem 1rem; border-bottom: 1px solid #e2e8f0; text-align: left;
    }
    .rec-table th { background: #f8fafc; font-weight: 600; color: #475569; }
    .rec-table td.yes { color: #16a34a; font-weight: 600; }
    .rec-table td.no { color: #dc2626; font-weight: 600; }

    h3 { margin-top: 1.5rem; margin-bottom: 0.75rem; }

    @media (max-width: 640px) {
      .input-row { grid-template-columns: 1fr; }
      .comparison-grid { grid-template-columns: 1fr; }
      .relation-col { padding-top: 0; }
      .hasse-compare { grid-template-columns: 1fr; }
      .verdict-grid.four { grid-template-columns: repeat(2, 1fr); }
    }
  `],
})
export class CompareComponent {
  type1 = '';
  type2 = '';
  loading = signal(false);
  error = signal<string | null>(null);
  result = signal<CompareResponse | null>(null);

  examples = [
    {
      name: 'Subtype (width)',
      type1: '&{read: end, write: end}',
      type2: '&{read: end}',
    },
    {
      name: 'Duals',
      type1: '&{request: +{OK: end, ERROR: end}}',
      type2: '+{request: &{OK: end, ERROR: end}}',
    },
    {
      name: 'Equivalent',
      type1: 'rec X . &{next: X, stop: end}',
      type2: 'rec Y . &{next: Y, stop: end}',
    },
    {
      name: 'Unrelated',
      type1: '&{read: end}',
      type2: '+{write: end}',
    },
    {
      name: 'Recursive vs Finite',
      type1: 'rec X . &{send: X, done: end}',
      type2: '&{send: &{send: end, done: end}, done: end}',
    },
  ];

  constructor(private api: ApiService) {}

  loadExample(ex: { type1: string; type2: string }) {
    this.type1 = ex.type1;
    this.type2 = ex.type2;
    this.compare();
  }

  subtypingClass(r: CompareResponse): string {
    if (r.type1SubtypeOfType2 && r.type2SubtypeOfType1) return 'equivalent';
    if (r.type1SubtypeOfType2 || r.type2SubtypeOfType1) return 'subtype';
    return 'unrelated';
  }

  compare() {
    if (!this.type1.trim() || !this.type2.trim()) return;
    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.api.compareTypes(this.type1.trim(), this.type2.trim()).subscribe({
      next: (response) => {
        this.result.set(response);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error || 'Comparison failed');
        this.loading.set(false);
      },
    });
  }
}
