import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { ActivatedRoute } from '@angular/router';
import { HasseDiagramComponent } from '../../components/hasse-diagram/hasse-diagram.component';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';
import { ApiService } from '../../services/api.service';
import { GlobalAnalyzeResponse, ProjectionDto } from '../../models/api.models';

@Component({
  selector: 'app-global-analyzer',
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
    MatExpansionModule,
    HasseDiagramComponent,
    CodeBlockComponent,
  ],
  template: `
    <div class="page-container">
      <h1>Global Type Analyzer</h1>
      <p class="subtitle">
        Analyze multiparty global session types: parse, build state space,
        check lattice properties, and project onto individual roles.
      </p>

      <!-- Input -->
      <div class="input-section">
        <label for="global-type-input">Global type</label>
        <textarea
          id="global-type-input"
          [(ngModel)]="typeString"
          placeholder="e.g. Buyer1 -> Seller : {lookup: Seller -> Buyer1 : {price: end}}"
          rows="4"
          (keydown.control.enter)="analyze()"
          (keydown.meta.enter)="analyze()"
        ></textarea>
        <div class="input-actions">
          <button mat-raised-button color="primary" (click)="analyze()" [disabled]="loading()">
            <mat-icon>play_arrow</mat-icon>
            Analyze
          </button>
          <span class="hint">Ctrl+Enter</span>
        </div>
      </div>

      <!-- Quick examples -->
      <div class="examples">
        <span class="examples-label">Examples:</span>
        @for (ex of examples; track ex.name) {
          <button mat-stroked-button (click)="loadExample(ex.type)" class="example-btn">
            {{ ex.name }}
          </button>
        }
      </div>

      <!-- Grammar reference -->
      @if (!result()) {
        <mat-card class="grammar-card">
          <mat-card-header>
            <mat-card-title>Grammar</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <pre class="grammar">G  ::=  sender -> receiver : {{ '{' }} m₁ : G₁ , … , mₙ : Gₙ {{ '}' }}
     |  G₁ || G₂
     |  rec X . G
     |  X
     |  end</pre>
          </mat-card-content>
        </mat-card>
      }

      <!-- Loading -->
      @if (loading()) {
        <div class="loading">
          <mat-spinner diameter="40"></mat-spinner>
          <span>Analyzing global type...</span>
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
                <div class="verdict-item" [class.pass]="r.isLattice" [class.fail]="!r.isLattice">
                  <mat-icon>{{ r.isLattice ? 'check_circle' : 'cancel' }}</mat-icon>
                  <span>Lattice</span>
                </div>
                <div class="verdict-item pass">
                  <mat-icon>check_circle</mat-icon>
                  <span>{{ r.numRoles }} roles</span>
                </div>
                <div class="verdict-item" [class.pass]="!r.usesParallel" [class.info]="r.usesParallel">
                  <mat-icon>{{ r.usesParallel ? 'call_split' : 'linear_scale' }}</mat-icon>
                  <span>{{ r.usesParallel ? 'Parallel' : 'Sequential' }}</span>
                </div>
                <div class="verdict-item" [class.pass]="!r.isRecursive" [class.info]="r.isRecursive">
                  <mat-icon>{{ r.isRecursive ? 'loop' : 'trending_flat' }}</mat-icon>
                  <span>{{ r.isRecursive ? 'Recursive' : 'Finite' }}</span>
                </div>
              </div>

              <div class="metrics-grid">
                <div class="metric"><span class="metric-value">{{ r.numStates }}</span><span class="metric-label">States</span></div>
                <div class="metric"><span class="metric-value">{{ r.numTransitions }}</span><span class="metric-label">Transitions</span></div>
                <div class="metric"><span class="metric-value">{{ r.numSccs }}</span><span class="metric-label">SCCs</span></div>
                <div class="metric"><span class="metric-value">{{ r.numRoles }}</span><span class="metric-label">Roles</span></div>
              </div>

              @if (r.counterexample) {
                <mat-card class="error-card">
                  <mat-card-content>
                    <mat-icon>warning</mat-icon>
                    <span>{{ r.counterexample }}</span>
                  </mat-card-content>
                </mat-card>
              }

              <h3>Roles</h3>
              <mat-chip-set>
                @for (role of r.roles; track role) {
                  <mat-chip>{{ role }}</mat-chip>
                }
              </mat-chip-set>

              <h3>Pretty-printed</h3>
              <pre class="pretty-type">{{ r.pretty }}</pre>
            </div>
          </mat-tab>

          <!-- Global Hasse Diagram -->
          <mat-tab label="Hasse Diagram">
            <div class="tab-content">
              <h3>Global State Space</h3>
              <app-hasse-diagram [svgHtml]="r.svgHtml"></app-hasse-diagram>
            </div>
          </mat-tab>

          <!-- Projections -->
          <mat-tab label="Projections">
            <div class="tab-content">
              <p>Each role's local view of the protocol, obtained by projection.</p>
              <mat-accordion>
                @for (role of r.roles; track role) {
                  @if (r.projections[role]; as proj) {
                    <mat-expansion-panel>
                      <mat-expansion-panel-header>
                        <mat-panel-title>
                          {{ role }}
                          @if (proj.localIsLattice) {
                            <mat-icon class="lattice-icon pass-icon">check_circle</mat-icon>
                          }
                        </mat-panel-title>
                        <mat-panel-description>
                          {{ proj.localStates }} states, {{ proj.localTransitions }} transitions
                        </mat-panel-description>
                      </mat-expansion-panel-header>

                      <div class="projection-content">
                        <h4>Local type</h4>
                        <pre class="pretty-type">{{ proj.localType }}</pre>

                        <div class="proj-metrics">
                          <span><strong>States:</strong> {{ proj.localStates }}</span>
                          <span><strong>Transitions:</strong> {{ proj.localTransitions }}</span>
                          <span><strong>Lattice:</strong> {{ proj.localIsLattice ? 'Yes' : 'No' }}</span>
                        </div>

                        @if (proj.localSvgHtml) {
                          <h4>Local Hasse Diagram</h4>
                          <app-hasse-diagram [svgHtml]="proj.localSvgHtml"></app-hasse-diagram>
                        }
                      </div>
                    </mat-expansion-panel>
                  }
                }
              </mat-accordion>
            </div>
          </mat-tab>

          <!-- DOT -->
          <mat-tab label="DOT">
            <div class="tab-content">
              <app-code-block [code]="r.dotSource" label="DOT source"></app-code-block>
            </div>
          </mat-tab>

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
    .input-section textarea {
      width: 100%; font-family: 'JetBrains Mono', monospace; font-size: 0.9rem;
      padding: 0.75rem; border: 1px solid #cbd5e1; border-radius: 8px;
      resize: vertical; box-sizing: border-box;
    }
    .input-actions { display: flex; align-items: center; gap: 1rem; margin-top: 0.5rem; }
    .hint { color: #94a3b8; font-size: 0.8rem; }

    .examples { display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: center; margin-bottom: 1.5rem; }
    .examples-label { font-weight: 600; color: #475569; }
    .example-btn { font-size: 0.8rem; }

    .grammar-card { margin-bottom: 1.5rem; }
    .grammar { font-family: 'JetBrains Mono', monospace; font-size: 0.85rem; margin: 0; }

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
    .metric {
      text-align: center; padding: 1rem; background: #f8fafc;
      border-radius: 8px; border: 1px solid #e2e8f0;
    }
    .metric-value { display: block; font-size: 1.5rem; font-weight: 700; color: #1e293b; }
    .metric-label { display: block; font-size: 0.8rem; color: #64748b; }

    .pretty-type {
      font-family: 'JetBrains Mono', monospace; font-size: 0.85rem;
      background: #f8fafc; padding: 1rem; border-radius: 8px;
      border: 1px solid #e2e8f0; overflow-x: auto; white-space: pre-wrap;
    }

    .projection-content { padding: 0.5rem 0; }
    .proj-metrics { display: flex; gap: 2rem; margin: 0.75rem 0; color: #475569; }
    .lattice-icon { margin-left: 0.5rem; font-size: 18px; height: 18px; width: 18px; }
    .pass-icon { color: #16a34a; }

    h3 { margin-top: 1.5rem; margin-bottom: 0.75rem; }
    h4 { margin-top: 1rem; margin-bottom: 0.5rem; }

    @media (max-width: 640px) {
      .verdict-grid, .metrics-grid { grid-template-columns: repeat(2, 1fr); }
    }
  `],
})
export class GlobalAnalyzerComponent {
  typeString = '';
  loading = signal(false);
  error = signal<string | null>(null);
  result = signal<GlobalAnalyzeResponse | null>(null);

