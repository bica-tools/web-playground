import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Subscription } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { StepPaper, PaperComment } from '../../models/api.models';

const STATUS_COLORS: Record<string, string> = {
  Draft: '#f59e0b',
  Complete: '#3b82f6',
  Proved: '#059669',
  Revised: '#7c3aed',
  Superseded: '#6b7280',
  Retracted: '#ef4444',
};

const STATUS_BANNERS: Record<string, { bg: string; text: string }> = {
  Revised: { bg: '#fffbeb', text: 'This paper has been revised. See revision notes below.' },
  Superseded: { bg: '#f3f4f6', text: 'This paper has been superseded by a newer version.' },
  Retracted: { bg: '#fef2f2', text: 'This paper has been retracted.' },
};

interface ReactionDef {
  type: string;
  label: string;
  icon: string;
}

const REACTIONS: ReactionDef[] = [
  { type: 'like', label: 'Like', icon: '+1' },
  { type: 'love', label: 'Love', icon: '<3' },
  { type: 'insightful', label: 'Insightful', icon: '(!)' },
  { type: 'refute', label: 'Refute', icon: '(?)' },
  { type: 'alternative', label: 'Alternative', icon: '(~)' },
  { type: 'revision', label: 'Needs Revision', icon: '(#)' },
];

