import { Component, OnInit } from '@angular/core';
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
        {{ benchmarks.length }} real-world and classic protocols expressed as session types,
        verified through the full reticulate pipeline.
      </p>
    </header>

    @if (loading) {
      <div class="loading">
        <mat-spinner diameter="32"></mat-spinner>
        <span>Loading benchmarks&hellip;</span>
      </div>
    } @else {
      <!-- Summary stats -->
      <p class="stats-line">
        {{ benchmarks.length }} protocols &middot;
        {{ parallelCount }} use &parallel; &middot;
        {{ latticeCount }} are lattices &middot;
        {{ totalStates }} total states &middot;
        {{ totalTests }} tests generated
      </p>

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
              <th>&parallel;</th>
              <th>Lattice</th>
              <th>Tests</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (b of benchmarks; track b.name; let i = $index) {
              <tr>
                <td>{{ i + 1 }}</td>
                <td class="protocol-name">{{ b.name }}</td>
                <td>{{ b.numStates }}</td>
                <td>{{ b.numTransitions }}</td>
                <td>{{ b.numSccs }}</td>
                <td>@if (b.usesParallel) { &#x2713; }</td>
                <td>@if (b.isLattice) { &#x2713; }</td>
                <td>{{ b.numTests }}</td>
                <td><a [routerLink]="['/tools/analyzer']" [queryParams]="{type: b.typeString}">analyze</a></td>
              </tr>
              <tr class="detail-row">
                <td colspan="9">
                  <details>
                    <summary>Session type &amp; diagram</summary>
                    <div class="detail-content">
                      <app-code-block [code]="b.pretty" label="Session type"></app-code-block>
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

    .loading {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      padding: 48px 0;
    }

    .stats-line {
      text-align: center;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.6);
      padding: 16px 0;
    }

    .table-container {
      overflow-x: auto;
    }

    .benchmark-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
    }
    .benchmark-table th {
      text-align: left;
      padding: 12px 16px;
      font-weight: 500;
      border-bottom: 2px solid rgba(0, 0, 0, 0.12);
      white-space: nowrap;
    }
    .benchmark-table td {
      padding: 8px 16px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    }
    .benchmark-table tbody tr:hover td {
      background: rgba(0, 0, 0, 0.02);
    }
    .protocol-name {
      font-weight: 500;
    }
    .detail-row td {
      padding: 0 16px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    }
    .detail-row details {
      margin: 4px 0;
    }
    .detail-row summary {
      cursor: pointer;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.5);
      padding: 4px 0;
    }
    .detail-content {
      padding: 8px 0 16px;
    }
    .benchmark-table a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
    }
    .benchmark-table a:hover {
      text-decoration: underline;
    }
  `],
})
export class BenchmarksComponent implements OnInit {
  benchmarks: BenchmarkDto[] = [];
  loading = true;
  parallelCount = 0;
  latticeCount = 0;
  totalStates = 0;
  totalTests = 0;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getBenchmarks().subscribe({
      next: (data) => {
        this.benchmarks = data;
        this.parallelCount = data.filter((b) => b.usesParallel).length;
        this.latticeCount = data.filter((b) => b.isLattice).length;
        this.totalStates = data.reduce((s, b) => s + b.numStates, 0);
        this.totalTests = data.reduce((s, b) => s + b.numTests, 0);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }
}
