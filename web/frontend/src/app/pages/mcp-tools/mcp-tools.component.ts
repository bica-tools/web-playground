import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ApiService } from '../../services/api.service';
import { SubtypeResponse, DualResponse, TraceResponse } from '../../models/api.models';

@Component({
  selector: 'app-mcp-tools',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <!-- Header -->
    <section class="mcp-header">
      <h1>MCP Tools</h1>
      <p>Subtype checking, duality computation, and trace validation</p>
    </section>

    <div class="panels">
      <!-- ===== Subtype Checker ===== -->
      <section class="panel">
        <h2 class="panel-title">
          <mat-icon>compare</mat-icon>
          Subtype Checker
        </h2>
        <p class="panel-desc">Check Gay-Hole subtyping between two session types.</p>

        <div class="two-inputs">
          <div class="input-field">
            <label for="sub-subtype">Subtype (candidate)</label>
            <textarea id="sub-subtype" [(ngModel)]="subSubtype"
                      placeholder="&{read: end, write: end}"
                      spellcheck="false"
                      (keydown.control.enter)="checkSubtype()"
                      (keydown.meta.enter)="checkSubtype()"></textarea>
          </div>
          <div class="input-field">
            <label for="sub-supertype">Supertype</label>
            <textarea id="sub-supertype" [(ngModel)]="subSupertype"
                      placeholder="&{read: end}"
                      spellcheck="false"
                      (keydown.control.enter)="checkSubtype()"
                      (keydown.meta.enter)="checkSubtype()"></textarea>
          </div>
        </div>

        <div class="action-row">
          <button class="action-btn" (click)="checkSubtype()"
                  [disabled]="subLoading() || !subSubtype.trim() || !subSupertype.trim()">
            @if (subLoading()) {
              <mat-spinner diameter="16"></mat-spinner>
            } @else {
              Check Subtype
            }
          </button>
          <div class="examples-mini">
            @for (ex of subtypeExamples; track ex.name) {
              <button class="ex-chip" (click)="loadSubtypeExample(ex)">{{ ex.name }}</button>
            }
          </div>
        </div>

        @if (subResult()) {
          <div class="result-card" [class.success]="subResult()!.isSubtype" [class.fail]="!subResult()!.isSubtype">
            <div class="result-badge">
              @if (subResult()!.isEquivalent) {
                <mat-icon>swap_horiz</mat-icon> Equivalent
              } @else if (subResult()!.isSubtype) {
                <mat-icon>check_circle</mat-icon> Subtype confirmed
              } @else {
                <mat-icon>cancel</mat-icon> Not a subtype
              }
            </div>
            <div class="result-detail">
              <span class="mono">{{ subResult()!.prettySubtype }}</span>
              <span class="relation">{{ subResult()!.relation }}</span>
              <span class="mono">{{ subResult()!.prettySupertype }}</span>
            </div>
            <div class="result-meta">
              <span>Subtype: {{ subResult()!.subtypeStates }} states</span>
              <span>Supertype: {{ subResult()!.supertypeStates }} states</span>
            </div>
            <div class="svg-row">
              <div class="svg-col">
                <h4>Subtype</h4>
                <div class="svg-wrap" [innerHTML]="subSvg1()"></div>
              </div>
              <div class="svg-col">
                <h4>Supertype</h4>
                <div class="svg-wrap" [innerHTML]="subSvg2()"></div>
              </div>
            </div>
          </div>
        }
      </section>

      <!-- ===== Dual Generator ===== -->
      <section class="panel">
        <h2 class="panel-title">
          <mat-icon>flip</mat-icon>
          Dual Generator
        </h2>
        <p class="panel-desc">Compute the dual of a session type (swap Branch/Select) and verify involution.</p>

        <div class="input-field">
          <label for="dual-type">Session type</label>
          <textarea id="dual-type" [(ngModel)]="dualType"
                    placeholder="&{request: +{OK: end, ERROR: end}}"
                    spellcheck="false"
                    (keydown.control.enter)="computeDual()"
                    (keydown.meta.enter)="computeDual()"></textarea>
        </div>

        <div class="action-row">
          <button class="action-btn" (click)="computeDual()"
                  [disabled]="dualLoading() || !dualType.trim()">
            @if (dualLoading()) {
              <mat-spinner diameter="16"></mat-spinner>
            } @else {
              Compute Dual
            }
          </button>
          <div class="examples-mini">
            @for (ex of dualExamples; track ex.name) {
              <button class="ex-chip" (click)="loadDualExample(ex)">{{ ex.name }}</button>
            }
          </div>
        </div>

        @if (dualResult()) {
          <div class="result-card success">
            <div class="result-detail dual-detail">
              <div class="dual-pair">
                <span class="label">Original:</span>
                <span class="mono">{{ dualResult()!.prettyOriginal }}</span>
              </div>
              <div class="dual-pair">
                <span class="label">Dual:</span>
                <span class="mono">{{ dualResult()!.prettyDual }}</span>
              </div>
            </div>
            <div class="result-meta badges">
              <span class="badge" [class.ok]="dualResult()!.isInvolution">
                {{ dualResult()!.isInvolution ? 'Involution OK' : 'Involution FAIL' }}
              </span>
              <span class="badge" [class.ok]="dualResult()!.isIsomorphic">
                {{ dualResult()!.isIsomorphic ? 'Isomorphic' : 'Not isomorphic' }}
              </span>
              <span class="badge" [class.ok]="dualResult()!.selectionFlipped">
                {{ dualResult()!.selectionFlipped ? 'Selection flipped' : 'Selection not flipped' }}
              </span>
            </div>
            <div class="svg-row">
              <div class="svg-col">
                <h4>Original ({{ dualResult()!.originalStates }} states)</h4>
                <div class="svg-wrap" [innerHTML]="dualSvg1()"></div>
              </div>
              <div class="svg-col">
                <h4>Dual ({{ dualResult()!.dualStates }} states)</h4>
                <div class="svg-wrap" [innerHTML]="dualSvg2()"></div>
              </div>
            </div>
          </div>
        }
      </section>

      <!-- ===== Trace Validator ===== -->
      <section class="panel">
        <h2 class="panel-title">
          <mat-icon>route</mat-icon>
          Trace Validator
        </h2>
        <p class="panel-desc">Validate a method-call trace against a session type.</p>

        <div class="two-inputs">
          <div class="input-field">
            <label for="trace-type">Session type</label>
            <textarea id="trace-type" [(ngModel)]="traceType"
                      placeholder="rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"
                      spellcheck="false"
                      (keydown.control.enter)="validateTrace()"
                      (keydown.meta.enter)="validateTrace()"></textarea>
          </div>
          <div class="input-field">
            <label for="trace-input">Trace (comma or space separated)</label>
            <textarea id="trace-input" [(ngModel)]="traceInput"
                      placeholder="hasNext, TRUE, next, hasNext, FALSE"
                      spellcheck="false"
                      (keydown.control.enter)="validateTrace()"
                      (keydown.meta.enter)="validateTrace()"></textarea>
          </div>
        </div>

        <div class="action-row">
          <button class="action-btn" (click)="validateTrace()"
                  [disabled]="traceLoading() || !traceType.trim() || !traceInput.trim()">
            @if (traceLoading()) {
              <mat-spinner diameter="16"></mat-spinner>
            } @else {
              Validate Trace
            }
          </button>
          <div class="examples-mini">
            @for (ex of traceExamples; track ex.name) {
              <button class="ex-chip" (click)="loadTraceExample(ex)">{{ ex.name }}</button>
            }
          </div>
        </div>

        @if (traceResult()) {
          <div class="result-card" [class.success]="traceResult()!.valid && traceResult()!.complete"
               [class.warn]="traceResult()!.valid && !traceResult()!.complete"
               [class.fail]="!traceResult()!.valid">
            <div class="result-badge">
              @if (traceResult()!.valid && traceResult()!.complete) {
                <mat-icon>check_circle</mat-icon> Valid and complete
              } @else if (traceResult()!.valid) {
                <mat-icon>warning</mat-icon> Valid but incomplete
              } @else {
                <mat-icon>cancel</mat-icon> Violation at "{{ traceResult()!.violationAt }}"
              }
            </div>
            <div class="trace-path">
              @for (step of traceResult()!.path; track $index) {
                <span class="trace-step" [class.valid]="step.valid" [class.invalid]="!step.valid">
                  {{ step.method }}
                  <span class="step-arrow">{{ step.valid ? '(' + step.from + ' -> ' + step.to + ')' : 'BLOCKED' }}</span>
                </span>
                @if (!$last) {
                  <span class="step-sep">-></span>
                }
              }
            </div>
            @if (!traceResult()!.valid) {
              <div class="result-meta">
                <span>Enabled methods at violation: {{ traceResult()!.path[traceResult()!.path.length - 1]?.enabled?.join(', ') || 'none' }}</span>
              </div>
            }
            @if (traceResult()!.valid && !traceResult()!.complete) {
              <div class="result-meta">
                <span>Enabled methods to continue: {{ traceResult()!.enabledAtEnd.join(', ') || 'none' }}</span>
              </div>
            }
          </div>
        }
      </section>
    </div>
  `,
  styleUrl: './mcp-tools.component.scss',
})
export class McpToolsComponent {
  // --- Subtype ---
  subSubtype = '';
  subSupertype = '';
  subLoading = signal(false);
  subResult = signal<SubtypeResponse | null>(null);
  subSvg1 = signal<SafeHtml>('');
  subSvg2 = signal<SafeHtml>('');

  // --- Dual ---
  dualType = '';
  dualLoading = signal(false);
  dualResult = signal<DualResponse | null>(null);
  dualSvg1 = signal<SafeHtml>('');
  dualSvg2 = signal<SafeHtml>('');

  // --- Trace ---
  traceType = '';
  traceInput = '';
  traceLoading = signal(false);
  traceResult = signal<TraceResponse | null>(null);

  // --- Examples ---
  subtypeExamples = [
    { name: 'Width', sub: '&{read: end, write: end}', sup: '&{read: end}' },
    { name: 'Equivalent', sub: '&{a: end}', sup: '&{a: end}' },
    { name: 'Unrelated', sub: '&{read: end}', sup: '&{write: end}' },
    { name: 'Selection', sub: '+{OK: end}', sup: '+{OK: end, ERROR: end}' },
  ];

  dualExamples = [
    { name: 'Branch/Select', type: '&{request: +{OK: end, ERROR: end}}' },
    { name: 'Iterator', type: 'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}' },
    { name: 'Simple', type: '&{a: end, b: end}' },
  ];

  traceExamples = [
    { name: 'Valid complete', type: 'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}', trace: 'hasNext, TRUE, next, hasNext, FALSE' },
    { name: 'Violation', type: '&{read: end, write: end}', trace: 'read, write' },
    { name: 'Incomplete', type: '&{open: &{read: &{close: end}}}', trace: 'open, read' },
  ];

  constructor(private api: ApiService, private sanitizer: DomSanitizer) {}

  checkSubtype(): void {
    if (!this.subSubtype.trim() || !this.subSupertype.trim()) return;
    this.subLoading.set(true);
    this.subResult.set(null);
    this.api.checkSubtype(this.subSubtype.trim(), this.subSupertype.trim()).subscribe({
      next: (r) => {
        this.subResult.set(r);
        this.subSvg1.set(this.sanitizer.bypassSecurityTrustHtml(r.svgSubtype));
        this.subSvg2.set(this.sanitizer.bypassSecurityTrustHtml(r.svgSupertype));
        this.subLoading.set(false);
      },
      error: () => this.subLoading.set(false),
    });
  }

  computeDual(): void {
    if (!this.dualType.trim()) return;
    this.dualLoading.set(true);
    this.dualResult.set(null);
    this.api.computeDual(this.dualType.trim()).subscribe({
      next: (r) => {
        this.dualResult.set(r);
        this.dualSvg1.set(this.sanitizer.bypassSecurityTrustHtml(r.svgOriginal));
        this.dualSvg2.set(this.sanitizer.bypassSecurityTrustHtml(r.svgDual));
        this.dualLoading.set(false);
      },
      error: () => this.dualLoading.set(false),
    });
  }

  validateTrace(): void {
    if (!this.traceType.trim() || !this.traceInput.trim()) return;
    this.traceLoading.set(true);
    this.traceResult.set(null);
    this.api.validateTrace(this.traceType.trim(), this.traceInput.trim()).subscribe({
      next: (r) => {
        this.traceResult.set(r);
        this.traceLoading.set(false);
      },
      error: () => this.traceLoading.set(false),
    });
  }

  loadSubtypeExample(ex: { sub: string; sup: string }): void {
    this.subSubtype = ex.sub;
    this.subSupertype = ex.sup;
  }

  loadDualExample(ex: { type: string }): void {
    this.dualType = ex.type;
  }

  loadTraceExample(ex: { type: string; trace: string }): void {
    this.traceType = ex.type;
    this.traceInput = ex.trace;
  }
}
