import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';

@Component({
  selector: 'app-pipeline',
  standalone: true,
  imports: [RouterLink, CodeBlockComponent],
  template: `
    <header class="page-header">
      <h1>The Verification Pipeline</h1>
      <p class="stats-line">
        7 stages &middot; Parse &rarr; Termination &rarr; WF-Par &rarr; State Space &rarr; Conformance &rarr; Lattice &rarr; Thread Safety
      </p>
    </header>

    <!-- Pipeline diagram -->
    <div class="pipeline-diagram">
      @for (stage of stages; track stage.num; let last = $last) {
        <div class="pipeline-stage">
          <span class="stage-num">{{ stage.num }}</span>
          <span class="stage-name">{{ stage.name }}</span>
        </div>
        @if (!last) {
          <span class="pipeline-arrow">&rarr;</span>
        }
      }
    </div>

    <p class="intro">
      Every <code>&#64;Session</code> annotation goes through a 7-stage verification pipeline
      at compile time. Each stage can reject the protocol with a specific error, and the
      pipeline stops at the first failure. This fail-fast design means errors are reported
      at the earliest possible stage, with the most relevant context.
    </p>

    <!-- Stage 1: Parse -->
    <section class="doc-section" id="parse">
      <h2>Stage 1: Parse</h2>
      <p>
        The parser transforms a session type string into an abstract syntax tree (AST).
        It uses recursive descent with a tokenizer that supports both ASCII and Unicode
        notation.
      </p>
      <h3>Grammar</h3>
      <app-code-block [code]="grammarCode" label="Grammar"></app-code-block>

      <h3>Parse Error Catalogue</h3>
      <div class="table-container">
        <table class="error-table">
          <thead>
            <tr><th>Category</th><th>Example Input</th><th>Error</th></tr>
          </thead>
          <tbody>
            <tr><td>Empty input</td><td><code>""</code></td><td>Unexpected end of input</td></tr>
            <tr><td>Unknown token</td><td><code>"&#64;#$"</code></td><td>Unexpected character '&#64;'</td></tr>
            <tr><td>Missing brace (branch)</td><td><code>"&amp;&#123;read: end"</code></td><td>Expected '&#125;' to close branch</td></tr>
            <tr><td>Missing brace (select)</td><td><code>"+&#123;OK: end"</code></td><td>Expected '&#125;' to close selection</td></tr>
            <tr><td>Missing colon</td><td><code>"&amp;&#123;read end&#125;"</code></td><td>Expected ':' after label</td></tr>
            <tr><td>Empty branch/select</td><td><code>"&amp;&#123;&#125;"</code></td><td>Expected label in branch</td></tr>
            <tr><td>Missing rec variable</td><td><code>"rec . end"</code></td><td>Expected variable after 'rec'</td></tr>
            <tr><td>Missing rec dot</td><td><code>"rec X end"</code></td><td>Expected '.' after rec variable</td></tr>
            <tr><td>Missing rec body</td><td><code>"rec X ."</code></td><td>Unexpected end of input in rec body</td></tr>
            <tr><td>Unclosed parallel</td><td><code>"(a.end || b.end"</code></td><td>Expected ')' to close parallel</td></tr>
            <tr><td>Missing parallel RHS</td><td><code>"(a.end ||)"</code></td><td>Expected type after '||'</td></tr>
            <tr><td>Trailing tokens</td><td><code>"end end"</code></td><td>Unexpected token after complete type</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Stage 2: Termination -->
    <section class="doc-section" id="termination">
      <h2>Stage 2: Termination Check</h2>
      <p>
        Every recursive type must have an exit path that does not pass through the
        bound variable. This ensures all protocols eventually terminate.
      </p>
      <div class="example-card">
        <h4>Terminating (valid)</h4>
        <code>rec X . &amp;&#123;next: X, stop: end&#125;</code>
        <p>The <code>stop</code> branch reaches <code>end</code> without going through <code>X</code>.</p>
      </div>
      <div class="example-card">
        <h4>Non-terminating (rejected)</h4>
        <code>rec X . X</code>
        <p>No exit path &mdash; <code>X</code> is the only continuation.</p>
      </div>
      <div class="example-card">
        <h4>Non-terminating through branch</h4>
        <code>rec X . &amp;&#123;loop: X&#125;</code>
        <p>Every branch leads back to <code>X</code> &mdash; no escape.</p>
      </div>
    </section>

    <!-- Stage 3: WF-Par -->
    <section class="doc-section" id="wf-par">
      <h2>Stage 3: WF-Par (Well-Formed Parallel)</h2>
      <p>
        The parallel constructor <code>(S1 || S2)</code> has three well-formedness rules
        that ensure the product construction is sound:
      </p>
      <div class="example-card">
        <h4>Rule 1: Both branches must terminate</h4>
        <code>(rec X . X || a.end)</code> &mdash; <strong>rejected</strong>: left branch does not terminate.
      </div>
      <div class="example-card">
        <h4>Rule 2: No shared recursion variables</h4>
        <code>rec X . (X || a.end)</code> &mdash; <strong>rejected</strong>: <code>X</code> is free in the left branch but bound outside.
      </div>
      <div class="example-card">
        <h4>Rule 3: No nested parallel</h4>
        <code>((a.end || b.end) || c.end)</code> &mdash; <strong>rejected</strong>: parallel inside parallel.
      </div>
    </section>

    <!-- Stage 4: State Space -->
    <section class="doc-section" id="statespace">
      <h2>Stage 4: State Space Construction</h2>
      <p>
        The AST is compiled into a finite state machine. States are integer IDs,
        transitions are labeled edges. The construction handles recursion (via
        placeholder + merge), sequencing (bottom-to-top chaining), and parallel
        composition (via product construction).
      </p>
      <p>
        <strong>Example:</strong> <code>open . &amp;&#123;read: close . end, write: close . end&#125;</code>
        produces 4 states and 4 transitions.
      </p>
      <p>
        <a [routerLink]="['/tools/analyzer']"
           [queryParams]="{type: 'open . &{read: close . end, write: close . end}'}">
          Try it in the analyzer &rarr;
        </a>
      </p>
    </section>

    <!-- Stage 5: Conformance -->
    <section class="doc-section" id="conformance">
      <h2>Stage 5: Object Conformance</h2>
      <p>
        When a method <code>m</code> is followed by a selection <code>+&#123;OP1: S1, OP2: S2&#125;</code>,
        the method must return a type whose values cover all selection labels.
      </p>
      <div class="example-card">
        <h4>Valid: enum covers all labels</h4>
        <app-code-block [code]="conformanceValidCode" label="Java"></app-code-block>
      </div>
      <div class="example-card">
        <h4>Error: missing label</h4>
        <app-code-block [code]="conformanceErrorCode" label="Java"></app-code-block>
      </div>
      <div class="example-card">
        <h4>Valid: boolean for two labels</h4>
        <app-code-block [code]="conformanceBoolCode" label="Java"></app-code-block>
      </div>
    </section>

    <!-- Stage 6: Lattice -->
    <section class="doc-section" id="lattice">
      <h2>Stage 6: Lattice Check</h2>
      <p>
        The state space, ordered by reachability, must form a lattice. This means
        every pair of states has a unique least upper bound (join) and greatest lower
        bound (meet). The lattice structure is <em>necessary</em> for the parallel
        constructor: products of lattices are lattices.
      </p>
      <p>
        The checker quotients by strongly connected components (from recursive types),
        then checks all pairwise meets and joins on the quotient DAG.
      </p>
    </section>

    <!-- Stage 7: Thread Safety -->
    <section class="doc-section" id="thread-safety">
      <h2>Stage 7: Thread Safety</h2>
      <p>
        When the session type uses <code>||</code>, methods from different parallel
        branches may execute concurrently. Each method is classified on the concurrency
        lattice:
      </p>
      <ul>
        <li><strong>&#64;Shared</strong> &mdash; safe for any concurrent access</li>
        <li><strong>&#64;ReadOnly</strong> &mdash; safe with other reads, not with writes</li>
        <li><strong>&#64;Exclusive</strong> &mdash; must not be concurrent with anything</li>
      </ul>
      <div class="example-card">
        <h4>Error: exclusive methods in parallel</h4>
        <app-code-block [code]="threadSafetyCode" label="Java"></app-code-block>
      </div>
    </section>

    <p class="analyzer-link">
      <a routerLink="/tools/analyzer">Try the interactive analyzer &rarr;</a>
    </p>
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
    .stats-line {
      text-align: center;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.6);
      padding: 8px 0 16px;
    }
    .intro {
      line-height: 1.7;
      margin-bottom: 32px;
    }

    .pipeline-diagram {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      flex-wrap: wrap;
      padding: 24px 0;
      margin-bottom: 24px;
    }
    .pipeline-stage {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 12px 16px;
      border: 2px solid var(--brand-primary, #4338ca);
      border-radius: 8px;
      background: rgba(67, 56, 202, 0.05);
      min-width: 80px;
    }
    .stage-num {
      font-size: 12px;
      font-weight: 600;
      color: var(--brand-primary, #4338ca);
    }
    .stage-name {
      font-size: 14px;
      font-weight: 500;
    }
    .pipeline-arrow {
      font-size: 20px;
      color: rgba(0, 0, 0, 0.4);
    }

    .doc-section {
      margin: 32px 0;
    }
    .doc-section h2 {
      font-size: 20px;
      font-weight: 500;
      margin-bottom: 12px;
    }
    .doc-section h3 {
      font-size: 16px;
      font-weight: 500;
      margin: 16px 0 8px;
    }
    .doc-section p {
      line-height: 1.7;
      margin-bottom: 12px;
    }
    .doc-section ul {
      line-height: 1.8;
    }

    .example-card {
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      padding: 16px;
      margin: 12px 0;
      background: rgba(0, 0, 0, 0.01);
    }
    .example-card h4 {
      font-size: 14px;
      font-weight: 500;
      margin: 0 0 8px;
    }
    .example-card p {
      margin: 8px 0 0;
      font-size: 14px;
    }

    .table-container {
      overflow-x: auto;
    }
    .error-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
    }
    .error-table th {
      text-align: left;
      padding: 10px 16px;
      font-weight: 500;
      border-bottom: 2px solid rgba(0, 0, 0, 0.12);
    }
    .error-table td {
      padding: 8px 16px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    }

    .analyzer-link {
      text-align: center;
      padding: 32px 0;
      font-size: 16px;
    }
    .analyzer-link a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
    }
    .analyzer-link a:hover {
      text-decoration: underline;
    }
  `],
})
export class PipelineComponent {
  readonly stages = [
    { num: 1, name: 'Parse' },
    { num: 2, name: 'Termination' },
    { num: 3, name: 'WF-Par' },
    { num: 4, name: 'State Space' },
    { num: 5, name: 'Conformance' },
    { num: 6, name: 'Lattice' },
    { num: 7, name: 'Thread Safety' },
  ];

