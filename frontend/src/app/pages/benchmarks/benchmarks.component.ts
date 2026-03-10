import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../services/api.service';
import { BenchmarkDto } from '../../models/api.models';
import { HasseDiagramComponent } from '../../components/hasse-diagram/hasse-diagram.component';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';

@Component({
  selector: 'app-benchmarks',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    HasseDiagramComponent,
    CodeBlockComponent,
  ],
  template: `
    <header class="page-header">
      <h1>Benchmarks</h1>
      <p>
        {{ benchmarks().length }} real-world and classic protocols expressed as session types,
        verified through the full analysis pipeline.
      </p>
    </header>

    @if (loading()) {
      <div class="loading">
        <mat-spinner diameter="32"></mat-spinner>
        <span>Loading benchmarks&hellip;</span>
      </div>
    } @else {
      <!-- Summary stats -->
      <div class="stats-row">
        <div class="stat-chip">{{ benchmarks().length }} protocols</div>
        <div class="stat-chip">{{ parallelCount() }} use &#x2225;</div>
        <div class="stat-chip">{{ recursiveCount() }} recursive</div>
        <div class="stat-chip">{{ latticeCount() }} lattices</div>
        <div class="stat-chip">{{ totalStates() }} total states</div>
        <div class="stat-chip">{{ totalTests() }} tests generated</div>
      </div>

      <!-- Benchmark table -->
      <div class="table-container">
        <table class="benchmark-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Protocol</th>
              <th>States</th>
              <th>Trans.</th>
              <th>SCCs</th>
              <th>Methods</th>
              <th>&#x2225;</th>
              <th>rec</th>
              <th>Lattice</th>
              <th>Tests</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (b of benchmarks(); track b.name; let i = $index) {
              <tr>
                <td class="num-col">{{ i + 1 }}</td>
                <td class="protocol-name">{{ b.name }}</td>
                <td>{{ b.numStates }}</td>
                <td>{{ b.numTransitions }}</td>
                <td>{{ b.numSccs }}</td>
                <td>{{ b.numMethods }}</td>
                <td>@if (b.usesParallel) { &#x2713; }</td>
                <td>@if (b.isRecursive) { &#x2713; }</td>
                <td>@if (b.isLattice) { &#x2713; }</td>
                <td>{{ b.numTests }}</td>
                <td class="action-links">
                  <a [routerLink]="['/tools/analyzer']" [queryParams]="{type: b.typeString}">analyze</a>
                  <a [routerLink]="['/tools/test-generator']" [queryParams]="{type: b.typeString, class: toClassName(b.name)}">tests</a>
                </td>
              </tr>
              <tr class="detail-row">
                <td colspan="11">
                  <details>
                    <summary>{{ b.description }}</summary>
                    <div class="detail-content">
                      <app-code-block [code]="b.pretty" label="Session type"></app-code-block>
                      <div class="detail-meta">
                        <span class="meta-item">
                          <strong>Rec depth:</strong> {{ b.recDepth }}
                        </span>
                        <span class="meta-item">
                          <strong>Valid paths:</strong> {{ b.numValidPaths }}
                        </span>
                        <span class="meta-item">
                          <strong>Violations:</strong> {{ b.numViolations }}
                        </span>
                        <span class="meta-item">
                          <strong>Incomplete:</strong> {{ b.numIncomplete }}
                        </span>
                      </div>
                      @if (b.methods && b.methods.length > 0) {
                        <div class="detail-methods">
                          <strong>Methods:</strong>
                          @for (m of b.methods; track m) {
                            <span class="method-chip">{{ m }}</span>
                          }
                        </div>
                      }
                      @if (b.svgHtml) {
                        <app-hasse-diagram [svgHtml]="b.svgHtml"></app-hasse-diagram>
                      }
                    </div>
                  </details>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: [`
    .page-header { padding: 24px 0 16px; }
    .page-header h1 { font-size: 24px; font-weight: 600; margin: 0 0 8px; }
    .page-header p { color: rgba(0, 0, 0, 0.6); margin: 0; }

    .loading {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      padding: 48px 0;
    }

    .stats-row {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      justify-content: center;
      padding: 16px 0 24px;
    }
    .stat-chip {
      display: inline-block;
      padding: 6px 14px;
      background: #f1f5f9;
      border: 1px solid rgba(0,0,0,0.06);
      border-radius: 20px;
      font-size: 13px;
      color: rgba(0,0,0,0.7);
    }

    .table-container { overflow-x: auto; }

    .benchmark-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
    }
    .benchmark-table th {
      text-align: left;
      padding: 10px 12px;
      font-weight: 600;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.3px;
      border-bottom: 2px solid rgba(0, 0, 0, 0.12);
      white-space: nowrap;
      color: rgba(0,0,0,0.55);
    }
    .benchmark-table td {
      padding: 8px 12px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.04);
    }
    .benchmark-table tbody tr:hover td {
      background: rgba(0, 0, 0, 0.02);
    }
    .num-col {
      color: rgba(0,0,0,0.35);
      font-size: 12px;
    }
    .protocol-name { font-weight: 500; }

    .detail-row td {
      padding: 0 12px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    }
    .detail-row details { margin: 4px 0; }
    .detail-row summary {
      cursor: pointer;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.45);
      padding: 4px 0;
    }
    .detail-content { padding: 8px 0 16px; }

    .detail-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      margin: 10px 0;
      font-size: 13px;
      color: rgba(0,0,0,0.6);
    }

    .detail-methods {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      align-items: center;
      margin: 8px 0 12px;
      font-size: 13px;
    }
    .method-chip {
      display: inline-block;
      padding: 2px 10px;
      background: #f1f5f9;
      border: 1px solid rgba(0,0,0,0.06);
      border-radius: 12px;
      font-size: 12px;
      font-family: 'JetBrains Mono', monospace;
      color: rgba(0,0,0,0.65);
    }

    .action-links {
      display: flex;
      gap: 12px;
      white-space: nowrap;
    }
    .benchmark-table a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
      font-size: 13px;
    }
    .benchmark-table a:hover { text-decoration: underline; }
  `],
})
export class BenchmarksComponent implements OnInit {
  readonly benchmarks = signal<BenchmarkDto[]>([]);
  readonly loading = signal(true);
  readonly parallelCount = signal(0);
  readonly recursiveCount = signal(0);
  readonly latticeCount = signal(0);
  readonly totalStates = signal(0);
  readonly totalTests = signal(0);

  constructor(private api: ApiService) {}

  toClassName(name: string): string {
    return name.replace(/[^a-zA-Z0-9]/g, '');
  }

  ngOnInit(): void {
    this.api.getBenchmarks().subscribe({
      next: (data) => {
        this.benchmarks.set(data);
        this.parallelCount.set(data.filter((b) => b.usesParallel).length);
        this.recursiveCount.set(data.filter((b) => b.isRecursive).length);
        this.latticeCount.set(data.filter((b) => b.isLattice).length);
        this.totalStates.set(data.reduce((s, b) => s + b.numStates, 0));
        this.totalTests.set(data.reduce((s, b) => s + b.numTests, 0));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }
}
