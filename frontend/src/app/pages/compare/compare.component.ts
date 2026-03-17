import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { CompareResponse } from '../../models/api.models';

@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <!-- Header -->
    <section class="compare-header">
      <h1>Type Comparison</h1>
      <p>Side-by-side subtyping, duality, state spaces, and recursion analysis</p>
    </section>

    <!-- Input bar -->
    <div class="input-bar">
      <div class="input-grid">
        <div class="input-col">
          <label for="compare-type1">Type 1</label>
          <textarea id="compare-type1" [(ngModel)]="type1" placeholder="&{read: end, write: end}"
                    spellcheck="false"
                    (keydown.control.enter)="compare()"
                    (keydown.meta.enter)="compare()"></textarea>
        </div>
        <div class="input-mid">
          <button class="compare-btn" (click)="compare()"
                  [disabled]="loading() || !type1.trim() || !type2.trim()"
                  aria-label="Compare types">
            @if (loading()) {
              <mat-spinner diameter="16" class="compare-spinner"></mat-spinner>
            } @else {
              <mat-icon>compare_arrows</mat-icon>
              Compare
            }
          </button>
          <span class="compare-hint">Ctrl+Enter</span>
        </div>
        <div class="input-col">
          <label for="compare-type2">Type 2</label>
          <textarea id="compare-type2" [(ngModel)]="type2" placeholder="&{read: end}"
                    spellcheck="false"
                    (keydown.control.enter)="compare()"
                    (keydown.meta.enter)="compare()"></textarea>
        </div>
      </div>
    </div>

    <!-- Examples -->
    <div class="examples-row">
      <span class="ex-label">Examples:</span>
      @for (ex of examples; track ex.name) {
        <button class="ex-chip" (click)="loadExample(ex)" [attr.aria-label]="'Load ' + ex.name + ' example'">{{ ex.name }}</button>
      }
    </div>

    <!-- Error -->
    @if (error()) {
      <div class="error-banner" role="alert">
        <div class="inner">
          <mat-icon aria-hidden="true">error_outline</mat-icon>
          {{ error() }}
        </div>
      </div>
    }

    <!-- Loading -->
    @if (loading()) {
      <div class="loading-state" role="status" aria-label="Comparing types">
        <mat-spinner diameter="32"></mat-spinner>
        <span>Comparing types...</span>
      </div>
    }

    <!-- Results -->
    @if (result(); as r) {
      <!-- Relationship banner -->
      <div class="relation-banner">
        <div class="relation-inner">
          <span class="rel-badge" [class.rel-equivalent]="r.type1SubtypeOfType2 && r.type2SubtypeOfType1"
                [class.rel-subtype]="(r.type1SubtypeOfType2 || r.type2SubtypeOfType1) && !(r.type1SubtypeOfType2 && r.type2SubtypeOfType1)"
                [class.rel-unrelated]="!r.type1SubtypeOfType2 && !r.type2SubtypeOfType1">
            {{ r.subtypingRelation }}
          </span>
          <span class="rel-chip" [class.rel-pass]="r.type1SubtypeOfType2" [class.rel-fail]="!r.type1SubtypeOfType2">
            {{ r.type1SubtypeOfType2 ? '\u2713' : '\u2717' }} T1 &lt;: T2
          </span>
          <span class="rel-chip" [class.rel-pass]="r.type2SubtypeOfType1" [class.rel-fail]="!r.type2SubtypeOfType1">
            {{ r.type2SubtypeOfType1 ? '\u2713' : '\u2717' }} T2 &lt;: T1
          </span>
          <span class="rel-chip" [class.rel-pass]="r.areDuals" [class.rel-fail]="!r.areDuals">
            {{ r.areDuals ? '\u2713' : '\u2717' }} Duals
          </span>
        </div>
      </div>

      <!-- Side-by-side Hasse diagrams -->
      <div class="hasse-compare">
        <!-- Type 1 -->
        <div class="hasse-panel">
          <div class="panel-header">
            <span class="panel-title">Type 1</span>
            <div class="panel-meta">
              <span class="meta-chip" [class.meta-lattice]="r.isLattice1" [class.meta-not-lattice]="!r.isLattice1">
                {{ r.isLattice1 ? 'Lattice' : 'Not lattice' }}
              </span>
              <span class="meta-chip meta-states">{{ r.states1 }}s {{ r.transitions1 }}t</span>
            </div>
          </div>
          <div class="panel-pretty">{{ r.pretty1 }}</div>
          <figure class="panel-hasse" role="img" aria-label="Hasse diagram of Type 1" [innerHTML]="svg1()"></figure>
        </div>

        <!-- Type 2 -->
        <div class="hasse-panel">
          <div class="panel-header">
            <span class="panel-title">Type 2</span>
            <div class="panel-meta">
              <span class="meta-chip" [class.meta-lattice]="r.isLattice2" [class.meta-not-lattice]="!r.isLattice2">
                {{ r.isLattice2 ? 'Lattice' : 'Not lattice' }}
              </span>
              <span class="meta-chip meta-states">{{ r.states2 }}s {{ r.transitions2 }}t</span>
            </div>
          </div>
          <div class="panel-pretty">{{ r.pretty2 }}</div>
          <figure class="panel-hasse" role="img" aria-label="Hasse diagram of Type 2" [innerHTML]="svg2()"></figure>
        </div>
      </div>

      <!-- Properties comparison table -->
      <div class="props-section">
        <h2 class="props-title">Properties</h2>
        <table class="props-table" aria-label="Property comparison between Type 1 and Type 2">
          <thead>
            <tr>
              <th scope="col">Property</th>
              <th scope="col">Type 1</th>
              <th scope="col">Type 2</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Lattice</td>
              <td [class.prop-yes]="r.isLattice1" [class.prop-no]="!r.isLattice1">{{ r.isLattice1 ? 'Yes' : 'No' }}</td>
              <td [class.prop-yes]="r.isLattice2" [class.prop-no]="!r.isLattice2">{{ r.isLattice2 ? 'Yes' : 'No' }}</td>
            </tr>
            <tr>
              <td>States / Transitions</td>
              <td class="prop-val">{{ r.states1 }} / {{ r.transitions1 }}</td>
              <td class="prop-val">{{ r.states2 }} / {{ r.transitions2 }}</td>
            </tr>
            <tr>
              <td>Chomsky class</td>
              <td class="prop-val">{{ r.chomsky1 }}</td>
              <td class="prop-val">{{ r.chomsky2 }}</td>
            </tr>
            <tr>
              <td>Recursive</td>
              <td [class.prop-yes]="r.isRecursive1" [class.prop-no]="!r.isRecursive1">{{ r.isRecursive1 ? 'Yes' : 'No' }}</td>
              <td [class.prop-yes]="r.isRecursive2" [class.prop-no]="!r.isRecursive2">{{ r.isRecursive2 ? 'Yes' : 'No' }}</td>
            </tr>
            <tr>
              <td>Guarded</td>
              <td [class.prop-yes]="r.isGuarded1" [class.prop-no]="!r.isGuarded1">{{ r.isGuarded1 ? 'Yes' : 'No' }}</td>
              <td [class.prop-yes]="r.isGuarded2" [class.prop-no]="!r.isGuarded2">{{ r.isGuarded2 ? 'Yes' : 'No' }}</td>
            </tr>
            <tr>
              <td>Contractive</td>
              <td [class.prop-yes]="r.isContractive1" [class.prop-no]="!r.isContractive1">{{ r.isContractive1 ? 'Yes' : 'No' }}</td>
              <td [class.prop-yes]="r.isContractive2" [class.prop-no]="!r.isContractive2">{{ r.isContractive2 ? 'Yes' : 'No' }}</td>
            </tr>
            <tr>
              <td>Tail-recursive</td>
              <td [class.prop-yes]="r.isTailRecursive1" [class.prop-no]="!r.isTailRecursive1">{{ r.isTailRecursive1 ? 'Yes' : 'No' }}</td>
              <td [class.prop-yes]="r.isTailRecursive2" [class.prop-no]="!r.isTailRecursive2">{{ r.isTailRecursive2 ? 'Yes' : 'No' }}</td>
            </tr>
            <tr>
              <td>Dual</td>
              <td class="prop-val">{{ r.dual1 }}</td>
              <td class="prop-val">{{ r.dual2 }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    }
  `,
  styleUrl: './compare.component.scss',
})
export class CompareComponent implements OnInit {
  type1 = '';
  type2 = '';
  loading = signal(false);
  error = signal<string | null>(null);
  result = signal<CompareResponse | null>(null);
  svg1 = signal<SafeHtml>('');
  svg2 = signal<SafeHtml>('');

  examples = [
    { name: 'Subtype (width)', type1: '&{read: end, write: end}', type2: '&{read: end}' },
    { name: 'Duals', type1: '&{request: +{OK: end, ERROR: end}}', type2: '+{request: &{OK: end, ERROR: end}}' },
    { name: 'Equivalent', type1: 'rec X . &{next: X, stop: end}', type2: 'rec Y . &{next: Y, stop: end}' },
    { name: 'Unrelated', type1: '&{read: end}', type2: '+{write: end}' },
    { name: 'Recursive vs Finite', type1: 'rec X . &{send: X, done: end}', type2: '&{send: &{send: end, done: end}, done: end}' },
  ];

  constructor(
    private api: ApiService,
    private sanitizer: DomSanitizer,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['type1']) this.type1 = params['type1'];
      if (params['type2']) this.type2 = params['type2'];
      if (this.type1.trim() && this.type2.trim()) {
        setTimeout(() => this.compare(), 0);
      }
    });
  }

  loadExample(ex: { type1: string; type2: string }): void {
    this.type1 = ex.type1;
    this.type2 = ex.type2;
    this.compare();
  }

  compare(): void {
    if (!this.type1.trim() || !this.type2.trim()) return;
    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.api.compareTypes(this.type1.trim(), this.type2.trim()).subscribe({
      next: (r) => {
        this.result.set(r);
        this.svg1.set(this.sanitizer.bypassSecurityTrustHtml(r.svgHtml1));
        this.svg2.set(this.sanitizer.bypassSecurityTrustHtml(r.svgHtml2));
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error || 'Comparison failed');
        this.loading.set(false);
      },
    });
  }
}
