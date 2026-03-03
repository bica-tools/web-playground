import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatExpansionModule } from '@angular/material/expansion';
import { CodeBlockComponent } from '../../components/code-block/code-block.component';
import { TUTORIALS } from './tutorials-data';

interface TocEntry {
  id: string;
  label: string;
  level: number;
}

@Component({
  selector: 'app-tutorials',
  standalone: true,
  imports: [RouterLink, MatTabsModule, MatExpansionModule, CodeBlockComponent],
  template: `
    <header class="page-header">
      <h1>Tutorials</h1>
      <p>Step-by-step guides to session types, reticulates, and tooling ({{ tutorials.length + 1 }} tutorials).</p>
    </header>

    <div class="tut-layout">
      <!-- Sticky sidebar -->
      <aside class="tut-sidebar">
        <nav class="sidebar-nav">
          <h3>Contents</h3>
          <ul>
            @for (entry of tocEntries; track entry.id) {
              <li [class.sub]="entry.level === 2"
                  [class.active]="activeSection === entry.id">
                <a (click)="scrollTo(entry.id)">{{ entry.label }}</a>
              </li>
            }
          </ul>
        </nav>
      </aside>

      <!-- Main content -->
      <div class="tut-content">

        <!-- ============================================================ -->
        <!-- QUICK START                                                   -->
        <!-- ============================================================ -->
        <section class="tutorial-section" id="quick-start">
          <h2>Quick Start</h2>
          <p>Get up and running in 5 minutes. Choose your language:</p>

          <mat-tab-group animationDuration="200ms">
            <!-- Python tab -->
            <mat-tab label="Python (Reticulate)">
              <div class="tab-content">
                <div class="tutorial-step" id="qs-install">
                  <h3>Step 1: Install</h3>
                  <p>Clone the repository and run directly (Python 3.11+, no dependencies):</p>
                  <app-code-block [code]="pythonInstallSource" label="bash"></app-code-block>
                </div>

                <div class="tutorial-step" id="qs-define">
                  <h3>Step 2: Define a protocol</h3>
                  <app-code-block [code]="pythonDefine" label="python"></app-code-block>
                </div>

                <div class="tutorial-step" id="qs-build">
                  <h3>Step 3: Build state space and check</h3>
                  <app-code-block [code]="pythonBuild" label="python"></app-code-block>
                </div>

                <div class="tutorial-step" id="qs-cli">
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

        <!-- ============================================================ -->
        <!-- TUTORIALS 1–10 (data-driven)                                  -->
        <!-- ============================================================ -->
        @for (tut of tutorials; track tut.id) {
          <section class="tutorial-section" [id]="tut.id">
            <h2>Tutorial {{ tut.number }}: {{ tut.title }}</h2>
            <p>{{ tut.subtitle }}</p>

            @for (step of tut.steps; track step.title; let i = $index) {
              <div class="tutorial-step" [id]="stepId(tut.id, i)">
                <h3>Step {{ i + 1 }}: {{ step.title }}</h3>
                <p [innerHTML]="step.prose"></p>
                @if (step.code) {
                  <app-code-block [code]="step.code" [label]="step.codeLabel || ''"></app-code-block>
                }
              </div>
            }
          </section>
        }

        <p class="analyzer-link">
          <a routerLink="/tools/analyzer">Open the interactive analyzer &rarr;</a>
        </p>
      </div>
    </div>
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

    /* Sidebar layout */
    .tut-layout {
      display: flex;
      gap: 32px;
      align-items: flex-start;
    }

    .tut-sidebar {
      position: sticky;
      top: 80px;
      width: 260px;
      flex-shrink: 0;
      max-height: calc(100vh - 100px);
      overflow-y: auto;
    }

    .sidebar-nav h3 {
      font-size: 13px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: rgba(0, 0, 0, 0.5);
      margin: 0 0 12px;
      padding: 0 12px;
    }
    .sidebar-nav ul {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .sidebar-nav li {
      margin: 0;
    }
    .sidebar-nav li a {
      display: block;
      padding: 6px 12px;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.7);
      text-decoration: none;
      border-left: 3px solid transparent;
      cursor: pointer;
      transition: all 0.15s;
    }
    .sidebar-nav li.sub a {
      padding-left: 24px;
      font-size: 13px;
      color: rgba(0, 0, 0, 0.55);
    }
    .sidebar-nav li a:hover {
      color: var(--brand-primary, #4338ca);
      background: rgba(67, 56, 202, 0.04);
    }
    .sidebar-nav li.active a {
      color: var(--brand-primary, #4338ca);
      border-left-color: var(--brand-primary, #4338ca);
      font-weight: 500;
      background: rgba(67, 56, 202, 0.06);
    }
    .sidebar-nav li.active.sub a {
      font-weight: 500;
    }

    .tut-content {
      flex: 1;
      min-width: 0;
    }

    @media (max-width: 900px) {
      .tut-layout {
        flex-direction: column;
      }
      .tut-sidebar {
        position: static;
        width: 100%;
        max-height: none;
        border: 1px solid rgba(0, 0, 0, 0.08);
        border-radius: 8px;
        padding: 12px 0;
        background: rgba(0, 0, 0, 0.01);
      }
    }

    /* Content styles */
    .tutorial-section {
      margin: 40px 0;
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
    .tutorial-section ul, .tutorial-section ol {
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
export class TutorialsComponent implements OnInit, OnDestroy {
  readonly tutorials = TUTORIALS;
  activeSection = 'quick-start';
  sectionIds: string[] = [];

  readonly tocEntries: TocEntry[] = this.buildToc();

  ngOnInit(): void {
    this.sectionIds = this.tocEntries.map((e) => e.id);
  }

  ngOnDestroy(): void {}

  @HostListener('window:scroll')
  onScroll(): void {
    const offset = 120;
    for (let i = this.sectionIds.length - 1; i >= 0; i--) {
      const el = document.getElementById(this.sectionIds[i]);
      if (el && el.getBoundingClientRect().top <= offset) {
        this.activeSection = this.sectionIds[i];
        return;
      }
    }
    this.activeSection = this.sectionIds[0];
  }

  scrollTo(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  }

  stepId(tutId: string, stepIndex: number): string {
    return `${tutId}-step-${stepIndex}`;
  }

  private buildToc(): TocEntry[] {
    const entries: TocEntry[] = [
      { id: 'quick-start', label: 'Quick Start', level: 1 },
    ];
    for (const tut of TUTORIALS) {
      entries.push({
        id: tut.id,
        label: `${tut.number}. ${tut.title}`,
        level: 1,
      });
      for (let i = 0; i < tut.steps.length; i++) {
        entries.push({
          id: `${tut.id}-step-${i}`,
          label: tut.steps[i].title,
          level: 2,
        });
      }
    }
    return entries;
  }

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
}