  examples = [
    {
      name: 'Request-Response',
      type: 'Client -> Server : {request: Server -> Client : {response: end}}',
    },
    {
      name: 'Two-Buyer',
      type: 'Buyer1 -> Seller : {lookup: Seller -> Buyer1 : {price: Seller -> Buyer2 : {price: Buyer1 -> Buyer2 : {share: Buyer2 -> Seller : {accept: Seller -> Buyer2 : {deliver: end}, reject: end}}}}}',
    },
    {
      name: 'Streaming',
      type: 'rec X . Producer -> Consumer : {data: X, done: end}',
    },
    {
      name: 'Two-Phase Commit',
      type: 'Coord -> P : {prepare: P -> Coord : {yes: Coord -> P : {commit: end}, no: Coord -> P : {abort: end}}}',
    },
    {
      name: 'Delegation',
      type: 'Client -> Master : {task: Master -> Worker : {delegate: Worker -> Master : {result: Master -> Client : {response: end}}}}',
    },
    {
      name: 'Auth-Service',
      type: 'Client -> Server : {login: Server -> Client : {granted: rec X . Client -> Server : {request: Server -> Client : {response: X}, logout: end}, denied: end}}',
    },
  ];

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
  ) {
    this.route.queryParams.subscribe((params) => {
      if (params['type']) {
        this.typeString = params['type'];
        this.analyze();
      }
    });
  }

  loadExample(type: string) {
    this.typeString = type;
    this.analyze();
  }

  analyze() {
    if (!this.typeString.trim()) return;
    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.api.analyzeGlobal(this.typeString.trim()).subscribe({
      next: (response) => {
        this.result.set(response);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error || 'Analysis failed');
        this.loading.set(false);
      },
    });
  }
}