  readonly grammarCode = `S  ::=  &{ m\u2081 : S\u2081 , ... , m\u2099 : S\u2099 }     -- branch (external choice)
     |  +{ l\u2081 : S\u2081 , ... , l\u2099 : S\u2099 }     -- selection (internal choice)
     |  ( S\u2081 || S\u2082 )                      -- parallel
     |  rec X . S                          -- recursion
     |  X                                  -- variable
     |  end                                -- terminated
     |  S\u2081 . S\u2082                            -- sequencing (sugar)`;

  readonly conformanceValidCode = `@Session("authenticate . +{OK: dashboard . end, DENIED: end}")
class Auth {
    enum Result { OK, DENIED }
    Result authenticate() { ... }  // Correct: covers {OK, DENIED}
}`;

  readonly conformanceErrorCode = `@Session("authenticate . +{OK: dashboard . end, DENIED: end}")
class Auth {
    enum Result { OK }  // Error: missing DENIED
    Result authenticate() { ... }
}`;

  readonly conformanceBoolCode = `@Session("hasNext . +{TRUE: next . end, FALSE: end}")
class Iterator {
    boolean hasNext() { ... }  // Correct: boolean covers {TRUE, FALSE}
}`;

  readonly threadSafetyCode = `@Session("(write . end || read . end)")
class File {
    @Exclusive void write() { ... }
    @ReadOnly String read() { ... }  // Error: read || write not safe
}`;
}
