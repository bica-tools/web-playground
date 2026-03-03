import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatExpansionModule } from '@angular/material/expansion';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';

@Component({
  selector: 'app-tutorials',
  standalone: true,
  imports: [RouterLink, MatTabsModule, MatExpansionModule, CodeBlockComponent],
  template: `
    <header class="page-header">
      <h1>Tutorials</h1>
      <p>Step-by-step guides to get started with session types.</p>
    </header>

    <!-- ================================================================ -->
    <!-- QUICK START                                                      -->
    <!-- ================================================================ -->
    <section class="tutorial-section">
      <h2>Quick Start</h2>
      <p>Get up and running in 5 minutes. Choose your language:</p>

      <mat-tab-group animationDuration="200ms">
        <!-- Python tab -->
        <mat-tab label="Python (Reticulate)">
          <div class="tab-content">
            <div class="tutorial-step">
              <h3>Step 1: Install</h3>
              <p>Clone the repository and run directly (Python 3.11+, no dependencies):</p>
              <app-code-block [code]="pythonInstallSource" label="bash"></app-code-block>
            </div>

            <div class="tutorial-step">
              <h3>Step 2: Define a protocol</h3>
              <app-code-block [code]="pythonDefine" label="python"></app-code-block>
            </div>

            <div class="tutorial-step">
              <h3>Step 3: Build state space and check</h3>
              <app-code-block [code]="pythonBuild" label="python"></app-code-block>
            </div>

            <div class="tutorial-step">
              <h3>Step 4: CLI usage</h3>
              <app-code-block [code]="pythonCli" label="bash"></app-code-block>
            </div>
          </div>
        </mat-tab>

        <!-- Java tab -->
        <mat-tab label="Java (BICA Reborn)">
          <div class="tab-content">
            <div class="tutorial-step">
              <h3>Step 1: Add Maven dependency</h3>
              <app-code-block [code]="javaDep" label="xml"></app-code-block>
            </div>

            <div class="tutorial-step">
              <h3>Step 2: Annotate your class</h3>
              <app-code-block [code]="javaAnnotate" label="java"></app-code-block>
            </div>

            <div class="tutorial-step">
              <h3>Step 3: Compile</h3>
              <app-code-block [code]="javaCompile" label="bash"></app-code-block>
            </div>

            <div class="tutorial-step">
              <h3>Step 4: Violations are compile errors</h3>
              <app-code-block [code]="javaViolation" label="java"></app-code-block>
            </div>
          </div>
        </mat-tab>

        <!-- Web tab -->
        <mat-tab label="Web (Browser)">
          <div class="tab-content">
            <div class="tutorial-step">
              <h3>Step 1: Open the analyzer</h3>
              <p>Go to the <a routerLink="/tools/analyzer">interactive analyzer</a> &mdash; no installation required.</p>
            </div>

            <div class="tutorial-step">
              <h3>Step 2: Enter a session type</h3>
              <p>Type or paste a session type string, for example:</p>
              <app-code-block code="open . &{read: close . end, write: close . end}" label="session type"></app-code-block>
            </div>

            <div class="tutorial-step">
              <h3>Step 3: Click "Analyze"</h3>
              <p>You'll see the state space metrics, lattice verdict, Hasse diagram, and optionally generated JUnit 5 tests.</p>
            </div>

            <div class="tutorial-step">
              <h3>Step 4: Try a benchmark</h3>
              <p>
                Select from the benchmark dropdown to load one of the
                <a routerLink="/benchmarks">34 verified protocols</a>.
              </p>
            </div>
          </div>
        </mat-tab>
      </mat-tab-group>
    </section>

    <!-- ================================================================ -->
    <!-- TUTORIAL 1: @Session                                             -->
    <!-- ================================================================ -->
    <section class="tutorial-section">
      <h2>Tutorial 1: Typechecking with <code>&#64;Session</code></h2>
      <p>
        BICA Reborn is a Java annotation processor that enforces communication protocols
        at <strong>compile time</strong>. You annotate a class with <code>&#64;Session</code>,
        write the protocol as a session type string, and the compiler checks that every
        client uses the object correctly.
      </p>

      <div class="tutorial-step">
        <h3>Step 1: Add the Dependency</h3>
        <p>Clone the repository and install it to your local Maven cache:</p>
        <app-code-block [code]="tut1Install" label="bash"></app-code-block>
        <p>Then add the dependency to your project:</p>
        <app-code-block [code]="tut1Dep" label="pom.xml"></app-code-block>
      </div>

      <div class="tutorial-step">
        <h3>Step 2: Annotate Your Class</h3>
        <app-code-block [code]="tut1Annotate" label="FileHandle.java"></app-code-block>
        <p>
          This type says: first call <code>open</code>, then choose either
          <code>read</code> or <code>write</code>, then call <code>close</code>,
          then the protocol ends.
        </p>
      </div>

      <div class="tutorial-step">
        <h3>Step 3: Implement the Protocol</h3>
        <app-code-block [code]="tut1Implement" label="FileHandle.java (full)"></app-code-block>
      </div>

      <div class="tutorial-step">
        <h3>Step 4: Write Correct Client Code</h3>
        <app-code-block [code]="tut1Client" label="Client.java"></app-code-block>
      </div>

      <div class="tutorial-step">
        <h3>Step 5: See Compile-Time Errors</h3>
        <app-code-block [code]="tut1BadClient" label="BadClient.java"></app-code-block>
        <p>The error message tells you exactly which methods are available in the current state.</p>
      </div>

      <div class="tutorial-step">
        <h3>Step 6: Add Concurrency Annotations</h3>
        <p>
          When your session type uses <code>&parallel;</code>, methods in concurrent branches
          need concurrency annotations:
        </p>
        <ul>
          <li><code>&#64;Shared</code> &mdash; safe for concurrent access</li>
          <li><code>&#64;ReadOnly</code> &mdash; read-only access</li>
          <li><code>&#64;Exclusive</code> &mdash; exclusive access</li>
        </ul>
      </div>

      <div class="tutorial-step">
        <h3>Step 7: Generate Tests Automatically</h3>
        <p>
          BICA can generate JUnit 5 test suites from a session type covering valid paths,
          violations, and incomplete prefixes.
        </p>
        <app-code-block [code]="tut1TestGen" label="bash"></app-code-block>
        <p>
          You can also generate tests from the
          <a routerLink="/tools/analyzer">interactive tool</a> by entering a class name.
        </p>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- TUTORIAL 2: Reticulate CLI                                       -->
    <!-- ================================================================ -->
    <section class="tutorial-section">
      <h2>Tutorial 2: Reticulate CLI</h2>
      <p>
        Reticulate is a Python command-line tool that takes a session type string and
        performs the full analysis pipeline: <strong>parse</strong>, <strong>build</strong>
        the state space, <strong>check</strong> lattice properties, and optionally
        <strong>visualize</strong> the Hasse diagram.
      </p>

      <div class="tutorial-step">
        <h3>Step 1: Install Reticulate</h3>
        <app-code-block [code]="tut2Install" label="bash"></app-code-block>
        <p>You only need Python 3.11+ and the standard library.
          The optional <code>graphviz</code> Python package is needed for Hasse diagrams.</p>
      </div>

      <div class="tutorial-step">
        <h3>Step 2: Your First Analysis</h3>
        <app-code-block code='python -m reticulate "a . b . end"' label="bash"></app-code-block>
        <p>Output:</p>
        <app-code-block [code]="tut2Output" label="output"></app-code-block>
      </div>

      <div class="tutorial-step">
        <h3>Step 3: Branches and Selections</h3>
        <app-code-block [code]="tut2Branch" label="bash"></app-code-block>
        <p>
          This gives a 4-state diamond: the initial state branches into <code>read</code>
          and <code>write</code>, both merging at <code>close</code>.
        </p>
      </div>

      <div class="tutorial-step">
        <h3>Step 4: Recursion</h3>
        <app-code-block [code]="tut2Rec" label="bash"></app-code-block>
        <p>
          The SCC quotient collapses cyclic states. The Recursion Lemma guarantees that
          if the quotient is a lattice, so is the original.
        </p>
      </div>

      <div class="tutorial-step">
        <h3>Step 5: The Parallel Constructor</h3>
        <app-code-block code='python -m reticulate "(a.end || b.end)"' label="bash"></app-code-block>
        <p>
          The state space is the product lattice L(S1) &times; L(S2),
          ordered componentwise.
        </p>
      </div>

      <div class="tutorial-step">
        <h3>Step 6: Visualize with <code>--dot</code> and <code>--hasse</code></h3>
        <app-code-block [code]="tut2Viz" label="bash"></app-code-block>
      </div>

      <div class="tutorial-step">
        <h3>Step 7: Real-World Protocols</h3>
        <app-code-block [code]="tut2RealWorld" label="bash"></app-code-block>
        <p>
          The project includes <strong>34 benchmark protocols</strong>.
          Browse them in the <a routerLink="/benchmarks">benchmarks gallery</a> or paste any
          protocol into the <a routerLink="/tools/analyzer">interactive analyzer</a>.
        </p>
      </div>
    </section>

    <p class="analyzer-link">
      <a routerLink="/tools/analyzer">Open the interactive analyzer &rarr;</a>
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
    .page-header p {
      color: rgba(0, 0, 0, 0.6);
      margin: 0;
    }

    .tutorial-section {
      margin: 32px 0 40px;
    }
    .tutorial-section h2 {
      font-size: 22px;
      font-weight: 500;
      margin-bottom: 12px;
    }
    .tutorial-section > p {
      line-height: 1.7;
      margin-bottom: 16px;
    }
    .tutorial-section ul {
      line-height: 1.8;
      margin-bottom: 12px;
    }

    .tab-content {
      padding: 24px 0;
    }

    .tutorial-step {
      margin-bottom: 24px;
    }
    .tutorial-step h3 {
      font-size: 16px;
      font-weight: 500;
      margin: 0 0 12px;
    }
    .tutorial-step p {
      line-height: 1.7;
      margin: 8px 0;
    }
    .tutorial-step a {
      color: var(--brand-primary, #4338ca);
      text-decoration: none;
    }
    .tutorial-step a:hover {
      text-decoration: underline;
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
export class TutorialsComponent {
  // === Quick Start snippets ===

  readonly pythonInstallSource = `git clone https://github.com/zuacaldeira/SessionTypesResearch.git
cd SessionTypesResearch/reticulate
python3 -m reticulate "end"`;

  readonly pythonDefine = `from reticulate import parse, build_statespace, check_lattice, pretty

# Parse a session type
ast = parse("open . &{read: close . end, write: close . end}")
print(pretty(ast))
# &{open: &{read: &{close: end}, write: &{close: end}}}`;

  readonly pythonBuild = `# Build the state space
ss = build_statespace(ast)
print(f"{len(ss.states)} states, {len(ss.transitions)} transitions")

# Check lattice properties
result = check_lattice(ss)
print(f"Is lattice: {result.is_lattice}")  # True`;

  readonly pythonCli = `# From the command line
python -m reticulate "open . &{read: close . end, write: close . end}"

# Generate a Hasse diagram
python -m reticulate --hasse diagram.svg "open . &{read: close . end, write: close . end}"`;

  readonly javaDep = `<dependency>
    <groupId>com.zuacaldeira.bica</groupId>
    <artifactId>bica-reborn</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>`;

  readonly javaAnnotate = `import com.bica.reborn.annotation.Session;

@Session("open . &{read: close . end, write: close . end}")
public class FileHandle {
    public void open() { /* ... */ }
    public String read() { /* ... */ }
    public void write(String data) { /* ... */ }
    public void close() { /* ... */ }
}`;

  readonly javaCompile = `# The annotation processor runs automatically during compilation
mvn compile

# If the protocol is well-formed, you'll see:
# [INFO] Session type for FileHandle verified: 4 states, 4 transitions, lattice OK`;

  readonly javaViolation = `// Bad client code — calls read() after close()
FileHandle f = new FileHandle();
f.open();
f.close();
f.read();  // COMPILE ERROR: Method 'read' not enabled in state 3`;

  // === Tutorial 1: @Session snippets ===

  readonly tut1Install = `git clone https://github.com/zuacaldeira/SessionTypesResearch.git
cd SessionTypesResearch/bica
mvn install`;

  readonly tut1Dep = `<dependency>
    <groupId>com.bica</groupId>
    <artifactId>bica-reborn</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>`;

  readonly tut1Annotate = `import com.bica.reborn.annotation.Session;

@Session("open . &{read: close . end, write: close . end}")
public class FileHandle {
    // ...
}`;

  readonly tut1Implement = `import com.bica.reborn.annotation.Session;

@Session("open . &{read: close . end, write: close . end}")
public class FileHandle {
    private enum State { INITIAL, OPENED, PENDING_CLOSE, CLOSED }
    private State state = State.INITIAL;

    public void open() {
        requireState(State.INITIAL);
        state = State.OPENED;
    }

    public void read() {
        requireState(State.OPENED);
        state = State.PENDING_CLOSE;
    }

    public void write() {
        requireState(State.OPENED);
        state = State.PENDING_CLOSE;
    }

    public void close() {
        requireState(State.PENDING_CLOSE);
        state = State.CLOSED;
    }

    private void requireState(State expected) {
        if (state != expected)
            throw new IllegalStateException(
                "Expected " + expected + ", got " + state);
    }
}`;

  readonly tut1Client = `public class Client {
    void use() {
        FileHandle f = new FileHandle();
        f.open();
        f.read();
        f.close();
        // Compiles \u2014 follows protocol: open \u2192 read \u2192 close \u2192 end
    }
}`;

  readonly tut1BadClient = `public class BadClient {
    void use() {
        FileHandle f = new FileHandle();
        f.read();   // ERROR: 'read' not enabled in state 0 (enabled: [open])
        f.close();
    }
}`;

  readonly tut1TestGen = `java -jar bica.jar --test-gen --class-name FileHandle \\
    "open . &{read: close . end, write: close . end}"`;

  // === Tutorial 2: Reticulate CLI snippets ===

  readonly tut2Install = `git clone https://github.com/zuacaldeira/SessionTypesResearch.git
cd SessionTypesResearch/reticulate
python -m reticulate "end"`;

  readonly tut2Output = `Session type: a . b . end
States:       3
Transitions:  2
SCCs:         3
Lattice:      \u2713 yes`;

  readonly tut2Branch = `python -m reticulate "&{read: close . end, write: close . end}"`;

  readonly tut2Rec = `python -m reticulate "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"`;

  readonly tut2Viz = `python -m reticulate --dot "a . b . end"
python -m reticulate --dot "a . b . end" | dot -Tpng -o diagram.png
python -m reticulate --hasse diagram "a . b . end"`;

  readonly tut2RealWorld = `python -m reticulate "connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}"`;
}
