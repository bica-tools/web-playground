import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { BlogPost } from '../../models/api.models';

const ARC_NAMES: Record<number, string> = {
  1: 'Foundations',
  2: 'The State Space',
  3: 'Properties',
  4: 'Tools',
  5: 'The Algebra',
  6: 'Applications',
};

@Component({
  selector: 'app-blog-post',
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (loading()) {
      <div class="loading">Loading...</div>
    } @else if (post()) {
      <article class="blog-post">
        <a routerLink="/blog" class="back-link">&larr; All Posts</a>

        <div class="post-meta">
          <span class="arc-badge">{{ arcName(post()!.arc) }}</span>
          <span class="post-date">{{ formatDate(post()!.publishedAt) }}</span>
          <span class="post-author">by {{ post()!.author }}</span>
        </div>

        <h1>{{ post()!.title }}</h1>

        <div class="post-body" [innerHTML]="renderedContent()"></div>

        <div class="post-footer">
          <div class="post-tags">
            @for (tag of parseTags(post()!.tags); track tag) {
              <span class="tag">{{ tag }}</span>
            }
          </div>
          <a routerLink="/blog" class="back-link">&larr; Back to Blog</a>
        </div>
      </article>
    } @else {
      <div class="empty-state">
        <p>Post not found.</p>
        <a routerLink="/blog">&larr; Back to Blog</a>
      </div>
    }
  `,
  styles: [`
    .blog-post {
      max-width: 720px;
      margin: 0 auto;
      padding: 24px 0 48px;
    }

    .back-link {
      display: inline-block;
      font-size: 14px;
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
      margin-bottom: 20px;
    }
    .back-link:hover {
      text-decoration: underline;
    }

    .post-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
      flex-wrap: wrap;
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
    .post-date, .post-author {
      font-size: 13px;
      color: rgba(0, 0, 0, 0.45);
    }

    h1 {
      font-size: 28px;
      font-weight: 600;
      margin: 0 0 24px;
      line-height: 1.3;
      color: rgba(0, 0, 0, 0.87);
    }

    .post-body {
      font-size: 16px;
      line-height: 1.8;
      color: rgba(0, 0, 0, 0.75);
    }
    .post-body :deep(h2) {
      font-size: 22px;
      font-weight: 500;
      margin: 32px 0 12px;
      color: rgba(0, 0, 0, 0.87);
    }
    .post-body :deep(h3) {
      font-size: 18px;
      font-weight: 500;
      margin: 24px 0 8px;
      color: rgba(0, 0, 0, 0.87);
    }
    .post-body :deep(p) {
      margin: 0 0 16px;
    }
    .post-body :deep(code) {
      background: rgba(0, 0, 0, 0.05);
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 14px;
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
    }
    .post-body :deep(pre) {
      background: rgba(0, 0, 0, 0.03);
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      padding: 16px;
      overflow-x: auto;
      margin: 16px 0;
    }
    .post-body :deep(pre code) {
      background: none;
      padding: 0;
    }
    .post-body :deep(blockquote) {
      border-left: 3px solid var(--brand-primary, #4338ca);
      margin: 16px 0;
      padding: 8px 16px;
      color: rgba(0, 0, 0, 0.6);
      background: rgba(67, 56, 202, 0.03);
      border-radius: 0 8px 8px 0;
    }
    .post-body :deep(ul), .post-body :deep(ol) {
      margin: 8px 0 16px;
      padding-left: 24px;
    }
    .post-body :deep(li) {
      margin: 4px 0;
    }
    .post-body :deep(a) {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
    }
    .post-body :deep(a:hover) {
      text-decoration: underline;
    }

    .post-footer {
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid rgba(0, 0, 0, 0.08);
    }
    .post-tags {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      margin-bottom: 16px;
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
    .empty-state a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
    }

    @media (max-width: 640px) {
      h1 { font-size: 22px; }
      .post-body { font-size: 15px; }
    }
  `],
})
export class BlogPostComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private sub: Subscription | null = null;

  post = signal<BlogPost | null>(null);
  loading = signal(false);
  renderedContent = signal('');

  ngOnInit(): void {
    this.sub = this.route.paramMap.subscribe((params) => {
      const slug = params.get('slug');
      if (slug) {
        this.loadPost(slug);
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
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

  private loadPost(slug: string): void {
    this.loading.set(true);
    this.post.set(null);
    this.api.getBlogPost(slug).subscribe({
      next: (post) => {
        this.post.set(post);
        this.renderedContent.set(this.renderMarkdown(post.content));
        this.loading.set(false);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  /**
   * Minimal markdown-to-HTML renderer for blog content.
   * Handles: headings, bold, italic, code, links, lists, blockquotes, paragraphs.
   * Will be replaced by a proper library (marked.js) in Phase 3.
   */
  private renderMarkdown(md: string): string {
    if (!md) return '';
    let html = md
      // Code blocks (fenced)
      .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
      // Inline code
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      // Headings
      .replace(/^### (.+)$/gm, '<h3>$1</h3>')
      .replace(/^## (.+)$/gm, '<h2>$1</h2>')
      // Bold and italic
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      // Links
      .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>')
      // Blockquotes
      .replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')
      // Unordered lists
      .replace(/^- (.+)$/gm, '<li>$1</li>')
      // Wrap consecutive <li> in <ul>
      .replace(/((?:<li>.*<\/li>\n?)+)/g, '<ul>$1</ul>');

    // Paragraphs: wrap remaining loose text
    html = html.split('\n\n').map(block => {
      block = block.trim();
      if (!block) return '';
      if (block.startsWith('<')) return block;
      return `<p>${block.replace(/\n/g, '<br>')}</p>`;
    }).join('\n');

    return html;
  }
}
