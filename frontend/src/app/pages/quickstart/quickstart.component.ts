import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';

@Component({
  selector: 'app-quickstart',
  standalone: true,
  imports: [RouterLink, MatTabsModule, CodeBlockComponent],
  template: `
    <header class="page-header">
      <h1>Quick Start</h1>
      <p>Get up and running in 5 minutes. Choose your language:</p>
    </header>

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
export class QuickstartComponent {
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
    <groupId>com.bica</groupId>
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
}
