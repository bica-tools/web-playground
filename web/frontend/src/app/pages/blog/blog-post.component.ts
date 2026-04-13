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
      <div class="post-layout">
        <!-- Sticky sidebar TOC -->
        @if (headings().length > 0) {
          <aside class="post-sidebar">
            <nav class="toc-nav">
              <a class="back-link" routerLink="/blog">&larr; All Posts</a>
              <h4>{{ post()!.title }}</h4>
              <ul>
                @for (h of headings(); track h.id; let i = $index) {
                  <li [class.active]="activeHeading() === h.id">
                    <a (click)="scrollToHeading(h.id)">{{ h.text }}</a>
                  </li>
                }
              </ul>
            </nav>
          </aside>
        }

        <!-- Main content -->
        <article class="blog-post">
          <a routerLink="/blog" class="back-link mobile-back">&larr; All Posts</a>

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
      </div>
    } @else {
      <div class="empty-state">
        <p>Post not found.</p>
        <a routerLink="/blog">&larr; Back to Blog</a>
      </div>
    }
  `,
  styles: [`
    .post-layout {
      display: flex;
      gap: 40px;
      align-items: flex-start;
      max-width: 1060px;
      margin: 0 auto;
      padding-top: 16px;
    }

    .post-sidebar {
      position: sticky;
      top: 80px;
      width: 240px;
      flex-shrink: 0;
      max-height: calc(100vh - 100px);
      overflow-y: auto;
      display: none;
    }
    @media (min-width: 1100px) {
      .post-sidebar { display: block; }
      .mobile-back { display: none !important; }
    }

    .toc-nav h4 {
      font-size: 14px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.8);
      margin: 0 0 12px;
      padding: 0 12px;
      line-height: 1.4;
    }
    .toc-nav ul {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .toc-nav li a {
      display: block;
      padding: 5px 12px;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.5);
      text-decoration: none;
      border-left: 2px solid transparent;
      cursor: pointer;
      transition: all 0.15s;
      line-height: 1.4;
    }
    .toc-nav li a:hover {
      color: var(--brand-primary, #4338ca);
      background: rgba(67, 56, 202, 0.03);
    }
    .toc-nav li.active a {
      color: var(--brand-primary, #4338ca);
      border-left-color: var(--brand-primary, #4338ca);
      font-weight: 500;
      background: rgba(67, 56, 202, 0.05);
    }

    .blog-post {
      flex: 1;
      min-width: 0;
      max-width: 720px;
      padding: 0 0 48px;
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
      margin: 56px 0 16px;
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
      background: #f0edf8;
      color: #5b3dbb;
      padding: 2px 7px;
      border-radius: 4px;
      font-size: 0.88em;
      font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', 'SF Mono', monospace;
      font-weight: 500;
      letter-spacing: -0.02em;
    }
    /* Default code block — dark editor */
    .post-body :deep(pre) {
      background: #1e1e2e;
      border: none;
      border-radius: 10px;
      padding: 20px 24px;
      overflow-x: auto;
      margin: 24px 0;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
    }
    .post-body :deep(pre code) {
      background: none;
      color: #cdd6f4;
      padding: 0;
      font-size: 14px;
      font-weight: 400;
      line-height: 1.7;
      letter-spacing: 0;
    }

    /* Session type expressions — the hero artifact */
    .post-body :deep(pre.lang-session-type) {
      background: linear-gradient(135deg, #f5f0ff 0%, #ede5ff 100%);
      border-left: 4px solid var(--brand-primary, #4338ca);
      box-shadow: none;
      padding: 20px 24px;
    }
    .post-body :deep(pre.lang-session-type code) {
      color: #3b1f8e;
      font-size: 15px;
      font-weight: 600;
      letter-spacing: 0.01em;
    }

    /* AST tree diagrams — structural, lighter */
    .post-body :deep(pre.lang-tree) {
      background: #f8f9fa;
      border: 1px solid rgba(0, 0, 0, 0.06);
      box-shadow: none;
      padding: 20px 24px;
    }
    .post-body :deep(pre.lang-tree code) {
      color: #2d3748;
      font-size: 13px;
      font-weight: 400;
      line-height: 1.9;
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
    .post-body :deep(hr) {
      border: none;
      height: 1px;
      background: linear-gradient(90deg, transparent, rgba(67, 56, 202, 0.2), transparent);
      margin: 32px 0;
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
  private scrollListener: (() => void) | null = null;

  post = signal<BlogPost | null>(null);
  loading = signal(false);
  renderedContent = signal('');
  headings = signal<{ id: string; text: string }[]>([]);
  activeHeading = signal('');

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
    if (this.scrollListener) {
      window.removeEventListener('scroll', this.scrollListener);
    }
  }

  scrollToHeading(id: string): void {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      this.activeHeading.set(id);
    }
  }

  private extractHeadings(html: string): void {
    const matches = [...html.matchAll(/<h2[^>]*id="([^"]*)"[^>]*>(.*?)<\/h2>/g)];
    this.headings.set(matches.map(m => ({ id: m[1], text: m[2].replace(/<[^>]+>/g, '') })));

    // Track scroll position to highlight active heading
    if (this.scrollListener) window.removeEventListener('scroll', this.scrollListener);
    this.scrollListener = () => {
      const ids = this.headings().map(h => h.id);
      let active = '';
      for (const id of ids) {
        const el = document.getElementById(id);
        if (el && el.getBoundingClientRect().top <= 120) active = id;
      }
      if (active) this.activeHeading.set(active);
    };
    window.addEventListener('scroll', this.scrollListener, { passive: true });
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
        const html = this.renderMarkdown(post.content);
        this.renderedContent.set(html);
        this.extractHeadings(html);
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
      // Horizontal rules
      .replace(/^---$/gm, '<hr>')
      // Code blocks (fenced, with language class)
      .replace(/```([\w-]*)\n([\s\S]*?)```/g, (_, lang, code) => {
        const cls = lang ? ` class="lang-${lang}"` : '';
        return `<pre${cls}><code>${code}</code></pre>`;
      })
      // Inline code
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      // Headings
      .replace(/^### (.+)$/gm, '<h3>$1</h3>')
      .replace(/^## (.+)$/gm, (_, title) => {
        const id = title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
        return `<h2 id="${id}">${title}</h2>`;
      })
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