@Component({
  selector: 'app-paper-detail',
  standalone: true,
  imports: [RouterLink, FormsModule, DecimalPipe],
  template: `
    @if (loading()) {
      <div class="loading">Loading...</div>
    } @else if (paper()) {
      <article class="paper-detail">
        <a routerLink="/papers" class="back-link">&larr; All Papers</a>

        <!-- Status banner -->
        @if (statusBanner()) {
          <div class="status-banner" [style.background]="statusBanner()!.bg">
            {{ statusBanner()!.text }}
            @if (paper()!.supersededBy) {
              <a [routerLink]="['/papers', paper()!.supersededBy]" class="superseded-link">View replacement</a>
            }
          </div>
        }

        <!-- Header -->
        <div class="paper-header">
          <div class="paper-meta">
            <span class="step-badge">{{ paper()!.stepNumber }}</span>
            <span class="phase-badge">{{ paper()!.phase }}</span>
            <span class="status-pill" [style.background]="statusColor(paper()!.status)">{{ paper()!.status }}</span>
            <span class="grade-badge" [style.color]="gradeColor(paper()!.grade)">{{ paper()!.grade }}</span>
            @if (paper()!.proofBacking === 'Mechanised') {
              <span class="proof-badge proof-mech">Mechanised</span>
            } @else if (paper()!.proofBacking === 'Partial') {
              <span class="proof-badge proof-partial">Partial proof</span>
            }
          </div>
          <h1>{{ paper()!.title }}</h1>
        </div>

        <!-- Abstract -->
        @if (paper()!.abstract) {
          <section class="abstract-section">
            <h2>Abstract</h2>
            <p class="abstract-text">{{ paper()!.abstract }}</p>
          </section>
        }

        <!-- Metadata table -->
        <section class="metadata-section">
          <h2>Details</h2>
          <table class="meta-table">
            @if (paper()!.domain) {
              <tr><td class="meta-key">Domain</td><td>{{ paper()!.domain }}</td></tr>
            }
            <tr><td class="meta-key">Word count</td><td>{{ paper()!.wordCount | number }}</td></tr>
            <tr><td class="meta-key">Version</td><td>{{ paper()!.version }}</td></tr>
            <tr><td class="meta-key">Last updated</td><td>{{ formatDate(paper()!.updatedAt) }}</td></tr>
            @if (paper()!.dependsOn) {
              <tr>
                <td class="meta-key">Dependencies</td>
                <td>
                  @for (dep of parseList(paper()!.dependsOn); track dep) {
                    <a [routerLink]="['/papers', dep]" class="dep-link">{{ dep }}</a>
                  }
                </td>
              </tr>
            }
            @if (paper()!.relatedSteps) {
              <tr>
                <td class="meta-key">Related steps</td>
                <td>
                  @for (rel of parseList(paper()!.relatedSteps); track rel) {
                    <a [routerLink]="['/papers', rel]" class="dep-link">{{ rel }}</a>
                  }
                </td>
              </tr>
            }
            @if (paper()!.reticulateModule) {
              <tr><td class="meta-key">Reticulate module</td><td><code>{{ paper()!.reticulateModule }}</code></td></tr>
            }
            @if (paper()!.bicaPackage) {
              <tr><td class="meta-key">BICA package</td><td><code>{{ paper()!.bicaPackage }}</code></td></tr>
            }
            @if (paper()!.leanFiles) {
              <tr><td class="meta-key">Lean files</td><td><code>{{ paper()!.leanFiles }}</code></td></tr>
            }
          </table>
        </section>

        <!-- Download PDF -->
        @if (paper()!.pdfPath) {
          <div class="pdf-section">
            <a [href]="paper()!.pdfPath" target="_blank" rel="noopener" class="pdf-btn">
              Download PDF
            </a>
          </div>
        }

        <!-- BibTeX -->
        @if (bibtex()) {
          <section class="bibtex-section">
            <h2>BibTeX</h2>
            <div class="bibtex-block">
              <pre><code>{{ bibtex() }}</code></pre>
              <button class="copy-btn" (click)="copyBibtex()">{{ copied() ? 'Copied' : 'Copy' }}</button>
            </div>
          </section>
        }

        <!-- Reactions -->
        <section class="reactions-section">
          <h2>Reactions</h2>
          <div class="reactions-bar">
            @for (r of reactions; track r.type) {
              <button class="reaction-btn" [class.reacted]="userReacted().has(r.type)" (click)="react(r.type)">
                <span class="reaction-icon">{{ r.icon }}</span>
                <span class="reaction-label">{{ r.label }}</span>
                @if (reactionCount(r.type) > 0) {
                  <span class="reaction-count">{{ reactionCount(r.type) }}</span>
                }
              </button>
            }
          </div>
        </section>

        <!-- Revision history -->
        @if (paper()!.version > 1 && paper()!.revisionNotes) {
          <section class="revision-section">
            <h2>Revision history</h2>
            <button class="expand-btn" (click)="revisionsExpanded.set(!revisionsExpanded())">
              {{ revisionsExpanded() ? 'Hide' : 'Show' }} revision notes
            </button>
            @if (revisionsExpanded()) {
              <div class="revision-notes">
                <p>{{ paper()!.revisionNotes }}</p>
              </div>
            }
          </section>
        }

        <!-- Related blog posts -->
        @if (blogSlugs().length > 0) {
          <section class="related-section">
            <h2>Related blog posts</h2>
            <div class="related-cards">
              @for (slug of blogSlugs(); track slug) {
                <a [routerLink]="['/blog', slug]" class="related-card">{{ slug }}</a>
              }
            </div>
          </section>
        }

        <!-- Comments -->
        <section class="comments-section">
          <h2>Comments ({{ comments().length }})</h2>

          <!-- Add comment form -->
          <div class="comment-form">
            <input type="text" placeholder="Your name" class="comment-name"
                   [(ngModel)]="commentName">
            <textarea placeholder="Add a comment..." class="comment-body"
                      [(ngModel)]="commentContent" rows="3"></textarea>
            <button class="comment-submit" (click)="submitComment()" [disabled]="!commentName || !commentContent">
              Post comment
            </button>
          </div>

          <!-- Comments list -->
          @for (comment of comments(); track comment.id) {
            <div class="comment-card">
              <div class="comment-header">
                <span class="comment-author">{{ comment.authorName }}</span>
                <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
              </div>
              <p class="comment-text">{{ comment.content }}</p>
              <button class="reply-btn" (click)="replyTo.set(replyTo() === comment.id ? null : comment.id)">Reply</button>

              @if (replyTo() === comment.id) {
                <div class="reply-form">
                  <input type="text" placeholder="Your name" class="comment-name"
                         [(ngModel)]="replyName">
                  <textarea placeholder="Write a reply..." class="comment-body"
                            [(ngModel)]="replyContent" rows="2"></textarea>
                  <button class="comment-submit" (click)="submitReply(comment.id)" [disabled]="!replyName || !replyContent">
                    Post reply
                  </button>
                </div>
              }

              @if (comment.replies && comment.replies.length > 0) {
                @for (reply of comment.replies; track reply.id) {
                  <div class="comment-card reply-card">
                    <div class="comment-header">
                      <span class="comment-author">{{ reply.authorName }}</span>
                      <span class="comment-date">{{ formatDate(reply.createdAt) }}</span>
                    </div>
                    <p class="comment-text">{{ reply.content }}</p>
                  </div>
                }
              }
            </div>
          }
        </section>

        <div class="paper-footer">
          <a routerLink="/papers" class="back-link">&larr; Back to Papers</a>
        </div>
      </article>
    } @else {
      <div class="empty-state">
        <p>Paper not found.</p>
        <a routerLink="/papers">&larr; Back to Papers</a>
      </div>
    }
  `,
  styles: [`
    .paper-detail {
      max-width: 780px;
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

    /* Status banner */
    .status-banner {
      padding: 12px 16px;
      border-radius: 8px;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.7);
      margin-bottom: 20px;
      line-height: 1.5;
    }
    .superseded-link {
      color: var(--brand-primary, #4338ca);
      margin-left: 8px;
      text-decoration: none;
    }
    .superseded-link:hover {
      text-decoration: underline;
    }

    /* Header */
    .paper-header {
      margin-bottom: 24px;
    }
    .paper-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      flex-wrap: wrap;
    }
    .step-badge {
      padding: 2px 10px;
      border-radius: 12px;
      background: rgba(67, 56, 202, 0.08);
      color: var(--brand-primary, #4338ca);
      font-size: 12px;
      font-weight: 700;
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
    }
    .phase-badge {
      padding: 2px 10px;
      border-radius: 12px;
      background: rgba(0, 0, 0, 0.04);
      color: rgba(0, 0, 0, 0.6);
      font-size: 12px;
      font-weight: 600;
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
      font-size: 14px;
      font-weight: 700;
    }
    .proof-badge {
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 11px;
      font-weight: 600;
    }
    .proof-mech {
      background: rgba(5, 150, 105, 0.1);
      color: #059669;
    }
    .proof-partial {
      background: rgba(245, 158, 11, 0.1);
      color: #f59e0b;
    }

    h1 {
      font-size: 26px;
      font-weight: 600;
      margin: 0;
      line-height: 1.3;
      color: rgba(0, 0, 0, 0.87);
    }

    h2 {
      font-size: 18px;
      font-weight: 600;
      margin: 0 0 12px;
      color: rgba(0, 0, 0, 0.8);
    }

    /* Abstract */
    .abstract-section {
      margin-bottom: 28px;
    }
    .abstract-text {
      font-size: 15px;
      line-height: 1.7;
      color: rgba(0, 0, 0, 0.65);
      margin: 0;
    }

    /* Metadata table */
    .metadata-section {
      margin-bottom: 28px;
    }
    .meta-table {
      width: 100%;
      border-collapse: collapse;
    }
    .meta-table tr {
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    }
    .meta-table td {
      padding: 8px 0;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.7);
      vertical-align: top;
    }
    .meta-key {
      width: 140px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.5);
    }
    .dep-link {
      display: inline-block;
      padding: 1px 6px;
      margin: 2px 4px 2px 0;
      border-radius: 4px;
      background: rgba(67, 56, 202, 0.06);
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
      font-size: 13px;
    }
    .dep-link:hover {
      background: rgba(67, 56, 202, 0.12);
    }
    code {
      background: rgba(0, 0, 0, 0.04);
      padding: 1px 6px;
      border-radius: 4px;
      font-size: 13px;
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
    }

    /* PDF button */
    .pdf-section {
      margin-bottom: 28px;
    }
    .pdf-btn {
      display: inline-block;
      padding: 10px 28px;
      background: var(--brand-primary, #4338ca);
      color: white;
      border-radius: 8px;
      text-decoration: none;
      font-size: 14px;
      font-weight: 600;
      transition: background 0.15s;
    }
    .pdf-btn:hover {
      background: #3730a3;
    }

    /* BibTeX */
    .bibtex-section {
      margin-bottom: 28px;
    }
    .bibtex-block {
      position: relative;
    }
    .bibtex-block pre {
      background: rgba(0, 0, 0, 0.03);
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      padding: 14px;
      overflow-x: auto;
      font-size: 13px;
      margin: 0;
    }
    .bibtex-block pre code {
      background: none;
      padding: 0;
    }
    .copy-btn {
      position: absolute;
      top: 8px;
      right: 8px;
      padding: 4px 12px;
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-radius: 6px;
      background: white;
      font-size: 12px;
      cursor: pointer;
      color: rgba(0, 0, 0, 0.6);
    }
    .copy-btn:hover {
      border-color: var(--brand-primary, #4338ca);
      color: var(--brand-primary, #4338ca);
    }

    /* Reactions */
    .reactions-section {
      margin-bottom: 28px;
    }
    .reactions-bar {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
    .reaction-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-radius: 20px;
      background: white;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.15s;
      color: rgba(0, 0, 0, 0.6);
    }
    .reaction-btn:hover {
      border-color: var(--brand-primary, #4338ca);
      color: var(--brand-primary, #4338ca);
    }
    .reaction-btn.reacted {
      background: rgba(67, 56, 202, 0.08);
      border-color: var(--brand-primary, #4338ca);
      color: var(--brand-primary, #4338ca);
    }
    .reaction-icon {
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
      font-size: 12px;
    }
    .reaction-label {
      font-size: 12px;
    }
    .reaction-count {
      font-size: 12px;
      font-weight: 600;
      color: var(--brand-primary, #4338ca);
    }

    /* Revision */
    .revision-section {
      margin-bottom: 28px;
    }
    .expand-btn {
      padding: 6px 14px;
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-radius: 6px;
      background: white;
      font-size: 13px;
      cursor: pointer;
      color: rgba(0, 0, 0, 0.6);
    }
    .expand-btn:hover {
      border-color: var(--brand-primary, #4338ca);
    }
    .revision-notes {
      margin-top: 12px;
      padding: 14px;
      background: rgba(0, 0, 0, 0.02);
      border-radius: 8px;
      font-size: 14px;
      line-height: 1.6;
      color: rgba(0, 0, 0, 0.65);
    }

    /* Related */
    .related-section {
      margin-bottom: 28px;
    }
    .related-cards {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }
    .related-card {
      padding: 8px 16px;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      text-decoration: none;
      color: var(--brand-primary, #4338ca);
      font-size: 14px;
      transition: all 0.15s;
    }
    .related-card:hover {
      border-color: var(--brand-primary, #4338ca);
      background: rgba(67, 56, 202, 0.04);
    }

    /* Comments */
    .comments-section {
      margin-bottom: 28px;
    }
    .comment-form, .reply-form {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-bottom: 20px;
    }
    .reply-form {
      margin-top: 12px;
      margin-left: 24px;
    }
    .comment-name {
      padding: 8px 12px;
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-radius: 8px;
      font-size: 14px;
      outline: none;
      max-width: 260px;
    }
    .comment-name:focus {
      border-color: var(--brand-primary, #4338ca);
    }
    .comment-body {
      padding: 10px 12px;
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-radius: 8px;
      font-size: 14px;
      font-family: inherit;
      outline: none;
      resize: vertical;
    }
    .comment-body:focus {
      border-color: var(--brand-primary, #4338ca);
    }
    .comment-submit {
      align-self: flex-start;
      padding: 8px 20px;
      background: var(--brand-primary, #4338ca);
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.15s;
    }
    .comment-submit:hover {
      background: #3730a3;
    }
    .comment-submit:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .comment-card {
      padding: 16px;
      border: 1px solid rgba(0, 0, 0, 0.06);
      border-radius: 10px;
      margin-bottom: 12px;
      background: white;
    }
    .reply-card {
      margin-left: 24px;
      margin-top: 10px;
      background: rgba(0, 0, 0, 0.015);
    }
    .comment-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;
    }
    .comment-author {
      font-size: 14px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.8);
    }
    .comment-date {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.4);
    }
    .comment-text {
      font-size: 14px;
      line-height: 1.6;
      color: rgba(0, 0, 0, 0.65);
      margin: 0;
    }
    .reply-btn {
      margin-top: 8px;
      padding: 4px 10px;
      border: none;
      background: none;
      font-size: 12px;
      color: var(--brand-primary, #4338ca);
      cursor: pointer;
    }
    .reply-btn:hover {
      text-decoration: underline;
    }

    /* Footer */
    .paper-footer {
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid rgba(0, 0, 0, 0.08);
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
      h1 { font-size: 21px; }
      .meta-key { width: 110px; }
      .reactions-bar { gap: 6px; }
      .reaction-btn { padding: 4px 10px; font-size: 12px; }
      .reply-form { margin-left: 12px; }
      .reply-card { margin-left: 12px; }
    }
  `],
})
export class PaperDetailComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private sub: Subscription | null = null;

  paper = signal<StepPaper | null>(null);
  bibtex = signal<string>('');
  comments = signal<PaperComment[]>([]);
  loading = signal(false);
  copied = signal(false);
  revisionsExpanded = signal(false);
  userReacted = signal<Set<string>>(new Set());
  replyTo = signal<number | null>(null);

  reactions = REACTIONS;

  commentName = '';
  commentContent = '';
  replyName = '';
  replyContent = '';

  statusBanner = computed(() => {
    const p = this.paper();
    if (!p) return null;
    return STATUS_BANNERS[p.status] || null;
  });

  blogSlugs = computed(() => {
    const p = this.paper();
    if (!p || !p.blogSlugs) return [];
    return p.blogSlugs.split(',').map(s => s.trim()).filter(s => s.length > 0);
  });

  ngOnInit(): void {
    this.sub = this.route.paramMap.subscribe(params => {
      const slug = params.get('slug');
      if (slug) {
        this.loadPaper(slug);
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
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

  statusColor(status: string): string {
    return STATUS_COLORS[status] || '#6b7280';
  }

  gradeColor(grade: string): string {
    const colors: Record<string, string> = {
      'A+': '#059669', 'A': '#10b981', 'B+': '#3b82f6', 'B': '#6366f1', 'C': '#f59e0b', 'D': '#ef4444',
    };
    return colors[grade] || '#6b7280';
  }

  reactionCount(type: string): number {
    const p = this.paper();
    if (!p || !p.reactionCounts) return 0;
    return p.reactionCounts[type] || 0;
  }

  react(type: string): void {
    const p = this.paper();
    if (!p) return;
    this.api.addReaction(p.slug, type).subscribe({
      next: (counts) => {
        this.paper.set({ ...p, reactionCounts: counts });
        const reacted = new Set(this.userReacted());
        if (reacted.has(type)) {
          reacted.delete(type);
        } else {
          reacted.add(type);
        }
        this.userReacted.set(reacted);
      },
    });
  }

  copyBibtex(): void {
    navigator.clipboard.writeText(this.bibtex()).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  submitComment(): void {
    const p = this.paper();
    if (!p || !this.commentName || !this.commentContent) return;
    this.api.addComment(p.slug, { authorName: this.commentName, content: this.commentContent }).subscribe({
      next: (comment) => {
        this.comments.update(c => [...c, comment]);
        this.commentContent = '';
      },
    });
  }

  submitReply(parentId: number): void {
    const p = this.paper();
    if (!p || !this.replyName || !this.replyContent) return;
    this.api.addComment(p.slug, { authorName: this.replyName, content: this.replyContent, parentId }).subscribe({
      next: (reply) => {
        this.comments.update(comments => comments.map(c => {
          if (c.id === parentId) {
            return { ...c, replies: [...(c.replies || []), reply] };
          }
          return c;
        }));
        this.replyContent = '';
        this.replyTo.set(null);
      },
    });
  }

  private loadPaper(slug: string): void {
    this.loading.set(true);
    this.paper.set(null);
    this.bibtex.set('');
    this.comments.set([]);

    this.api.getPaper(slug).subscribe({
      next: (paper) => {
        this.paper.set(paper);
        this.loading.set(false);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: () => this.loading.set(false),
    });

    this.api.getPaperBibtex(slug).subscribe({
      next: (bib) => this.bibtex.set(bib),
      error: () => {},
    });

    this.api.getComments(slug).subscribe({
      next: (comments) => this.comments.set(comments),
      error: () => {},
    });
  }
}
