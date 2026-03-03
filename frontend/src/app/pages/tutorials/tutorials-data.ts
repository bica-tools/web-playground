export interface TutorialStep {
  title: string;
  prose: string;
  code?: string;
  codeLabel?: string;
}

export interface Tutorial {
  id: string;
  number: number;
  title: string;
  subtitle: string;
  steps: TutorialStep[];
}

export const TUTORIALS: Tutorial[] = [
  // ────────────────────────────────────────────────────────────
  // 1. Quick Start
  // ────────────────────────────────────────────────────────────
  {
    id: 'quick-start',
    number: 1,
    title: 'Quick Start',
    subtitle:
      'Get up and running in 5 minutes with Python, Java, or the web analyzer.',
    steps: [
      {
        title: 'Python: Install Reticulate',
        prose:
          'Clone the repository and run directly. You need Python 3.11+ with no extra dependencies:',
        code: `git clone https://github.com/zuacaldeira/SessionTypesResearch.git
cd SessionTypesResearch/reticulate
python3 -m reticulate "end"`,
        codeLabel: 'bash',
      },
      {
        title: 'Python: Define and analyze a protocol',
        prose:
          'Use the Python API to parse a session type, build its state space, and check lattice properties:',
        code: `from reticulate import parse, build_statespace, check_lattice, pretty

# Parse a session type
ast = parse("open . &{read: close . end, write: close . end}")
print(pretty(ast))

# Build the state space
ss = build_statespace(ast)
print(f"{len(ss.states)} states, {len(ss.transitions)} transitions")

# Check lattice properties
result = check_lattice(ss)
print(f"Is lattice: {result.is_lattice}")  # True`,
        codeLabel: 'python',
      },
      {
        title: 'Python: CLI usage',
        prose:
          'The command-line interface runs the full pipeline in one command:',
        code: `# Analyze a session type
python3 -m reticulate "open . &{read: close . end, write: close . end}"

# Generate a Hasse diagram
python3 -m reticulate --hasse diagram.svg "open . &{read: close . end, write: close . end}"`,
        codeLabel: 'bash',
      },
      {
        title: 'Java: Add BICA Reborn dependency',
        prose:
          'Add the Maven dependency to your project. The annotation processor runs automatically during compilation:',
        code: `<dependency>
    <groupId>com.zuacaldeira.bica</groupId>
    <artifactId>bica-reborn</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>`,
        codeLabel: 'pom.xml',
      },
      {
        title: 'Java: Annotate your class',
        prose:
          'Write the protocol as a <code>&#64;Session</code> string. The compiler verifies that every client uses the object correctly:',
        code: `import com.bica.reborn.annotation.Session;

@Session("open . &{read: close . end, write: close . end}")
public class FileHandle {
    public void open() { /* ... */ }
    public String read() { /* ... */ }
    public void write(String data) { /* ... */ }
    public void close() { /* ... */ }
}`,
        codeLabel: 'java',
      },
      {
        title: 'Java: Compile-time checking',
        prose:
          'Protocol violations become <strong>compile errors</strong>. Calling a method out of order is caught before runtime:',
        code: `# Compile — the processor runs automatically
mvn compile

# Bad client code — calls read() after close()
FileHandle f = new FileHandle();
f.open();
f.close();
f.read();  // COMPILE ERROR: Method 'read' not enabled in state 3`,
        codeLabel: 'java',
      },
      {
        title: 'Web: Browser analyzer',
        prose:
          'No installation required. Open the <a href="/tools/analyzer">interactive analyzer</a>, ' +
          'type or paste a session type, and click <strong>Analyze</strong>. ' +
          'You&rsquo;ll see the state space metrics, lattice verdict, Hasse diagram, and optionally generated JUnit 5 tests. ' +
          'Select from the benchmark dropdown to load one of the <a href="/benchmarks">34 verified protocols</a>.',
      },
    ],
  },

  // ────────────────────────────────────────────────────────────
  // 2. Your First Session Type
  // ────────────────────────────────────────────────────────────
  {
    id: 'first-session-type',
    number: 2,
    title: 'Your First Session Type',
    subtitle:
      'Write a simple protocol, understand the grammar, and see the state space visualized.',
    steps: [
      {
        title: 'What is a session type?',
        prose:
          'A <strong>session type</strong> is a type that describes a communication protocol on an object. ' +
          'Instead of a flat interface like <code>interface FileHandle { open(); read(); close(); }</code>, ' +
          'a session type specifies the <em>order</em> in which methods must be called. ' +
          'The object&rsquo;s type <strong>evolves</strong> as methods are invoked.',
      },
      {
        title: 'A minimal protocol',
        prose:
          'The simplest interesting session type is a sequence of method calls ending with <code>end</code>:',
        code: 'open . read . close . end',
        codeLabel: 'session type',
      },
      {
        title: 'Reading the type',
        prose:
          'Read it left to right: first call <code>open</code>, then <code>read</code>, ' +
          'then <code>close</code>, then the protocol is <strong>finished</strong> (<code>end</code>). ' +
          'Calling methods out of order &mdash; say, <code>read</code> before <code>open</code> &mdash; is a type error.',
      },
      {
        title: 'Run it through the analyzer',
        prose:
          'You can use the Python CLI or the <a href="/tools/analyzer">web analyzer</a>:',
        code: 'python3 -m reticulate "open . read . close . end"',
        codeLabel: 'bash',
      },
      {
        title: 'Interpreting the output',
        prose:
          'The analyzer reports:',
        code: `Session type: open . read . close . end
States:       4
Transitions:  3
SCCs:         4
Lattice:      \u2713 yes`,
        codeLabel: 'output',
      },
      {
        title: 'What the numbers mean',
        prose:
          '<strong>4 states</strong>: the protocol stages &mdash; before <code>open</code>, after <code>open</code>, ' +
          'after <code>read</code>, and after <code>close</code> (<code>end</code>). ' +
          '<strong>3 transitions</strong>: one per method call. ' +
          '<strong>4 SCCs</strong>: no cycles, so each state is its own component. ' +
          '<strong>Lattice: yes</strong>: the state space is a bounded lattice &mdash; ' +
          'a linear chain is always a lattice.',
      },
      {
        title: 'Visualize it',
        prose:
          'Generate a Hasse diagram to see the state space as a graph:',
        code: 'python3 -m reticulate --dot "open . read . close . end"',
        codeLabel: 'bash',
      },
    ],
  },

  // ────────────────────────────────────────────────────────────
  // 3. Branching and Selection
  // ────────────────────────────────────────────────────────────
  {
    id: 'branching-selection',
    number: 3,
    title: 'Branching and Selection',
    subtitle:
      'Model an ATM with external choice (&) and internal choice (+), and understand the diamond pattern.',
    steps: [
      {
        title: 'External choice: the client decides',
        prose:
          'The <strong>branch</strong> constructor <code>&amp;{...}</code> offers the client a menu of methods. ' +
          'The client picks one. This is <em>external choice</em> &mdash; the environment controls the decision.',
        code: '&{withdraw: end, deposit: end, balance: end}',
        codeLabel: 'session type',
      },
      {
        title: 'Internal choice: the object decides',
        prose:
          'The <strong>selection</strong> constructor <code>+{...}</code> means the <em>object</em> picks the outcome. ' +
          'The client must be prepared to handle every possibility. Think of it as a return value.',
        code: '+{OK: end, ERROR: end}',
        codeLabel: 'session type',
      },
      {
        title: 'An ATM protocol',
        prose:
          'Combine both: the client chooses an operation, the ATM decides the outcome.',
        code: `&{
  withdraw: +{OK: end, INSUFFICIENT: end},
  deposit: end,
  balance: end
}`,
        codeLabel: 'session type',
      },
      {
        title: 'Analyze it',
        prose: 'Run the analysis:',
        code: `python3 -m reticulate "&{withdraw: +{OK: end, INSUFFICIENT: end}, deposit: end, balance: end}"`,
        codeLabel: 'bash',
      },
      {
        title: 'The diamond pattern',
        prose:
          'When a branch has two alternatives that converge to the same continuation, the state space ' +
          'forms a <strong>diamond</strong>: one state at the top (before the branch), two in the middle ' +
          '(the alternatives), one at the bottom (the convergence point). ' +
          'This is the simplest non-trivial lattice shape.',
      },
      {
        title: 'Selection vs Branch in the state space',
        prose:
          'Transitions from <code>&amp;{...}</code> and <code>+{...}</code> look the same in the state space ' +
          '&mdash; both are labeled edges. The difference is <strong>who controls the choice</strong>. ' +
          'The analyzer tracks this distinction with <code>selection_transitions</code>, which matters ' +
          'for test generation: a branch becomes a separate test case, while a selection becomes a ' +
          '<code>switch</code> on the return value.',
      },
    ],
  },

  // ────────────────────────────────────────────────────────────
  // 4. Recursive Protocols
  // ────────────────────────────────────────────────────────────
  {
    id: 'recursive-protocols',
    number: 4,
    title: 'Recursive Protocols',
    subtitle:
      'Build an iterator with rec, understand cycles and SCC quotient, and check termination.',
    steps: [
      {
        title: 'Why recursion?',
        prose:
          'Most real protocols loop: an iterator reads multiple items, a server handles ' +
          'repeated requests, a file reads until EOF. The <code>rec X . S</code> constructor ' +
          'names a loop point <code>X</code> and continues with body <code>S</code>, ' +
          'where <code>X</code> marks "go back to the start."',
      },
      {
        title: 'A Java-style iterator',
        prose: 'The classic <code>hasNext/next</code> pattern:',
        code: 'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}',
        codeLabel: 'session type',
      },
      {
        title: 'Reading it',
        prose:
          'Call <code>hasNext</code>. The object decides: if <code>TRUE</code>, ' +
          'call <code>next</code> and loop back to <code>X</code>. If <code>FALSE</code>, ' +
          'the protocol ends. Every loop iteration is one <code>hasNext &rarr; next</code> cycle.',
      },
      {
        title: 'Analyze it',
        prose: 'The analyzer handles recursion automatically:',
        code: `python3 -m reticulate "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"`,
        codeLabel: 'bash',
      },
      {
        title: 'Cycles and SCC quotient',
        prose:
          'Recursion creates <strong>cycles</strong> in the state space. The variable <code>X</code> ' +
          'loops back to the entry state, forming a strongly connected component (SCC). ' +
          'To check lattice properties, the analyzer <strong>quotients by SCCs</strong>: it collapses ' +
          'each cycle into a single node, producing an acyclic DAG. The <strong>Recursion Lemma</strong> ' +
          '(proved in Lean 4) guarantees this preserves the lattice property.',
      },
      {
        title: 'Termination checking',
        prose:
          'A recursive type is <strong>well-formed</strong> only if it has an exit path &mdash; ' +
          'a way to reach <code>end</code> without going through the recursion variable. ' +
          'The type <code>rec X . X</code> is <em>not</em> well-formed (infinite loop, no exit). ' +
          'The analyzer&rsquo;s termination checker rejects it:',
        code: `python3 -m reticulate "rec X . X"
# Error: non-terminating recursive type`,
        codeLabel: 'bash',
      },
      {
        title: 'Nested recursion',
        prose:
          'You can nest recursion: <code>rec X . &amp;{a: rec Y . &amp;{b: Y, c: X}, d: end}</code>. ' +
          'The inner loop (<code>Y</code>) can exit to the outer loop (<code>X</code>), ' +
          'which can exit to <code>end</code>. Each level adds its own SCC.',
      },
    ],
  },

  // ────────────────────────────────────────────────────────────
  // 5. The Parallel Constructor
  // ────────────────────────────────────────────────────────────
  {
    id: 'parallel-constructor',
    number: 5,
    title: 'The Parallel Constructor',
    subtitle:
      'Model concurrent access with \u2225, understand product lattices and WF-Par.',
    steps: [
      {
        title: 'Concurrent access on shared objects',
        prose:
          'The <strong>parallel constructor</strong> <code>S\u2081 \u2225 S\u2082</code> models two ' +
          'sub-protocols executing <em>concurrently</em> on a shared object. This is the ' +
          '<strong>key novelty</strong> of this work &mdash; and the reason lattice structure is ' +
          '<em>necessary</em>, not just nice.',
      },
      {
        title: 'A simple example',
        prose:
          'A file that can be read and written concurrently:',
        code: '(read . end || write . end)',
        codeLabel: 'session type',
      },
      {
        title: 'The product lattice',
        prose:
          'The state space of <code>S\u2081 \u2225 S\u2082</code> is the <strong>product</strong> ' +
          '<code>L(S\u2081) &times; L(S\u2082)</code>, ordered componentwise. For the example above: ' +
          'L(read.end) has 2 states and L(write.end) has 2 states, so the product has 2&times;2 = 4 states.',
        code: `python3 -m reticulate "(read . end || write . end)"`,
        codeLabel: 'bash',
      },
      {
        title: 'Why products guarantee lattices',
        prose:
          'A fundamental theorem in lattice theory: <strong>the product of two lattices is always ' +
          'a lattice</strong>. Meets and joins are computed componentwise. This means any well-formed ' +
          'session type using <code>\u2225</code> <em>necessarily</em> has a lattice state space. ' +
          'The parallel constructor is what makes lattice structure a <em>guaranteed</em> property.',
      },
      {
        title: 'WF-Par: well-formedness rules',
        prose:
          'Not every use of <code>\u2225</code> is valid. The <strong>WF-Par</strong> condition requires: ' +
          '<ol>' +
          '<li>Both branches must <strong>terminate</strong> (reach <code>end</code>)</li>' +
          '<li>Both branches must be <strong>well-formed</strong></li>' +
          '<li>Branches must be <strong>variable-disjoint</strong> (no shared recursion variables)</li>' +
          '<li>No <strong>nested</strong> <code>\u2225</code> inside branches</li>' +
          '</ol>',
      },
      {
        title: 'A richer example',
        prose:
          'A database connection with concurrent query and health-check:',
        code: '(query . process . end || ping . end)',
        codeLabel: 'session type',
      },
      {
        title: 'Analyze it',
        prose: 'The product gives 3&times;2 = 6 states:',
        code: `python3 -m reticulate "(query . process . end || ping . end)"`,
        codeLabel: 'bash',
      },
    ],
  },

  // ────────────────────────────────────────────────────────────
  // 6. Lattice Properties Explained
  // ────────────────────────────────────────────────────────────
  {
    id: 'lattice-properties',
    number: 6,
    title: 'Lattice Properties Explained',
    subtitle:
      'Walk through meet, join, top, and bottom on real benchmarks; understand counterexamples.',
    steps: [
      {
        title: 'What makes a lattice?',
        prose:
          'A <strong>bounded lattice</strong> is a partially ordered set with four properties: ' +
          '<ol>' +
          '<li><strong>Top</strong>: a greatest element (the initial protocol state)</li>' +
          '<li><strong>Bottom</strong>: a least element (the terminal state, <code>end</code>)</li>' +
          '<li><strong>Meet</strong>: every pair of elements has a greatest lower bound (convergence point)</li>' +
          '<li><strong>Join</strong>: every pair of elements has a least upper bound (divergence point)</li>' +
          '</ol>',
      },
      {
        title: 'Top and bottom',
        prose:
          'In a session type state space, <strong>top</strong> is the initial state ' +
          '(where the protocol starts) and <strong>bottom</strong> is the terminal state ' +
          '(<code>end</code>). Every state is reachable from top, and bottom is reachable ' +
          'from every state. These are colored in Hasse diagrams: ' +
          '<strong>green</strong> for top, <strong>red</strong> for bottom.',
      },
      {
        title: 'Meet: where paths converge',
        prose:
          'The <strong>meet</strong> (greatest lower bound) of two states is where their ' +
          'execution paths <em>converge</em>. In <code>&amp;{read: close . end, write: close . end}</code>, ' +
          'the meet of the read and write states is the close state &mdash; both paths must pass through it.',
        code: `python3 -m reticulate "&{read: close . end, write: close . end}"`,
        codeLabel: 'bash',
      },
      {
        title: 'Join: where paths diverge',
        prose:
          'The <strong>join</strong> (least upper bound) of two states is where their paths <em>diverge</em>. ' +
          'In the same example, the join of read and write is the initial state &mdash; the latest ' +
          'common ancestor where the branch happens.',
      },
      {
        title: 'A real benchmark: two-buyer protocol',
        prose:
          'The two-buyer protocol involves a buyer requesting a price, negotiating, and either ' +
          'accepting or declining. All branches eventually converge to the final state:',
        code: `python3 -m reticulate "requestPrice . receiveQuote . &{accept: pay . end, reject: end}"`,
        codeLabel: 'bash',
      },
      {
        title: 'When lattice check fails',
        prose:
          'If the analyzer says <strong>Lattice: no</strong>, it provides a <strong>counterexample</strong>: ' +
          'two states that lack a meet or join. This means the protocol has an ambiguous convergence ' +
          'or divergence point. The Reticulate Theorem proves this <em>cannot happen</em> for well-formed ' +
          'session types &mdash; so a failure indicates a malformed input, not a bug in the theory.',
      },
      {
        title: 'Explore in the web tool',
        prose:
          'The <a href="/tools/analyzer">interactive analyzer</a> renders the lattice verdict ' +
          'with highlighted counterexamples when the check fails. Try modifying a benchmark ' +
          'to break the lattice property and see what happens.',
      },
    ],
  },

  // ────────────────────────────────────────────────────────────
  // 7. Morphisms Between Protocols
  // ────────────────────────────────────────────────────────────
  {
    id: 'morphisms',
    number: 7,
    title: 'Morphisms Between Protocols',
    subtitle:
      'Compare two protocol versions using isomorphism, embedding, projection, and Galois connections.',
    steps: [
      {
        title: 'Why compare protocols?',
        prose:
          'When you evolve a protocol (add a method, remove a branch, refactor), you want to know ' +
          'the <em>relationship</em> between the old and new versions. Is the new protocol equivalent? ' +
          'A refinement? A simplification? <strong>Morphisms</strong> answer these questions precisely.',
      },
      {
        title: 'The morphism hierarchy',
        prose:
          'From strongest to weakest: ' +
          '<ol>' +
          '<li><strong>Isomorphism</strong> &mdash; bijective, order-preserving and reflecting. The protocols are structurally identical.</li>' +
          '<li><strong>Embedding</strong> &mdash; injective, order-preserving and reflecting. One protocol is a sub-protocol of the other.</li>' +
          '<li><strong>Projection</strong> &mdash; surjective, order-preserving. One protocol collapses states of the other.</li>' +
          '<li><strong>Homomorphism</strong> &mdash; order-preserving. The weakest structural map.</li>' +
          '</ol>',
      },
      {
        title: 'Example: protocol refinement',
        prose:
          'Consider adding an optional step to a protocol:',
        code: `# Protocol v1: simple
a . b . end

# Protocol v2: adds a branch after a
a . &{b: end, c: end}`,
        codeLabel: 'comparison',
      },
      {
        title: 'Checking morphisms in Python',
        prose:
          'Use the Reticulate API to find morphisms between state spaces:',
        code: `from reticulate import parse, build_statespace
from reticulate.morphism import classify_morphism, find_embedding

ss1 = build_statespace(parse("a . b . end"))
ss2 = build_statespace(parse("a . &{b: end, c: end}"))

result = classify_morphism(ss1, ss2)
print(result.kind)  # embedding, projection, or homomorphism`,
        codeLabel: 'python',
      },
      {
        title: 'Galois connections',
        prose:
          'A <strong>Galois connection</strong> is an adjunction between two lattices: ' +
          'a pair of maps (\u03b1, \u03b3) such that \u03b1(x) \u2264 y if and only if x \u2264 \u03b3(y). ' +
          'This captures <em>approximation</em>: \u03b1 abstracts and \u03b3 concretizes. ' +
          'This is useful when one protocol approximates another (e.g., a simplified model).',
      },
      {
        title: 'Practical use: version compatibility',
        prose:
          'If protocol v2 <em>embeds</em> into v1, then every client of v2 also works with v1 &mdash; ' +
          'the new version is backwards-compatible. If there is only a <em>projection</em>, some behavior ' +
          'was lost. If only a <em>homomorphism</em>, the structure was significantly changed. ' +
          'This gives a precise vocabulary for protocol evolution.',
      },
    ],
  },

  // ────────────────────────────────────────────────────────────
  // 8. Modeling Real Protocols
  // ────────────────────────────────────────────────────────────
  {
    id: 'modeling-real-protocols',
    number: 8,
    title: 'Modeling Real Protocols',
    subtitle:
      'Step-by-step walkthrough of encoding SMTP as a session type.',
    steps: [
      {
        title: 'Choosing a protocol',
        prose:
          'SMTP (Simple Mail Transfer Protocol) is an ideal first real-world encoding: ' +
          'it has a clear lifecycle (connect, authenticate, send, quit), loops, and error handling. ' +
          'We&rsquo;ll build the session type incrementally.',
      },
      {
        title: 'Step 1: The connection lifecycle',
        prose:
          'An SMTP session starts with <code>connect</code> and <code>ehlo</code>, ' +
          'then enters a loop of sending messages, and ends with <code>quit</code>:',
        code: 'connect . ehlo . rec X . &{mail: ..., quit: end}',
        codeLabel: 'session type (skeleton)',
      },
      {
        title: 'Step 2: The mail command',
        prose:
          'After <code>mail</code>, the client specifies a recipient with <code>rcpt</code>, ' +
          'then sends the message body with <code>data</code>:',
        code: 'mail . rcpt . data . ...',
        codeLabel: 'session type (fragment)',
      },
      {
        title: 'Step 3: Server response',
        prose:
          'The server decides if the message was accepted. On success or failure, loop back:',
        code: '+{OK: X, ERR: X}',
        codeLabel: 'session type (fragment)',
      },
      {
        title: 'Step 4: Complete SMTP session type',
        prose:
          'Putting it all together:',
        code: 'connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}',
        codeLabel: 'session type',
      },
      {
        title: 'Analyze it',
        prose: 'Run the full pipeline:',
        code: `python3 -m reticulate "connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}"`,
        codeLabel: 'bash',
      },
      {
        title: 'Other real protocols',
        prose:
          'The project includes <strong>34 benchmark protocols</strong> covering: ' +
          'SMTP, OAuth 2.0, HTTP, JDBC Connection, Java Iterator, two-buyer, MCP (AI agents), ' +
          'A2A (Google agents), Raft consensus, circuit breaker, saga, and more. ' +
          'Browse them all in the <a href="/benchmarks">benchmarks gallery</a>.',
      },
    ],
  },

  // ────────────────────────────────────────────────────────────
  // 9. Test Generation from Session Types
  // ────────────────────────────────────────────────────────────
  {
    id: 'test-generation',
    number: 9,
    title: 'Test Generation from Session Types',
    subtitle:
      'Generate JUnit 5 tests automatically: valid paths, violations, and incomplete prefixes.',
    steps: [
      {
        title: 'Three kinds of tests',
        prose:
          'A session type defines <em>exactly</em> which method sequences are legal. ' +
          'From this, we generate three kinds of tests: ' +
          '<ol>' +
          '<li><strong>Valid paths</strong>: complete legal sequences from top to bottom</li>' +
          '<li><strong>Violations</strong>: call a disabled method and expect an exception</li>' +
          '<li><strong>Incomplete prefixes</strong>: legal prefixes that stop before reaching <code>end</code></li>' +
          '</ol>',
      },
      {
        title: 'Valid paths',
        prose:
          'Every path from the initial state to <code>end</code> is a valid test case. ' +
          'For <code>&amp;{read: close . end, write: close . end}</code>, there are two valid paths:',
        code: `// Path 1: read → close
obj.read();
obj.close();

// Path 2: write → close
obj.write();
obj.close();`,
        codeLabel: 'java',
      },
      {
        title: 'Violations',
        prose:
          'At each reachable state, try every <em>disabled</em> method. It should throw:',
        code: `// In the initial state, close is not enabled
assertThrows(IllegalStateException.class, () -> obj.close());`,
        codeLabel: 'java',
      },
      {
        title: 'Selection-aware tests',
        prose:
          'When a path goes through a <code>+{...}</code> (selection), the test uses a ' +
          '<code>switch</code> on the return value instead of separate test cases. ' +
          'This avoids a cartesian explosion of paths:',
        code: `var result = obj.hasNext();
switch (result) {
    case TRUE -> obj.next();
    case FALSE -> { /* end */ }
}`,
        codeLabel: 'java',
      },
      {
        title: 'Generate with the CLI',
        prose: 'BICA Reborn generates complete JUnit 5 test files:',
        code: `java -jar bica.jar --test-gen --class-name FileHandle \\
    "open . &{read: close . end, write: close . end}"`,
        codeLabel: 'bash',
      },
      {
        title: 'Generate with the web tool',
        prose:
          'In the <a href="/tools/analyzer">interactive analyzer</a>, enter a class name ' +
          'alongside the session type. The generated JUnit 5 test source appears in a ' +
          'collapsible section below the analysis results. Copy it directly into your project.',
      },
      {
        title: 'Benchmark coverage',
        prose:
          'Across all 34 benchmarks, the test generator produces <strong>5,183 tests</strong> ' +
          '(average 152 per protocol). Seven protocols hit the 100-path cap and are truncated. ' +
          'This provides high structural coverage from the type alone, with zero manual test writing.',
      },
    ],
  },

  // ────────────────────────────────────────────────────────────
  // 10. Using BICA Reborn Annotations
  // ────────────────────────────────────────────────────────────
  {
    id: 'bica-annotations',
    number: 10,
    title: 'Using BICA Reborn Annotations',
    subtitle:
      'Annotate a Java class with @Session, @Shared, @ReadOnly, @Exclusive and run the processor.',
    steps: [
      {
        title: 'Setup',
        prose:
          'Add BICA Reborn to your Maven project. The annotation processor runs automatically during <code>mvn compile</code>.',
        code: `<dependency>
    <groupId>com.bica</groupId>
    <artifactId>bica-reborn</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>`,
        codeLabel: 'pom.xml',
      },
      {
        title: 'The @Session annotation',
        prose:
          '<code>&#64;Session</code> declares the protocol a class must follow. The annotation processor ' +
          'parses it, builds the state space, checks lattice properties, and reports errors at compile time.',
        code: `import com.bica.reborn.annotation.Session;

@Session("open . &{read: close . end, write: close . end}")
public class FileHandle {
    public void open() { /* ... */ }
    public String read() { /* ... */ }
    public void write(String data) { /* ... */ }
    public void close() { /* ... */ }
}`,
        codeLabel: 'java',
      },
      {
        title: 'What the processor checks',
        prose:
          'The annotation processor verifies: ' +
          '<ol>' +
          '<li>The session type <strong>parses</strong> correctly</li>' +
          '<li>Recursive types <strong>terminate</strong></li>' +
          '<li>Parallel branches satisfy <strong>WF-Par</strong></li>' +
          '<li>The state space is a <strong>lattice</strong></li>' +
          '<li>Methods used in the session type <strong>exist</strong> in the class</li>' +
          '</ol>',
      },
      {
        title: 'Concurrency annotations for \u2225',
        prose:
          'When your protocol uses the parallel constructor, methods in concurrent branches ' +
          'need concurrency annotations to ensure thread safety:',
        code: `import com.bica.reborn.annotation.*;

@Session("(read . end || write . end)")
public class ConcurrentFile {
    @ReadOnly
    public String read() { /* ... */ }

    @Exclusive
    public void write(String data) { /* ... */ }
}`,
        codeLabel: 'java',
      },
      {
        title: 'The concurrency lattice',
        prose:
          'BICA classifies methods into four concurrency levels: ' +
          '<code>SHARED</code> (top, safe for all), ' +
          '<code>SYNC</code> and <code>READ_ONLY</code> (middle), ' +
          '<code>EXCLUSIVE</code> (bottom, exclusive access). ' +
          'Two methods can run concurrently if and only if neither is <code>EXCLUSIVE</code>.',
      },
      {
        title: 'Selection return types',
        prose:
          'Methods at a selection point (<code>+{...}</code>) must return an enum whose variants ' +
          'match the selection labels:',
        code: `@Session("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")
public class MyIterator<T> {
    public enum HasNextResult { TRUE, FALSE }

    public HasNextResult hasNext() { /* ... */ }
    public T next() { /* ... */ }
}`,
        codeLabel: 'java',
      },
      {
        title: 'Compile and verify',
        prose: 'Build your project &mdash; the processor runs automatically:',
        code: `mvn compile
# [INFO] Session type for FileHandle verified: 4 states, 4 transitions, lattice OK
# [INFO] Session type for ConcurrentFile verified: 4 states, 4 transitions, lattice OK`,
        codeLabel: 'bash',
      },
    ],
  },

  // ────────────────────────────────────────────────────────────
  // 11. From Hasse Diagram to Protocol Understanding
  // ────────────────────────────────────────────────────────────
  {
    id: 'hasse-diagrams',
    number: 11,
    title: 'From Hasse Diagram to Protocol Understanding',
    subtitle:
      'Read and interpret Hasse diagrams; use visualization to debug protocol designs.',
    steps: [
      {
        title: 'What is a Hasse diagram?',
        prose:
          'A <strong>Hasse diagram</strong> is the canonical visualization of a partially ordered set. ' +
          'It shows elements as nodes and covering relations as edges (with implied upward direction). ' +
          'For session types, it is the state space graph with <strong>transitive edges removed</strong> &mdash; ' +
          'showing only the essential structure.',
      },
      {
        title: 'Reading the diagram',
        prose:
          '<strong>Top</strong> (green) is at the top &mdash; the initial protocol state. ' +
          '<strong>Bottom</strong> (red) is at the bottom &mdash; <code>end</code>. ' +
          'Edges are labeled with method names or selection labels. ' +
          'Downward paths = legal method sequences. ' +
          'Width = concurrency or branching. Height = protocol depth.',
      },
      {
        title: 'Generate a diagram',
        prose: 'Use the CLI to generate a DOT source or render to an image:',
        code: `# DOT source to stdout
python3 -m reticulate --dot "&{read: close . end, write: close . end}"

# Render to PNG (requires graphviz system package)
python3 -m reticulate --hasse diagram "&{read: close . end, write: close . end}"`,
        codeLabel: 'bash',
      },
      {
        title: 'Pattern: the chain',
        prose:
          'A <strong>chain</strong> (linear sequence) means no branching &mdash; a purely sequential protocol. ' +
          'Example: <code>a . b . c . end</code> produces a vertical line of 4 nodes.',
      },
      {
        title: 'Pattern: the diamond',
        prose:
          'A <strong>diamond</strong> means a branch with convergence. ' +
          'Example: <code>&amp;{a: end, b: end}</code> produces a 3-node diamond ' +
          '(top &rarr; a/b &rarr; bottom). Two diamonds stacked means nested branching.',
      },
      {
        title: 'Pattern: the grid',
        prose:
          'A <strong>grid</strong> (product lattice) comes from the parallel constructor. ' +
          '<code>(a . end || b . end)</code> produces a 2&times;2 grid: ' +
          'the top-left corner is the initial state, the bottom-right is end, ' +
          'and the other two corners represent "a done, b pending" and "a pending, b done."',
      },
      {
        title: 'SCC collapsing',
        prose:
          'When a protocol has recursion, the Hasse diagram shows the <strong>quotient</strong>: ' +
          'cyclic states are collapsed into single nodes. This gives a cleaner view of the ' +
          'protocol&rsquo;s essential structure without the visual clutter of back edges.',
      },
      {
        title: 'Debugging with diagrams',
        prose:
          'If a protocol doesn&rsquo;t behave as expected, generate the Hasse diagram: ' +
          '<ul>' +
          '<li>Too many states? Your branches may be creating unintended combinations.</li>' +
          '<li>Missing edges? Check that method names in the type match your expectations.</li>' +
          '<li>Lattice check fails? The counterexample states are highlighted in the diagram.</li>' +
          '</ul>' +
          'Use the <a href="/tools/analyzer">web analyzer</a> for instant visualization.',
      },
    ],
  },
];
