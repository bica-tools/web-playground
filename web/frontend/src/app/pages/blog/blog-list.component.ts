import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { BlogPostSummary } from '../../models/api.models';

const ARC_NAMES: Record<number, string> = {
  1: 'Foundations',
  2: 'The State Space',
  3: 'Properties',
  4: 'Tools',
  5: 'The Algebra',
  6: 'Applications',
};

@Component({
  selector: 'app-blog-list',
  standalone: true,
  imports: [],
  template: `
    <header class="page-header">
      <h1>Blog</h1>
      <p>Session types explained — from first principles to real-world applications.</p>
    </header>

    <div class="arc-filter">
      <button
        [class.active]="selectedArc() === null"
        (click)="filterArc(null)">All</button>
      @for (arc of arcs(); track arc) {
        <button
          [class.active]="selectedArc() === arc"
          (click)="filterArc(arc)">{{ arcName(arc) }}</button>
      }
    </div>

    @if (loading()) {
      <div class="loading">Loading posts...</div>
    } @else if (posts().length === 0) {
      <div class="empty-state">
        <p>No posts published yet. Check back soon.</p>
      </div>
    } @else {
      <div class="posts-list">
        @for (post of posts(); track post.id) {
          <article class="post-card" (click)="openPost(post.slug)" tabindex="0" (keydown.enter)="openPost(post.slug)">
            <div class="post-meta">
              <span class="arc-badge">{{ arcName(post.arc) }}</span>
              <span class="post-date">{{ formatDate(post.publishedAt) }}</span>
            </div>
            <h2 class="post-title">{{ post.title }}</h2>
            <p class="post-summary">{{ post.summary }}</p>
            <div class="post-tags">
              @for (tag of parseTags(post.tags); track tag) {
                <span class="tag">{{ tag }}</span>
              }
            </div>
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

    .arc-filter {
      display: flex;
      gap: 8px;
      padding: 8px 0 24px;
      flex-wrap: wrap;
    }
    .arc-filter button {
      padding: 6px 16px;
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-radius: 20px;
      background: white;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.15s;
      color: rgba(0, 0, 0, 0.7);
    }
    .arc-filter button:hover {
      border-color: var(--brand-primary, #4338ca);
      color: var(--brand-primary, #4338ca);
    }
    .arc-filter button.active {
      background: var(--brand-primary, #4338ca);
      color: white;
      border-color: var(--brand-primary, #4338ca);
    }

    .posts-list {
      display: flex;
      flex-direction: column;
      gap: 20px;
      padding-bottom: 40px;
    }

    .post-card {
      padding: 24px;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
      background: white;
    }
    .post-card:hover {
      border-color: var(--brand-primary, #4338ca);
      box-shadow: 0 4px 12px rgba(67, 56, 202, 0.1);
      transform: translateY(-1px);
    }
    .post-card:focus-visible {
      outline: 2px solid var(--brand-primary, #4338ca);
      outline-offset: 2px;
    }

    .post-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
    }
    .arc-badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 12px;
      background: rgba(67, 56, 202, 0.08);
      color: var(--brand-primary, #4338ca);
      font-size: 12px;
      font-weight: 600;
    }
    .post-date {
      font-size: 13px;
      color: rgba(0, 0, 0, 0.45);
    }

    .post-title {
      font-size: 20px;
      font-weight: 500;
      margin: 0 0 8px;
      color: rgba(0, 0, 0, 0.87);
    }

    .post-summary {
      font-size: 15px;
      color: rgba(0, 0, 0, 0.6);
      margin: 0 0 12px;
      line-height: 1.6;
    }

    .post-tags {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }
    .tag {
      padding: 2px 8px;
      border-radius: 4px;
      background: rgba(0, 0, 0, 0.04);
      color: rgba(0, 0, 0, 0.5);
      font-size: 12px;
    }

    .loading, .empty-state {
      padding: 48px 16px;
      text-align: center;
      color: rgba(0, 0, 0, 0.5);
      font-size: 16px;
    }

    @media (max-width: 640px) {
      .page-header h1 { font-size: 22px; }
      .post-title { font-size: 17px; }
    }
  `],
})
export class BlogListComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);

  posts = signal<BlogPostSummary[]>([]);
  arcs = signal<number[]>([]);
  selectedArc = signal<number | null>(null);
  loading = signal(false);

  ngOnInit(): void {
    this.loadPosts();
  }

  filterArc(arc: number | null): void {
    this.selectedArc.set(arc);
    this.loadPosts();
  }

  openPost(slug: string): void {
    this.router.navigate(['/blog', slug]);
  }

  arcName(arc: number): string {
    return ARC_NAMES[arc] || `Arc ${arc}`;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  parseTags(tags: string): string[] {
    if (!tags) return [];
    return tags.split(',').map(t => t.trim()).filter(t => t.length > 0);
  }

  private loadPosts(): void {
    this.loading.set(true);
    const arc = this.selectedArc() ?? undefined;
    this.api.getBlogPosts(arc).subscribe({
      next: (posts) => {
        this.posts.set(posts);
        if (this.arcs().length === 0) {
          const unique = [...new Set(posts.map(p => p.arc))].sort();
          this.arcs.set(unique);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
