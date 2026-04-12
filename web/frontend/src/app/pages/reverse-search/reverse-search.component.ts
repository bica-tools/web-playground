import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';

interface SearchResult {
  name: string;
  description: string;
  typeString: string;
  numStates: string;
  numTransitions: string;
  isLattice: string;
}

@Component({
  selector: 'app-reverse-search',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <section class="rs-header">
      <h1>Reverse Search</h1>
      <p>Find a benchmark protocol by name, description, method, tag, or type fragment</p>
    </section>

    <div class="rs-input-row">
      <input class="rs-input" type="text" placeholder="e.g. read, SMTP, biology, rec X..."
             [ngModel]="query()" (ngModelChange)="onQuery($event)"
             aria-label="Search query" />
    </div>

    @if (results().length > 0) {
      <div class="rs-count">{{ results().length }} match{{ results().length === 1 ? '' : 'es' }}</div>
      <div class="rs-grid">
        @for (r of results(); track r.name) {
          <div class="rs-card">
            <div class="rs-name">{{ r.name }}</div>
            <div class="rs-desc">{{ r.description }}</div>
            <div class="rs-meta">
              <span>{{ r.numStates }}s {{ r.numTransitions }}t</span>
              <span class="rs-lattice" [class.yes]="r.isLattice === 'true'">
                {{ r.isLattice === 'true' ? 'Lattice' : 'Not lattice' }}
              </span>
            </div>
            <code class="rs-type">{{ r.typeString }}</code>
            <a class="rs-link" [routerLink]="['/tools/analyzer']" [queryParams]="{type: r.typeString}">
              Open in Analyzer &rarr;
            </a>
          </div>
        }
      </div>
    }

    @if (query() && results().length === 0 && !loading()) {
      <div class="rs-empty">No matching protocols found. Try different keywords.</div>
    }
  `,
  styles: [`
    :host { display: block; }
    .rs-header {
      width: 100vw; margin-left: calc(-50vw + 50%); margin-top: -24px;
      background: linear-gradient(145deg, #1e1b4b, #312e81 60%, #4338ca);
      color: #fff; padding: 36px 24px 32px; text-align: center;
      h1 { font-size: 28px; font-weight: 700; margin: 0 0 6px; }
      p { font-size: 13px; color: rgba(255,255,255,0.85); margin: 0; }
    }
    .rs-input-row {
      max-width: 600px; margin: 24px auto 0; padding: 0 16px;
    }
    .rs-input {
      width: 100%; box-sizing: border-box; padding: 12px 16px;
      border: 1px solid rgba(0,0,0,0.12); border-radius: 10px;
      font-size: 15px; font-family: inherit;
      &:focus { outline: none; border-color: #4338ca; box-shadow: 0 0 0 3px rgba(99,102,241,0.1); }
    }
    .rs-count {
      max-width: 1200px; margin: 12px auto 0; padding: 0 16px;
      font-size: 12px; color: rgba(0,0,0,0.45);
    }
    .rs-grid {
      max-width: 1200px; margin: 12px auto; padding: 0 16px 48px;
      display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 12px;
    }
    .rs-card {
      border: 1px solid rgba(0,0,0,0.06); border-radius: 10px;
      background: #fff; padding: 14px; transition: box-shadow 0.15s;
      &:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
    }
    .rs-name { font-size: 14px; font-weight: 600; color: rgba(0,0,0,0.85); }
    .rs-desc { font-size: 12px; color: rgba(0,0,0,0.55); margin: 4px 0 8px; line-height: 1.5; }
    .rs-meta {
      display: flex; gap: 12px; font-size: 11px; color: rgba(0,0,0,0.45);
      font-family: 'JetBrains Mono', monospace; margin-bottom: 8px;
    }
    .rs-lattice { padding: 1px 6px; border-radius: 6px; background: #fef2f2; color: #b91c1c; }
    .rs-lattice.yes { background: #ecfdf5; color: #065f46; }
    .rs-type {
      display: block; font-size: 11px; color: rgba(0,0,0,0.5);
      font-family: 'JetBrains Mono', monospace; background: #f8f9fb;
      border-radius: 6px; padding: 6px 8px; margin-bottom: 8px;
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .rs-link {
      font-size: 12px; color: #4338ca; text-decoration: none;
      &:hover { text-decoration: underline; }
    }
    .rs-empty {
      text-align: center; padding: 48px 16px;
      color: rgba(0,0,0,0.4); font-size: 14px;
    }
  `],
})
export class ReverseSearchComponent {
  readonly query = signal('');
  readonly results = signal<SearchResult[]>([]);
  readonly loading = signal(false);

  private querySubject = new Subject<string>();

  constructor(private http: HttpClient) {
    this.querySubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((q) => {
        if (!q.trim()) {
          this.results.set([]);
          return of(null);
        }
        this.loading.set(true);
        return this.http.get<{ results: SearchResult[]; total: number }>('/api/reverse-search', {
          params: { q },
        });
      }),
    ).subscribe({
      next: (res) => {
        if (res) this.results.set(res.results);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onQuery(value: string): void {
    this.query.set(value);
    this.querySubject.next(value);
  }
}
