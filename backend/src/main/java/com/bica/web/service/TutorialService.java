package com.bica.web.service;

import com.bica.web.dto.TutorialDto;
import com.bica.web.dto.TutorialStepDto;
import com.bica.web.dto.TutorialSummaryDto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TutorialService {

    private final Map<String, TutorialDto> tutorials = new LinkedHashMap<>();

    public TutorialService() {
        for (var t : buildTutorials()) {
            tutorials.put(t.id(), t);
        }
    }

    public List<TutorialSummaryDto> getTutorials() {
        return tutorials.values().stream()
                .map(t -> new TutorialSummaryDto(t.id(), t.number(), t.title(), t.subtitle()))
                .toList();
    }

    public TutorialDto getTutorial(String id) {
        return tutorials.get(id);
    }

    private static TutorialStepDto step(String title, String prose) {
        return new TutorialStepDto(title, prose, null, null, null);
    }

    private static TutorialStepDto step(String title, String prose, String code, String codeLabel) {
        return new TutorialStepDto(title, prose, code, codeLabel, null);
    }

    private static TutorialStepDto interactive(String title, String prose, String typeString) {
        return new TutorialStepDto(title, prose, null, null, typeString);
    }

    private static List<TutorialDto> buildTutorials() {
        return List.of(
                // 1. Quick Start
                new TutorialDto("quick-start", 1, "Quick Start",
                        "Get up and running in 5 minutes with Python, Java, or the web analyzer.",
                        List.of(
                                step("Python: Install Reticulate",
                                        "Clone the repository and run directly. You need Python 3.11+ with no extra dependencies:",
                                        """
                                        git clone https://github.com/zuacaldeira/SessionTypesResearch.git
                                        cd SessionTypesResearch/reticulate
                                        python3 -m reticulate "end\"""", "bash"),
                                step("Python: Define and analyze a protocol",
                                        "Use the Python API to parse a session type, build its state space, and check lattice properties:",
                                        """
                                        from reticulate import parse, build_statespace, check_lattice, pretty

                                        # Parse a session type
                                        ast = parse("open . &{read: close . end, write: close . end}")
                                        print(pretty(ast))

                                        # Build the state space
                                        ss = build_statespace(ast)
                                        print(f"{len(ss.states)} states, {len(ss.transitions)} transitions")

                                        # Check lattice properties
                                        result = check_lattice(ss)
                                        print(f"Is lattice: {result.is_lattice}")  # True""", "python"),
                                step("Python: CLI usage",
                                        "The command-line interface runs the full pipeline in one command:",
                                        """
                                        # Analyze a session type
                                        python3 -m reticulate "open . &{read: close . end, write: close . end}"

                                        # Generate a Hasse diagram
                                        python3 -m reticulate --hasse diagram.svg "open . &{read: close . end, write: close . end}\"""", "bash"),
                                step("Java: Add BICA Reborn dependency",
                                        "Add the Maven dependency to your project. The annotation processor runs automatically during compilation:",
                                        """
                                        <dependency>
                                            <groupId>com.zuacaldeira.bica</groupId>
                                            <artifactId>bica-reborn</artifactId>
                                            <version>0.1.0-SNAPSHOT</version>
                                        </dependency>""", "pom.xml"),
                                step("Java: Annotate your class",
                                        "Write the protocol as a <code>@Session</code> string. The compiler verifies that every client uses the object correctly:",
                                        """
                                        import com.bica.reborn.annotation.Session;

                                        @Session("open . &{read: close . end, write: close . end}")
                                        public class FileHandle {
                                            public void open() { /* ... */ }
                                            public String read() { /* ... */ }
                                            public void write(String data) { /* ... */ }
                                            public void close() { /* ... */ }
                                        }""", "java"),
                                step("Java: Compile-time checking",
                                        "Protocol violations become <strong>compile errors</strong>. Calling a method out of order is caught before runtime:",
                                        """
                                        # Compile — the processor runs automatically
                                        mvn compile

                                        # Bad client code — calls read() after close()
                                        FileHandle f = new FileHandle();
                                        f.open();
                                        f.close();
                                        f.read();  // COMPILE ERROR: Method 'read' not enabled in state 3""", "java"),
                                step("Web: Browser analyzer",
                                        "No installation required. Open the <a href=\"/tools/analyzer\">interactive analyzer</a>, type or paste a session type, and click <strong>Analyze</strong>. You\u2019ll see the state space metrics, lattice verdict, Hasse diagram, and optionally generated JUnit 5 tests. Select from the benchmark dropdown to load one of the <a href=\"/benchmarks\">34 verified protocols</a>.")
                        )),

                // 2. Your First Session Type
                new TutorialDto("first-session-type", 2, "Your First Session Type",
                        "Write a simple protocol, understand the grammar, and see the state space visualized.",
                        List.of(
                                step("What is a session type?",
                                        "A <strong>session type</strong> is a type that describes a communication protocol on an object. Instead of a flat interface like <code>interface FileHandle { open(); read(); close(); }</code>, a session type specifies the <em>order</em> in which methods must be called. The object\u2019s type <strong>evolves</strong> as methods are invoked."),
                                step("A minimal protocol",
                                        "The simplest interesting session type is a sequence of method calls ending with <code>end</code>:",
                                        "open . read . close . end", "session type"),
                                step("Reading the type",
                                        "Read it left to right: first call <code>open</code>, then <code>read</code>, then <code>close</code>, then the protocol is <strong>finished</strong> (<code>end</code>). Calling methods out of order \u2014 say, <code>read</code> before <code>open</code> \u2014 is a type error."),
                                step("Run it through the analyzer",
                                        "You can use the Python CLI or the <a href=\"/tools/analyzer\">web analyzer</a>:",
                                        "python3 -m reticulate \"open . read . close . end\"", "bash"),
                                step("Interpreting the output",
                                        "The analyzer reports:",
                                        "Session type: open . read . close . end\nStates:       4\nTransitions:  3\nSCCs:         4\nLattice:      \u2713 yes", "output"),
                                step("What the numbers mean",
                                        "<strong>4 states</strong>: the protocol stages \u2014 before <code>open</code>, after <code>open</code>, after <code>read</code>, and after <code>close</code> (<code>end</code>). <strong>3 transitions</strong>: one per method call. <strong>4 SCCs</strong>: no cycles, so each state is its own component. <strong>Lattice: yes</strong>: the state space is a bounded lattice \u2014 a linear chain is always a lattice."),
                                step("Visualize it",
                                        "Generate a Hasse diagram to see the state space as a graph:",
                                        "python3 -m reticulate --dot \"open . read . close . end\"", "bash"),
                                interactive("Try it yourself",
                                        "Edit the session type below and watch the Hasse diagram update live. Try adding another method (e.g. <code>open . read . write . close . end</code>) or removing one.",
                                        "open . read . close . end")
                        )),

                // 3. Branching and Selection
                new TutorialDto("branching-selection", 3, "Branching and Selection",
                        "Model an ATM with external choice (&) and internal choice (+), and understand the diamond pattern.",
                        List.of(
                                step("External choice: the client decides",
                                        "The <strong>branch</strong> constructor <code>&amp;{...}</code> offers the client a menu of methods. The client picks one. This is <em>external choice</em> \u2014 the environment controls the decision.",
                                        "&{withdraw: end, deposit: end, balance: end}", "session type"),
                                step("Internal choice: the object decides",
                                        "The <strong>selection</strong> constructor <code>+{...}</code> means the <em>object</em> picks the outcome. The client must be prepared to handle every possibility. Think of it as a return value.",
                                        "+{OK: end, ERROR: end}", "session type"),
                                step("An ATM protocol",
                                        "Combine both: the client chooses an operation, the ATM decides the outcome.",
                                        "&{\n  withdraw: +{OK: end, INSUFFICIENT: end},\n  deposit: end,\n  balance: end\n}", "session type"),
                                step("Analyze it",
                                        "Run the analysis:",
                                        "python3 -m reticulate \"&{withdraw: +{OK: end, INSUFFICIENT: end}, deposit: end, balance: end}\"", "bash"),
                                step("The diamond pattern",
                                        "When a branch has two alternatives that converge to the same continuation, the state space forms a <strong>diamond</strong>: one state at the top (before the branch), two in the middle (the alternatives), one at the bottom (the convergence point). This is the simplest non-trivial lattice shape."),
                                step("Selection vs Branch in the state space",
                                        "Transitions from <code>&amp;{...}</code> and <code>+{...}</code> look the same in the state space \u2014 both are labeled edges. The difference is <strong>who controls the choice</strong>. The analyzer tracks this distinction with <code>selection_transitions</code>, which matters for test generation: a branch becomes a separate test case, while a selection becomes a <code>switch</code> on the return value."),
                                interactive("Build your own ATM",
                                        "Modify the ATM protocol below. Try adding a <code>transfer</code> option, or adding an <code>ERROR</code> outcome to <code>deposit</code>. Watch how the lattice shape changes.",
                                        "&{withdraw: +{OK: end, INSUFFICIENT: end}, deposit: end, balance: end}")
                        )),

                // 4. Recursive Protocols
                new TutorialDto("recursive-protocols", 4, "Recursive Protocols",
                        "Build an iterator with rec, understand cycles and SCC quotient, and check termination.",
                        List.of(
                                step("Why recursion?",
                                        "Most real protocols loop: an iterator reads multiple items, a server handles repeated requests, a file reads until EOF. The <code>rec X . S</code> constructor names a loop point <code>X</code> and continues with body <code>S</code>, where <code>X</code> marks \"go back to the start.\""),
                                step("A Java-style iterator",
                                        "The classic <code>hasNext/next</code> pattern:",
                                        "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}", "session type"),
                                step("Reading it",
                                        "Call <code>hasNext</code>. The object decides: if <code>TRUE</code>, call <code>next</code> and loop back to <code>X</code>. If <code>FALSE</code>, the protocol ends. Every loop iteration is one <code>hasNext \u2192 next</code> cycle."),
                                step("Analyze it",
                                        "The analyzer handles recursion automatically:",
                                        "python3 -m reticulate \"rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}\"", "bash"),
                                step("Cycles and SCC quotient",
                                        "Recursion creates <strong>cycles</strong> in the state space. The variable <code>X</code> loops back to the entry state, forming a strongly connected component (SCC). To check lattice properties, the analyzer <strong>quotients by SCCs</strong>: it collapses each cycle into a single node, producing an acyclic DAG. The <strong>Recursion Lemma</strong> (proved in Lean 4) guarantees this preserves the lattice property."),
                                step("Termination checking",
                                        "A recursive type is <strong>well-formed</strong> only if it has an exit path \u2014 a way to reach <code>end</code> without going through the recursion variable. The type <code>rec X . X</code> is <em>not</em> well-formed (infinite loop, no exit). The analyzer\u2019s termination checker rejects it:",
                                        "python3 -m reticulate \"rec X . X\"\n# Error: non-terminating recursive type", "bash"),
                                step("Nested recursion",
                                        "You can nest recursion: <code>rec X . &amp;{a: rec Y . &amp;{b: Y, c: X}, d: end}</code>. The inner loop (<code>Y</code>) can exit to the outer loop (<code>X</code>), which can exit to <code>end</code>. Each level adds its own SCC."),
                                interactive("Experiment with recursion",
                                        "Try the iterator below. What happens if you remove the <code>FALSE: end</code> branch? Or add a <code>reset</code> option that goes back to the start?",
                                        "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")
                        )),

                // 5. The Parallel Constructor
                new TutorialDto("parallel-constructor", 5, "The Parallel Constructor",
                        "Model concurrent access with \u2225, understand product lattices and WF-Par.",
                        List.of(
                                step("Concurrent access on shared objects",
                                        "The <strong>parallel constructor</strong> <code>S\u2081 \u2225 S\u2082</code> models two sub-protocols executing <em>concurrently</em> on a shared object. This is the <strong>key novelty</strong> of this work \u2014 and the reason lattice structure is <em>necessary</em>, not just nice."),
                                step("A simple example",
                                        "A file that can be read and written concurrently:",
                                        "(read . end || write . end)", "session type"),
                                step("The product lattice",
                                        "The state space of <code>S\u2081 \u2225 S\u2082</code> is the <strong>product</strong> <code>L(S\u2081) \u00d7 L(S\u2082)</code>, ordered componentwise. For the example above: L(read.end) has 2 states and L(write.end) has 2 states, so the product has 2\u00d72 = 4 states.",
                                        "python3 -m reticulate \"(read . end || write . end)\"", "bash"),
                                step("Why products guarantee lattices",
                                        "A fundamental theorem in lattice theory: <strong>the product of two lattices is always a lattice</strong>. Meets and joins are computed componentwise. This means any well-formed session type using <code>\u2225</code> <em>necessarily</em> has a lattice state space. The parallel constructor is what makes lattice structure a <em>guaranteed</em> property."),
                                step("WF-Par: well-formedness rules",
                                        "Not every use of <code>\u2225</code> is valid. The <strong>WF-Par</strong> condition requires: <ol><li>Both branches must <strong>terminate</strong> (reach <code>end</code>)</li><li>Both branches must be <strong>well-formed</strong></li><li>Branches must be <strong>variable-disjoint</strong> (no shared recursion variables)</li><li>No <strong>nested</strong> <code>\u2225</code> inside branches</li></ol>"),
                                step("A richer example",
                                        "A database connection with concurrent query and health-check:",
                                        "(query . process . end || ping . end)", "session type"),
                                step("Analyze it",
                                        "The product gives 3\u00d72 = 6 states:",
                                        "python3 -m reticulate \"(query . process . end || ping . end)\"", "bash"),
                                interactive("Build a parallel protocol",
                                        "Try the concurrent read/write file channel below. What happens to the state count if you add more methods to one branch? Try <code>(read . write . end || ping . pong . end)</code>.",
                                        "(read . end || write . end)")
                        )),

                // 6. Lattice Properties Explained
                new TutorialDto("lattice-properties", 6, "Lattice Properties Explained",
                        "Walk through meet, join, top, and bottom on real benchmarks; understand counterexamples.",
                        List.of(
                                step("What makes a lattice?",
                                        "A <strong>bounded lattice</strong> is a partially ordered set with four properties: <ol><li><strong>Top</strong>: a greatest element (the initial protocol state)</li><li><strong>Bottom</strong>: a least element (the terminal state, <code>end</code>)</li><li><strong>Meet</strong>: every pair of elements has a greatest lower bound (convergence point)</li><li><strong>Join</strong>: every pair of elements has a least upper bound (divergence point)</li></ol>"),
                                step("Top and bottom",
                                        "In a session type state space, <strong>top</strong> is the initial state (where the protocol starts) and <strong>bottom</strong> is the terminal state (<code>end</code>). Every state is reachable from top, and bottom is reachable from every state. These are colored in Hasse diagrams: <strong>green</strong> for top, <strong>red</strong> for bottom."),
                                step("Meet: where paths converge",
                                        "The <strong>meet</strong> (greatest lower bound) of two states is where their execution paths <em>converge</em>. In <code>&amp;{read: close . end, write: close . end}</code>, the meet of the read and write states is the close state \u2014 both paths must pass through it.",
                                        "python3 -m reticulate \"&{read: close . end, write: close . end}\"", "bash"),
                                step("Join: where paths diverge",
                                        "The <strong>join</strong> (least upper bound) of two states is where their paths <em>diverge</em>. In the same example, the join of read and write is the initial state \u2014 the latest common ancestor where the branch happens."),
                                step("A real benchmark: two-buyer protocol",
                                        "The two-buyer protocol involves a buyer requesting a price, negotiating, and either accepting or declining. All branches eventually converge to the final state:",
                                        "python3 -m reticulate \"requestPrice . receiveQuote . &{accept: pay . end, reject: end}\"", "bash"),
                                step("When lattice check fails",
                                        "If the analyzer says <strong>Lattice: no</strong>, it provides a <strong>counterexample</strong>: two states that lack a meet or join. This means the protocol has an ambiguous convergence or divergence point. The Reticulate Theorem proves this <em>cannot happen</em> for well-formed session types \u2014 so a failure indicates a malformed input, not a bug in the theory."),
                                step("Explore in the web tool",
                                        "The <a href=\"/tools/analyzer\">interactive analyzer</a> renders the lattice verdict with highlighted counterexamples when the check fails. Try modifying a benchmark to break the lattice property and see what happens.")
                        )),

                // 7. Morphisms Between Protocols
                new TutorialDto("morphisms", 7, "Morphisms Between Protocols",
                        "Compare two protocol versions using isomorphism, embedding, projection, and Galois connections.",
                        List.of(
                                step("Why compare protocols?",
                                        "When you evolve a protocol (add a method, remove a branch, refactor), you want to know the <em>relationship</em> between the old and new versions. Is the new protocol equivalent? A refinement? A simplification? <strong>Morphisms</strong> answer these questions precisely."),
                                step("The morphism hierarchy",
                                        "From strongest to weakest: <ol><li><strong>Isomorphism</strong> \u2014 bijective, order-preserving and reflecting. The protocols are structurally identical.</li><li><strong>Embedding</strong> \u2014 injective, order-preserving and reflecting. One protocol is a sub-protocol of the other.</li><li><strong>Projection</strong> \u2014 surjective, order-preserving. One protocol collapses states of the other.</li><li><strong>Homomorphism</strong> \u2014 order-preserving. The weakest structural map.</li></ol>"),
                                step("Example: protocol refinement",
                                        "Consider adding an optional step to a protocol:",
                                        "# Protocol v1: simple\na . b . end\n\n# Protocol v2: adds a branch after a\na . &{b: end, c: end}", "comparison"),
                                step("Checking morphisms in Python",
                                        "Use the Reticulate API to find morphisms between state spaces:",
                                        "from reticulate import parse, build_statespace\nfrom reticulate.morphism import classify_morphism, find_embedding\n\nss1 = build_statespace(parse(\"a . b . end\"))\nss2 = build_statespace(parse(\"a . &{b: end, c: end}\"))\n\nresult = classify_morphism(ss1, ss2)\nprint(result.kind)  # embedding, projection, or homomorphism", "python"),
                                step("Galois connections",
                                        "A <strong>Galois connection</strong> is an adjunction between two lattices: a pair of maps (\u03b1, \u03b3) such that \u03b1(x) \u2264 y if and only if x \u2264 \u03b3(y). This captures <em>approximation</em>: \u03b1 abstracts and \u03b3 concretizes. This is useful when one protocol approximates another (e.g., a simplified model)."),
                                step("Practical use: version compatibility",
                                        "If protocol v2 <em>embeds</em> into v1, then every client of v2 also works with v1 \u2014 the new version is backwards-compatible. If there is only a <em>projection</em>, some behavior was lost. If only a <em>homomorphism</em>, the structure was significantly changed. This gives a precise vocabulary for protocol evolution.")
                        )),

                // 8. Modeling Real Protocols
                new TutorialDto("modeling-real-protocols", 8, "Modeling Real Protocols",
                        "Step-by-step walkthrough of encoding SMTP as a session type.",
                        List.of(
                                step("Choosing a protocol",
                                        "SMTP (Simple Mail Transfer Protocol) is an ideal first real-world encoding: it has a clear lifecycle (connect, authenticate, send, quit), loops, and error handling. We\u2019ll build the session type incrementally."),
                                step("The connection lifecycle",
                                        "An SMTP session starts with <code>connect</code> and <code>ehlo</code>, then enters a loop of sending messages, and ends with <code>quit</code>:",
                                        "connect . ehlo . rec X . &{mail: ..., quit: end}", "session type (skeleton)"),
                                step("The mail command",
                                        "After <code>mail</code>, the client specifies a recipient with <code>rcpt</code>, then sends the message body with <code>data</code>:",
                                        "mail . rcpt . data . ...", "session type (fragment)"),
                                step("Server response",
                                        "The server decides if the message was accepted. On success or failure, loop back:",
                                        "+{OK: X, ERR: X}", "session type (fragment)"),
                                step("Complete SMTP session type",
                                        "Putting it all together:",
                                        "connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}", "session type"),
                                step("Analyze it",
                                        "Run the full pipeline:",
                                        "python3 -m reticulate \"connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}\"", "bash"),
                                step("Other real protocols",
                                        "The project includes <strong>34 benchmark protocols</strong> covering: SMTP, OAuth 2.0, HTTP, JDBC Connection, Java Iterator, two-buyer, MCP (AI agents), A2A (Google agents), Raft consensus, circuit breaker, saga, and more. Browse them all in the <a href=\"/benchmarks\">benchmarks gallery</a>.")
                        )),

                // 9. Test Generation from Session Types
                new TutorialDto("test-generation", 9, "Test Generation from Session Types",
                        "Generate JUnit 5 tests automatically: valid paths, violations, and incomplete prefixes.",
                        List.of(
                                step("Three kinds of tests",
                                        "A session type defines <em>exactly</em> which method sequences are legal. From this, we generate three kinds of tests: <ol><li><strong>Valid paths</strong>: complete legal sequences from top to bottom</li><li><strong>Violations</strong>: call a disabled method and expect an exception</li><li><strong>Incomplete prefixes</strong>: legal prefixes that stop before reaching <code>end</code></li></ol>"),
                                step("Valid paths",
                                        "Every path from the initial state to <code>end</code> is a valid test case. For <code>&amp;{read: close . end, write: close . end}</code>, there are two valid paths:",
                                        "// Path 1: read \u2192 close\nobj.read();\nobj.close();\n\n// Path 2: write \u2192 close\nobj.write();\nobj.close();", "java"),
                                step("Violations",
                                        "At each reachable state, try every <em>disabled</em> method. It should throw:",
                                        "// In the initial state, close is not enabled\nassertThrows(IllegalStateException.class, () -> obj.close());", "java"),
                                step("Selection-aware tests",
                                        "When a path goes through a <code>+{...}</code> (selection), the test uses a <code>switch</code> on the return value instead of separate test cases. This avoids a cartesian explosion of paths:",
                                        "var result = obj.hasNext();\nswitch (result) {\n    case TRUE -> obj.next();\n    case FALSE -> { /* end */ }\n}", "java"),
                                step("Generate with the CLI",
                                        "BICA Reborn generates complete JUnit 5 test files:",
                                        "java -jar bica.jar --test-gen --class-name FileHandle \\\n    \"open . &{read: close . end, write: close . end}\"", "bash"),
                                step("Generate with the web tool",
                                        "In the <a href=\"/tools/analyzer\">interactive analyzer</a>, enter a class name alongside the session type. The generated JUnit 5 test source appears in a collapsible section below the analysis results. Copy it directly into your project."),
                                step("Benchmark coverage",
                                        "Across all 34 benchmarks, the test generator produces <strong>5,183 tests</strong> (average 152 per protocol). Seven protocols hit the 100-path cap and are truncated. This provides high structural coverage from the type alone, with zero manual test writing.")
                        )),

                // 10. Using BICA Reborn Annotations
                new TutorialDto("bica-annotations", 10, "Using BICA Reborn Annotations",
                        "Annotate a Java class with @Session, @Shared, @ReadOnly, @Exclusive and run the processor.",
                        List.of(
                                step("Setup",
                                        "Add BICA Reborn to your Maven project. The annotation processor runs automatically during <code>mvn compile</code>.",
                                        "<dependency>\n    <groupId>com.bica</groupId>\n    <artifactId>bica-reborn</artifactId>\n    <version>0.1.0-SNAPSHOT</version>\n    <scope>provided</scope>\n</dependency>", "pom.xml"),
                                step("The @Session annotation",
                                        "<code>@Session</code> declares the protocol a class must follow. The annotation processor parses it, builds the state space, checks lattice properties, and reports errors at compile time.",
                                        "import com.bica.reborn.annotation.Session;\n\n@Session(\"open . &{read: close . end, write: close . end}\")\npublic class FileHandle {\n    public void open() { /* ... */ }\n    public String read() { /* ... */ }\n    public void write(String data) { /* ... */ }\n    public void close() { /* ... */ }\n}", "java"),
                                step("What the processor checks",
                                        "The annotation processor verifies: <ol><li>The session type <strong>parses</strong> correctly</li><li>Recursive types <strong>terminate</strong></li><li>Parallel branches satisfy <strong>WF-Par</strong></li><li>The state space is a <strong>lattice</strong></li><li>Methods used in the session type <strong>exist</strong> in the class</li></ol>"),
                                step("Concurrency annotations for \u2225",
                                        "When your protocol uses the parallel constructor, methods in concurrent branches need concurrency annotations to ensure thread safety:",
                                        "import com.bica.reborn.annotation.*;\n\n@Session(\"(read . end || write . end)\")\npublic class ConcurrentFile {\n    @ReadOnly\n    public String read() { /* ... */ }\n\n    @Exclusive\n    public void write(String data) { /* ... */ }\n}", "java"),
                                step("The concurrency lattice",
                                        "BICA classifies methods into four concurrency levels: <code>SHARED</code> (top, safe for all), <code>SYNC</code> and <code>READ_ONLY</code> (middle), <code>EXCLUSIVE</code> (bottom, exclusive access). Two methods can run concurrently if and only if neither is <code>EXCLUSIVE</code>."),
                                step("Selection return types",
                                        "Methods at a selection point (<code>+{...}</code>) must return an enum whose variants match the selection labels:",
                                        "@Session(\"rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}\")\npublic class MyIterator<T> {\n    public enum HasNextResult { TRUE, FALSE }\n\n    public HasNextResult hasNext() { /* ... */ }\n    public T next() { /* ... */ }\n}", "java"),
                                step("Compile and verify",
                                        "Build your project \u2014 the processor runs automatically:",
                                        "mvn compile\n# [INFO] Session type for FileHandle verified: 4 states, 4 transitions, lattice OK\n# [INFO] Session type for ConcurrentFile verified: 4 states, 4 transitions, lattice OK", "bash")
                        )),

                // 11. From Hasse Diagram to Protocol Understanding
                new TutorialDto("hasse-diagrams", 11, "From Hasse Diagram to Protocol Understanding",
                        "Read and interpret Hasse diagrams; use visualization to debug protocol designs.",
                        List.of(
                                step("What is a Hasse diagram?",
                                        "A <strong>Hasse diagram</strong> is the canonical visualization of a partially ordered set. It shows elements as nodes and covering relations as edges (with implied upward direction). For session types, it is the state space graph with <strong>transitive edges removed</strong> \u2014 showing only the essential structure."),
                                step("Reading the diagram",
                                        "<strong>Top</strong> (green) is at the top \u2014 the initial protocol state. <strong>Bottom</strong> (red) is at the bottom \u2014 <code>end</code>. Edges are labeled with method names or selection labels. Downward paths = legal method sequences. Width = concurrency or branching. Height = protocol depth."),
                                step("Generate a diagram",
                                        "Use the CLI to generate a DOT source or render to an image:",
                                        "# DOT source to stdout\npython3 -m reticulate --dot \"&{read: close . end, write: close . end}\"\n\n# Render to PNG (requires graphviz system package)\npython3 -m reticulate --hasse diagram \"&{read: close . end, write: close . end}\"", "bash"),
                                step("Pattern: the chain",
                                        "A <strong>chain</strong> (linear sequence) means no branching \u2014 a purely sequential protocol. Example: <code>a . b . c . end</code> produces a vertical line of 4 nodes."),
                                step("Pattern: the diamond",
                                        "A <strong>diamond</strong> means a branch with convergence. Example: <code>&amp;{a: end, b: end}</code> produces a 3-node diamond (top \u2192 a/b \u2192 bottom). Two diamonds stacked means nested branching."),
                                step("Pattern: the grid",
                                        "A <strong>grid</strong> (product lattice) comes from the parallel constructor. <code>(a . end || b . end)</code> produces a 2\u00d72 grid: the top-left corner is the initial state, the bottom-right is end, and the other two corners represent \"a done, b pending\" and \"a pending, b done.\""),
                                step("SCC collapsing",
                                        "When a protocol has recursion, the Hasse diagram shows the <strong>quotient</strong>: cyclic states are collapsed into single nodes. This gives a cleaner view of the protocol\u2019s essential structure without the visual clutter of back edges."),
                                step("Debugging with diagrams",
                                        "If a protocol doesn\u2019t behave as expected, generate the Hasse diagram: <ul><li>Too many states? Your branches may be creating unintended combinations.</li><li>Missing edges? Check that method names in the type match your expectations.</li><li>Lattice check fails? The counterexample states are highlighted in the diagram.</li></ul>Use the <a href=\"/tools/analyzer\">web analyzer</a> for instant visualization.")
                        ))
        );
    }
}
