import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { StepPaperSummary, PhaseStats } from '../../models/api.models';

const STATUS_COLORS: Record<string, string> = {
  Draft: '#f59e0b',
  Complete: '#3b82f6',
  Proved: '#059669',
  Revised: '#7c3aed',
  Superseded: '#6b7280',
  Retracted: '#ef4444',
};

const GRADE_COLORS: Record<string, string> = {
  'A+': '#059669',
  'A': '#10b981',
  'B+': '#3b82f6',
  'B': '#6366f1',
  'C': '#f59e0b',
  'D': '#ef4444',
};

const PROOF_LABELS: Record<string, string> = {
  Mechanised: 'Mechanised',
  Partial: 'Partial',
  Empirical: 'Empirical',
  None: 'None',
};

const REACTION_ICONS: Record<string, string> = {
  like: '+',
  love: '*',
  insightful: '!',
  refute: '?',
  alternative: '~',
  revision: '#',
};

const PAGE_SIZE = 50;

@Component({
  selector: 'app-paper-list',
  standalone: true,
  imports: [],
  template: `
    <header class="page-header">
      <h1>Research Papers</h1>
      <p>383 step papers — from first principles to advanced applications.</p>
    </header>

    <!-- Programme stats hero -->
    @if (phaseStats().length > 0) {
      <div class="stats-hero">
        <div class="stat-card">
          <span class="stat-number">{{ totalPapers() }}</span>
          <span class="stat-label">Papers</span>
        </div>
        <div class="stat-card">
          <span class="stat-number">{{ totalWords() }}</span>
          <span class="stat-label">Words</span>
        </div>
        <div class="stat-card">
          <span class="stat-number">{{ totalProved() }}</span>
          <span class="stat-label">Proved</span>
        </div>
        <div class="stat-card">
          <span class="stat-number">{{ phaseStats().length }}</span>
          <span class="stat-label">Phases</span>
        </div>
      </div>
    }

    <!-- Phase cards -->
    @if (phaseStats().length > 0) {
      <section class="phase-section">
        <h2>Phases</h2>
        <div class="phase-grid">
          @for (phase of phaseStats(); track phase.phase) {
            <div class="phase-card" (click)="filterByPhase(phase.phase)" [class.active]="activePhase() === phase.phase">
              <div class="phase-name">{{ phase.phase }}</div>
              <div class="phase-counts">{{ phase.completedPapers }} / {{ phase.totalPapers }} complete</div>
              <div class="phase-bar-outer">
                <div class="phase-bar-inner" [style.width.%]="phasePercent(phase)"></div>
              </div>
              <div class="phase-proof">{{ phase.provedPapers }} proved</div>
            </div>
          }
        </div>
      </section>
    }

    <!-- Filter bar -->
    <section class="filter-section">
      <div class="filter-row">
        <div class="filter-group">
          <span class="filter-label">Status:</span>
          @for (s of statusOptions; track s) {
            <button class="chip" [class.active]="activeStatus() === s" (click)="toggleStatus(s)">{{ s }}</button>
          }
        </div>
        <div class="filter-group">
          <span class="filter-label">Proof:</span>
          @for (p of proofOptions; track p) {
            <button class="chip" [class.active]="activeProof() === p" (click)="toggleProof(p)">{{ p }}</button>
          }
        </div>
        <div class="filter-group">
          <span class="filter-label">Grade:</span>
          @for (g of gradeOptions; track g) {
            <button class="chip" [class.active]="activeGrade() === g" (click)="toggleGrade(g)">{{ g }}</button>
          }
        </div>
      </div>
      <div class="search-row">
        <input type="text"
               class="search-input"
               placeholder="Search papers by title, step number, or tag..."
               [value]="searchQuery()"
               (input)="onSearch($event)">
        @if (hasActiveFilters()) {
          <button class="clear-btn" (click)="clearFilters()">Clear all</button>
        }
      </div>
    </section>

    <!-- Results count -->
    <div class="results-info">
      <span>{{ filteredPapers().length }} papers</span>
      @if (activePhase()) {
        <span class="active-filter">Phase: {{ activePhase() }}</span>
      }
    </div>

    <!-- Paper list -->
    @if (loading()) {
      <div class="loading">Loading papers...</div>
    } @else if (filteredPapers().length === 0) {
      <div class="empty-state">
        <p>No papers match your filters.</p>
      </div>
    } @else {
      <div class="paper-list">
        <div class="paper-header-row">
          <span class="col-step">Step</span>
          <span class="col-title">Title</span>
          <span class="col-status">Status</span>
          <span class="col-grade">Grade</span>
          <span class="col-proof">Proof</span>
          <span class="col-words">Words</span>
          <span class="col-reactions">Reactions</span>
        </div>
        @for (paper of visiblePapers(); track paper.id) {
          <div class="paper-row" (click)="openPaper(paper.slug)" tabindex="0" (keydown.enter)="openPaper(paper.slug)">
            <span class="col-step step-num">{{ paper.stepNumber }}</span>
            <span class="col-title paper-title">{{ paper.title }}</span>
            <span class="col-status">
              <span class="status-pill" [style.background]="statusColor(paper.status)">{{ paper.status }}</span>
            </span>
            <span class="col-grade">
              <span class="grade-badge" [style.color]="gradeColor(paper.grade)">{{ paper.grade }}</span>
            </span>
            <span class="col-proof">
              @if (paper.proofBacking === 'Mechanised') {
                <span class="proof-icon proof-mech" title="Mechanised proof">S</span>
              } @else if (paper.proofBacking === 'Partial') {
                <span class="proof-icon proof-partial" title="Partial proof">~</span>
              }
            </span>
            <span class="col-words">{{ formatWords(paper.wordCount) }}</span>
            <span class="col-reactions">
              @if (totalReactions(paper) > 0) {
                <span class="reaction-count">{{ totalReactions(paper) }}</span>
              }
            </span>
          </div>
        }
      </div>

      @if (visiblePapers().length < filteredPapers().length) {
        <div class="load-more">
          <button class="load-more-btn" (click)="loadMore()">
            Load more ({{ filteredPapers().length - visiblePapers().length }} remaining)
          </button>
        </div>
      }
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

    /* Stats hero */
    .stats-hero {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      padding: 16px 0 24px;
    }
    .stat-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 16px;
      background: white;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 12px;
    }
    .stat-number {
      font-size: 28px;
      font-weight: 700;
      color: var(--brand-primary, #4338ca);
    }
    .stat-label {
      font-size: 13px;
      color: rgba(0, 0, 0, 0.5);
      margin-top: 4px;
    }

    /* Phase section */
    .phase-section {
      padding-bottom: 24px;
    }
    .phase-section h2 {
      font-size: 18px;
      font-weight: 600;
      margin: 0 0 12px;
      color: rgba(0, 0, 0, 0.8);
    }
    .phase-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 12px;
    }
    .phase-card {
      padding: 14px;
      background: white;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.15s;
    }
    .phase-card:hover {
      border-color: var(--brand-primary, #4338ca);
      box-shadow: 0 2px 8px rgba(67, 56, 202, 0.1);
    }
    .phase-card.active {
      border-color: var(--brand-primary, #4338ca);
      background: rgba(67, 56, 202, 0.04);
    }
    .phase-name {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 6px;
      color: rgba(0, 0, 0, 0.8);
    }
    .phase-counts {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.5);
      margin-bottom: 8px;
    }
    .phase-bar-outer {
      height: 4px;
      background: rgba(0, 0, 0, 0.06);
      border-radius: 2px;
      overflow: hidden;
    }
    .phase-bar-inner {
      height: 100%;
      background: var(--brand-primary, #4338ca);
      border-radius: 2px;
      transition: width 0.3s;
    }
    .phase-proof {
      font-size: 11px;
      color: rgba(0, 0, 0, 0.4);
      margin-top: 6px;
    }

    /* Filter section */
    .filter-section {
      padding: 16px 0;
      border-top: 1px solid rgba(0, 0, 0, 0.06);
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    }
    .filter-row {
      display: flex;
      gap: 24px;
      flex-wrap: wrap;
      margin-bottom: 12px;
    }
    .filter-group {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
    }
    .filter-label {
      font-size: 12px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.5);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .chip {
      padding: 4px 12px;
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-radius: 16px;
      background: white;
      font-size: 12px;
      cursor: pointer;
      transition: all 0.15s;
      color: rgba(0, 0, 0, 0.65);
    }
    .chip:hover {
      border-color: var(--brand-primary, #4338ca);
      color: var(--brand-primary, #4338ca);
    }
    .chip.active {
      background: var(--brand-primary, #4338ca);
      color: white;
      border-color: var(--brand-primary, #4338ca);
    }

    .search-row {
      display: flex;
      gap: 12px;
      align-items: center;
    }
    .search-input {
      flex: 1;
      padding: 8px 14px;
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-radius: 8px;
      font-size: 14px;
      outline: none;
      transition: border-color 0.15s;
    }
    .search-input:focus {
      border-color: var(--brand-primary, #4338ca);
    }
    .clear-btn {
      padding: 6px 14px;
      border: none;
      background: rgba(0, 0, 0, 0.06);
      border-radius: 8px;
      font-size: 13px;
      cursor: pointer;
      color: rgba(0, 0, 0, 0.6);
    }
    .clear-btn:hover {
      background: rgba(0, 0, 0, 0.1);
    }

    /* Results info */
    .results-info {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 0 8px;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.5);
    }
    .active-filter {
      padding: 2px 8px;
      background: rgba(67, 56, 202, 0.08);
      color: var(--brand-primary, #4338ca);
      border-radius: 4px;
      font-size: 12px;
    }

    /* Paper list */
    .paper-list {
      padding-bottom: 24px;
    }
    .paper-header-row {
      display: grid;
      grid-template-columns: 70px 1fr 90px 50px 50px 70px 70px;
      gap: 8px;
      padding: 8px 12px;
      font-size: 11px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.4);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.08);
    }
    .paper-row {
      display: grid;
      grid-template-columns: 70px 1fr 90px 50px 50px 70px 70px;
      gap: 8px;
      padding: 10px 12px;
      align-items: center;
      border-bottom: 1px solid rgba(0, 0, 0, 0.04);
      cursor: pointer;
      transition: background 0.1s;
    }
    .paper-row:hover {
      background: rgba(67, 56, 202, 0.03);
    }
    .paper-row:focus-visible {
      outline: 2px solid var(--brand-primary, #4338ca);
      outline-offset: -2px;
      border-radius: 4px;
    }
    .step-num {
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
      font-size: 13px;
      color: var(--brand-primary, #4338ca);
      font-weight: 600;
    }
    .paper-title {
      font-size: 14px;
      color: rgba(0, 0, 0, 0.8);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .status-pill {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 11px;
      font-weight: 600;
      color: white;
    }
    .grade-badge {
      font-size: 13px;
      font-weight: 700;
    }
    .proof-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 22px;
      height: 22px;
      border-radius: 50%;
      font-size: 11px;
      font-weight: 700;
    }
    .proof-mech {
      background: rgba(5, 150, 105, 0.1);
      color: #059669;
    }
    .proof-partial {
      background: rgba(245, 158, 11, 0.1);
      color: #f59e0b;
    }
    .col-words {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.5);
      text-align: right;
    }
    .col-reactions {
      text-align: right;
    }
    .reaction-count {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.4);
    }

    /* Load more */
    .load-more {
      text-align: center;
      padding: 24px 0 40px;
    }
    .load-more-btn {
      padding: 10px 28px;
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-radius: 8px;
      background: white;
      font-size: 14px;
      cursor: pointer;
      color: var(--brand-primary, #4338ca);
      transition: all 0.15s;
    }
    .load-more-btn:hover {
      border-color: var(--brand-primary, #4338ca);
      background: rgba(67, 56, 202, 0.04);
    }

    .loading, .empty-state {
      padding: 48px 16px;
      text-align: center;
      color: rgba(0, 0, 0, 0.5);
      font-size: 16px;
    }

    @media (max-width: 768px) {
      .stats-hero {
        grid-template-columns: repeat(2, 1fr);
      }
      .filter-row {
        flex-direction: column;
        gap: 12px;
      }
      .paper-header-row {
        display: none;
      }
      .paper-row {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        padding: 12px;
      }
      .paper-row .col-title {
        width: 100%;
        order: -1;
        white-space: normal;
      }
      .paper-row .col-step {
        order: -2;
      }
    }

    @media (max-width: 640px) {
      .page-header h1 { font-size: 22px; }
      .stat-number { font-size: 22px; }
      .phase-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  `],
})
export class PaperListComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  allPapers = signal<StepPaperSummary[]>([]);
  phaseStats = signal<PhaseStats[]>([]);
  loading = signal(false);

  activePhase = signal<string | null>(null);
  activeStatus = signal<string | null>(null);
  activeProof = signal<string | null>(null);
  activeGrade = signal<string | null>(null);
  searchQuery = signal('');
  visibleCount = signal(PAGE_SIZE);

  statusOptions = ['Draft', 'Complete', 'Proved', 'Revised', 'Superseded', 'Retracted'];
  proofOptions = ['Mechanised', 'Partial', 'Empirical', 'None'];
  gradeOptions = ['A+', 'A', 'B+', 'B', 'C', 'D'];

  totalPapers = computed(() => this.allPapers().length);
  totalWords = computed(() => {
    const sum = this.allPapers().reduce((acc, p) => acc + (p.wordCount || 0), 0);
    if (sum >= 1_000_000) return (sum / 1_000_000).toFixed(1) + 'M';
    if (sum >= 1000) return (sum / 1000).toFixed(0) + 'k';
    return sum.toString();
  });
  totalProved = computed(() => this.allPapers().filter(p => p.proofBacking === 'Mechanised').length);

  filteredPapers = computed(() => {
    let papers = this.allPapers();
    const phase = this.activePhase();
    const status = this.activeStatus();
    const proof = this.activeProof();
    const grade = this.activeGrade();
    const query = this.searchQuery().toLowerCase().trim();

    if (phase) papers = papers.filter(p => p.phase === phase);
    if (status) papers = papers.filter(p => p.status === status);
    if (proof) papers = papers.filter(p => p.proofBacking === proof);
    if (grade) papers = papers.filter(p => p.grade === grade);
    if (query) {
      papers = papers.filter(p =>
        p.title.toLowerCase().includes(query) ||
        p.stepNumber.toLowerCase().includes(query) ||
        (p.tags && p.tags.toLowerCase().includes(query))
      );
    }
    return papers;
  });

  visiblePapers = computed(() => this.filteredPapers().slice(0, this.visibleCount()));

  hasActiveFilters = computed(() =>
    !!(this.activePhase() || this.activeStatus() || this.activeProof() || this.activeGrade() || this.searchQuery())
  );

  ngOnInit(): void {
    // Read query params
    this.route.queryParamMap.subscribe(params => {
      this.activePhase.set(params.get('phase'));
      this.activeStatus.set(params.get('status'));
      this.activeProof.set(params.get('proof'));
      this.activeGrade.set(params.get('grade'));
      this.searchQuery.set(params.get('q') || '');
    });

    this.loadData();
  }

  filterByPhase(phase: string): void {
    this.activePhase.set(this.activePhase() === phase ? null : phase);
    this.visibleCount.set(PAGE_SIZE);
    this.syncQueryParams();
  }

  toggleStatus(status: string): void {
    this.activeStatus.set(this.activeStatus() === status ? null : status);
    this.visibleCount.set(PAGE_SIZE);
    this.syncQueryParams();
  }

  toggleProof(proof: string): void {
    this.activeProof.set(this.activeProof() === proof ? null : proof);
    this.visibleCount.set(PAGE_SIZE);
    this.syncQueryParams();
  }

  toggleGrade(grade: string): void {
    this.activeGrade.set(this.activeGrade() === grade ? null : grade);
    this.visibleCount.set(PAGE_SIZE);
    this.syncQueryParams();
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
    this.visibleCount.set(PAGE_SIZE);
    this.syncQueryParams();
  }

  clearFilters(): void {
    this.activePhase.set(null);
    this.activeStatus.set(null);
    this.activeProof.set(null);
    this.activeGrade.set(null);
    this.searchQuery.set('');
    this.visibleCount.set(PAGE_SIZE);
    this.syncQueryParams();
  }

  loadMore(): void {
    this.visibleCount.update(c => c + PAGE_SIZE);
  }

  openPaper(slug: string): void {
    this.router.navigate(['/papers', slug]);
  }

  statusColor(status: string): string {
    return STATUS_COLORS[status] || '#6b7280';
  }

  gradeColor(grade: string): string {
    return GRADE_COLORS[grade] || '#6b7280';
  }

  formatWords(count: number): string {
    if (!count) return '-';
    if (count >= 1000) return (count / 1000).toFixed(1) + 'k';
    return count.toString();
  }

  totalReactions(paper: StepPaperSummary): number {
    if (!paper.reactionCounts) return 0;
    return Object.values(paper.reactionCounts).reduce((a, b) => a + b, 0);
  }

  phasePercent(phase: PhaseStats): number {
    if (!phase.totalPapers) return 0;
    return Math.round((phase.completedPapers / phase.totalPapers) * 100);
  }

  private syncQueryParams(): void {
    const params: Record<string, string> = {};
    if (this.activePhase()) params['phase'] = this.activePhase()!;
    if (this.activeStatus()) params['status'] = this.activeStatus()!;
    if (this.activeProof()) params['proof'] = this.activeProof()!;
    if (this.activeGrade()) params['grade'] = this.activeGrade()!;
    if (this.searchQuery()) params['q'] = this.searchQuery();
    this.router.navigate([], { queryParams: params, replaceUrl: true });
  }

  private loadData(): void {
    this.loading.set(true);
    this.api.getPapers().subscribe({
      next: (papers) => {
        this.allPapers.set(papers);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.api.getPhaseStats().subscribe({
      next: (stats) => this.phaseStats.set(stats),
      error: () => {},
    });
  }
}
