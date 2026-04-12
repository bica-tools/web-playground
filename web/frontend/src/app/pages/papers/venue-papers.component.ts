import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { VenuePaper } from '../../models/api.models';

const STATUS_COLORS: Record<string, string> = {
  Submitted: '#f59e0b',
  'Under Review': '#3b82f6',
  Accepted: '#059669',
  Published: '#059669',
  Rejected: '#ef4444',
  Withdrawn: '#6b7280',
};

@Component({
  selector: 'app-venue-papers',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="page-header">
      <h1>Publications</h1>
      <p>Venue papers submitted to conferences and journals.</p>
    </header>

    @if (loading()) {
      <div class="loading">Loading publications...</div>
    } @else if (papers().length === 0) {
      <div class="empty-state">
        <p>No venue papers published yet.</p>
      </div>
    } @else {
      <div class="venue-list">
        @for (paper of papers(); track paper.id) {
          <article class="venue-card" (click)="toggleDetail(paper.id)" tabindex="0" (keydown.enter)="toggleDetail(paper.id)">
            <div class="venue-row">
              <div class="venue-info">
                <span class="venue-badge">{{ paper.venue }}</span>
                <span class="venue-status" [style.background]="statusColor(paper.status)">{{ paper.status }}</span>
              </div>
              <h2 class="venue-title">{{ paper.title }}</h2>
              <div class="venue-dates">
                @if (paper.submissionDate) {
                  <span>Submitted: {{ formatDate(paper.submissionDate) }}</span>
                }
                @if (paper.decisionDate) {
                  <span>Decision: {{ formatDate(paper.decisionDate) }}</span>
                }
              </div>
            </div>

            @if (expandedId() === paper.id) {
              <div class="venue-detail" (click)="$event.stopPropagation()">
                @if (paper.abstract) {
                  <div class="detail-section">
                    <h3>Abstract</h3>
                    <p class="abstract-text">{{ paper.abstract }}</p>
                  </div>
                }

                @if (paper.stepsCovered) {
                  <div class="detail-section">
                    <h3>Steps covered</h3>
                    <div class="steps-links">
                      @for (step of parseList(paper.stepsCovered); track step) {
                        <a [routerLink]="['/papers', step]" class="step-link">{{ step }}</a>
                      }
                    </div>
                  </div>
                }

                <div class="detail-links">
                  @if (paper.doi) {
                    <a [href]="'https://doi.org/' + paper.doi" target="_blank" rel="noopener" class="ext-link">DOI</a>
                  }
                  @if (paper.arxivId) {
                    <a [href]="'https://arxiv.org/abs/' + paper.arxivId" target="_blank" rel="noopener" class="ext-link">arXiv</a>
                  }
                  @if (paper.pdfPath) {
                    <a [href]="paper.pdfPath" target="_blank" rel="noopener" class="ext-link">PDF</a>
                  }
                </div>
              </div>
            }
          </article>
        }
      </div>
    }
  `,
  styles: [`
    .page-header {
      padding: 24px 0 16px;
    }
    .page-header h1 {
      font-size: 28px;
      font-weight: 600;
      margin: 0 0 8px;
    }
    .page-header p {
      color: rgba(0, 0, 0, 0.6);
      margin: 0;
      font-size: 16px;
    }

    .venue-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding-bottom: 40px;
    }

    .venue-card {
      padding: 20px;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
      background: white;
    }
    .venue-card:hover {
      border-color: var(--brand-primary, #4338ca);
      box-shadow: 0 4px 12px rgba(67, 56, 202, 0.1);
    }
    .venue-card:focus-visible {
      outline: 2px solid var(--brand-primary, #4338ca);
      outline-offset: 2px;
    }

    .venue-row {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .venue-info {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .venue-badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 12px;
      background: rgba(67, 56, 202, 0.08);
      color: var(--brand-primary, #4338ca);
      font-size: 12px;
      font-weight: 600;
    }
    .venue-status {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 11px;
      font-weight: 600;
      color: white;
    }

    .venue-title {
      font-size: 18px;
      font-weight: 500;
      margin: 0;
      color: rgba(0, 0, 0, 0.87);
    }

    .venue-dates {
      display: flex;
      gap: 16px;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.45);
    }

    /* Detail section */
    .venue-detail {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid rgba(0, 0, 0, 0.06);
    }
    .detail-section {
      margin-bottom: 16px;
    }
    .detail-section h3 {
      font-size: 14px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.5);
      margin: 0 0 8px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .abstract-text {
      font-size: 14px;
      line-height: 1.7;
      color: rgba(0, 0, 0, 0.65);
      margin: 0;
    }

    .steps-links {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }
    .step-link {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      background: rgba(67, 56, 202, 0.06);
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
      font-size: 13px;
    }
    .step-link:hover {
      background: rgba(67, 56, 202, 0.12);
    }

    .detail-links {
      display: flex;
      gap: 10px;
    }
    .ext-link {
      display: inline-block;
      padding: 6px 16px;
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-radius: 8px;
      text-decoration: none;
      color: var(--brand-primary, #4338ca);
      font-size: 13px;
      font-weight: 600;
      transition: all 0.15s;
    }
    .ext-link:hover {
      border-color: var(--brand-primary, #4338ca);
      background: rgba(67, 56, 202, 0.04);
    }

    .loading, .empty-state {
      padding: 48px 16px;
      text-align: center;
      color: rgba(0, 0, 0, 0.5);
      font-size: 16px;
    }

    @media (max-width: 640px) {
      .page-header h1 { font-size: 22px; }
      .venue-title { font-size: 16px; }
      .venue-dates { flex-direction: column; gap: 4px; }
    }
  `],
})
export class VenuePapersComponent implements OnInit {
  private api = inject(ApiService);

  papers = signal<VenuePaper[]>([]);
  loading = signal(false);
  expandedId = signal<number | null>(null);

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getVenuePapers().subscribe({
      next: (papers) => {
        this.papers.set(papers);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  toggleDetail(id: number): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  statusColor(status: string): string {
    return STATUS_COLORS[status] || '#6b7280';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  parseList(csv: string): string[] {
    if (!csv) return [];
    return csv.split(',').map(s => s.trim()).filter(s => s.length > 0);
  }
}
