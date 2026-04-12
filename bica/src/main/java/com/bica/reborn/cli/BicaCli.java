package com.bica.reborn.cli;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.ParseError;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.termination.TerminationChecker;
import com.bica.reborn.termination.TerminationResult;
import com.bica.reborn.termination.WFParallelResult;
import com.bica.reborn.testgen.TestGenConfig;
import com.bica.reborn.testgen.TestGenerator;
import com.bica.reborn.testgen.ViolationStyle;

import java.io.PrintStream;
import java.util.*;

/**
 * CLI entry point for BICA Reborn session type analysis.
 *
 * <p>Parses a session type string, builds its state space, checks termination,
 * WF-Par well-formedness, and lattice properties. Optionally outputs DOT source
 * for Hasse diagram visualisation or generates JUnit 5 protocol conformance tests.
 *
 * <pre>{@code
 * Usage: bica <type-string> [options]
 *
 * Options:
 *   --dot              Print DOT source to stdout
 *   --test-gen         Generate JUnit 5 protocol conformance tests
 *   --call-anyway      Use assertThrows for violations (default: @Disabled)
 *   --class-name NAME  Class under test (required with --test-gen)
 *   --package PKG      Package for generated test class
 *   --var-name NAME    Variable name for object under test (default: obj)
 *   --max-revisits N   Max state revisits in path enumeration (default: 2)
 *   --skip-termination Treat termination failures as warnings
 *   --no-labels        Hide state labels on nodes
 *   --no-edge-labels   Hide transition labels on edges
 *   --title TITLE      Title for the diagram
 *   --help             Show help and exit
 * }</pre>
 */
public final class BicaCli {

    private static final String USAGE = """
            Usage: bica <type-string> [options]

            Positional:
              type-string               Session type to analyze

            Options:
              --dot                     Print DOT source to stdout
              --test-gen                Generate JUnit 5 protocol tests
              --call-anyway             Use assertThrows for violations (default: @Disabled)
              --class-name NAME         Class under test (required with --test-gen)
              --package PKG             Package for generated test class
              --var-name NAME           Variable name (default: obj)
              --max-revisits N          Max state revisits (default: 2)
              --skip-termination        Treat termination failures as warnings
              --no-labels               Hide state labels on nodes
              --no-edge-labels          Hide transition labels on edges
              --title TITLE             Title for the diagram
              --help                    Show help and exit""";

    private static final int MAX_LABEL_LEN = 40;

    private BicaCli() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Testable entry point with injectable output streams.
     *
     * @return exit code (0 = success, 1 = error)
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        // --- Subcommand dispatch ---
        if (args.length > 0 && "scan".equals(args[0])) {
            return ProjectScanner.main(
                    java.util.Arrays.copyOfRange(args, 1, args.length), out, err);
        }

        // --- Argument parsing ---
        String typeString = null;
        boolean dot = false;
        boolean testGen = false;
        boolean callAnyway = false;
        boolean skipTermination = false;
        boolean noLabels = false;
        boolean noEdgeLabels = false;
        String title = null;
        String className = null;
        String packageName = null;
        String varName = "obj";
        int maxRevisits = 2;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help" -> {
                    out.println(USAGE);
                    return 0;
                }
                case "--dot" -> dot = true;
                case "--test-gen" -> testGen = true;
                case "--call-anyway" -> callAnyway = true;
                case "--skip-termination" -> skipTermination = true;
                case "--no-labels" -> noLabels = true;
                case "--no-edge-labels" -> noEdgeLabels = true;
                case "--title" -> {
                    if (i + 1 >= args.length) {
                        err.println("Error: --title requires a value");
                        return 1;
                    }
                    title = args[++i];
                }
                case "--class-name" -> {
                    if (i + 1 >= args.length) {
                        err.println("Error: --class-name requires a value");
                        return 1;
                    }
                    className = args[++i];
                }
                case "--package" -> {
                    if (i + 1 >= args.length) {
                        err.println("Error: --package requires a value");
                        return 1;
                    }
                    packageName = args[++i];
                }
                case "--var-name" -> {
                    if (i + 1 >= args.length) {
                        err.println("Error: --var-name requires a value");
                        return 1;
                    }
                    varName = args[++i];
                }
                case "--max-revisits" -> {
                    if (i + 1 >= args.length) {
                        err.println("Error: --max-revisits requires a value");
                        return 1;
                    }
                    try {
                        maxRevisits = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        err.println("Error: --max-revisits requires an integer");
                        return 1;
                    }
                    if (maxRevisits < 0) {
                        err.println("Error: --max-revisits must be non-negative");
                        return 1;
                    }
                }
                default -> {
                    if (args[i].startsWith("--")) {
                        err.println("Error: unknown option: " + args[i]);
                        return 1;
                    }
                    if (typeString == null) {
                        typeString = args[i];
                    } else {
                        err.println("Error: unexpected argument: " + args[i]);
                        return 1;
                    }
                }
            }
        }

        if (typeString == null) {
            err.println("Error: missing session type string");
            err.println();
            err.println(USAGE);
            return 1;
        }

        if (dot && testGen) {
            err.println("Error: --dot and --test-gen are mutually exclusive");
            return 1;
        }

        if (testGen && className == null) {
            err.println("Error: --class-name is required with --test-gen");
            return 1;
        }

        // --- Pipeline ---

        // 1. Parse
        SessionType ast;
        try {
            ast = Parser.parse(typeString);
        } catch (ParseError e) {
            err.println("Parse error: " + e.getMessage());
            return 1;
        }

        // 2. Build state space
        StateSpace ss;
        try {
            ss = StateSpaceBuilder.build(ast);
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }

        // --- Output ---
        if (testGen) {
            var style = callAnyway ? ViolationStyle.CALL_ANYWAY : ViolationStyle.DISABLED_ANNOTATION;
            var config = new TestGenConfig(className, packageName, varName,
                    maxRevisits, 100, style);
            out.print(TestGenerator.generate(ss, config, PrettyPrinter.pretty(ast)));
        } else if (dot) {
            // 3-5: Full analysis for DOT/summary modes
            LatticeResult latticeResult = LatticeChecker.checkLattice(ss);
            out.println(buildDot(ss, latticeResult, title, !noLabels, !noEdgeLabels));
        } else {
            TerminationResult termResult = TerminationChecker.checkTermination(ast);
            WFParallelResult wfResult = TerminationChecker.checkWfParallel(ast);
            LatticeResult latticeResult = LatticeChecker.checkLattice(ss);
            printSummary(out, ast, ss, termResult, wfResult, latticeResult, skipTermination);
        }

        return 0;
    }

    // =========================================================================
    // Text summary
    // =========================================================================

    private static void printSummary(PrintStream out, SessionType ast, StateSpace ss,
                                     TerminationResult termResult, WFParallelResult wfResult,
                                     LatticeResult latticeResult, boolean skipTermination) {
        out.println("Session type: " + PrettyPrinter.pretty(ast));
        out.println("States: " + ss.states().size()
                + "  |  Transitions: " + ss.transitions().size()
                + "  |  SCCs: " + latticeResult.numScc());
        out.println();

        // Termination
        if (skipTermination && !termResult.isTerminating()) {
            out.println("Termination:    \u26a0 skipped (non-terminating vars: "
                    + termResult.nonTerminatingVars() + ")");
        } else {
            String termCheck = termResult.isTerminating() ? "\u2713" : "\u2717";
            if (termResult.isTerminating()) {
                out.println("Termination:    " + termCheck);
            } else {
                out.println("Termination:    " + termCheck
                        + " (non-terminating vars: " + termResult.nonTerminatingVars() + ")");
            }
        }

        // WF-Par
        if (skipTermination && !wfResult.isWellFormed()) {
            out.println("WF-Par:         \u26a0 skipped");
            for (String error : wfResult.errors()) {
                out.println("  - " + error);
            }
        } else {
            String wfCheck = wfResult.isWellFormed() ? "\u2713" : "\u2717";
            out.println("WF-Par:         " + wfCheck);
            if (!wfResult.isWellFormed()) {
                for (String error : wfResult.errors()) {
                    out.println("  - " + error);
                }
            }
        }
        out.println();

        // Lattice
        String latticeCheck = latticeResult.isLattice() ? "\u2713" : "\u2717";
        String verdict = latticeResult.isLattice() ? "IS a lattice" : "NOT a lattice";
        out.println("Lattice check: " + latticeCheck + " " + verdict);

        String topOk = latticeResult.hasTop() ? "\u2713" : "\u2717";
        String botOk = latticeResult.hasBottom() ? "\u2713" : "\u2717";
        String meetsOk = latticeResult.allMeetsExist() ? "\u2713" : "\u2717";
        String joinsOk = latticeResult.allJoinsExist() ? "\u2713" : "\u2717";

        out.println("  Top reachable:    " + topOk);
        out.println("  Bottom reachable: " + botOk);
        out.println("  All meets exist:  " + meetsOk);
        out.println("  All joins exist:  " + joinsOk);

        if (latticeResult.counterexample() != null) {
            var cx = latticeResult.counterexample();
            String kindStr = "no_meet".equals(cx.kind()) ? "no meet" : "no join";
            out.println("  Counterexample: states " + cx.a() + " and " + cx.b()
                    + " have " + kindStr);
        }
    }

    // =========================================================================
    // DOT generation
    // =========================================================================

    public static String buildDot(StateSpace ss, LatticeResult result, String title,
                                   boolean labels, boolean edgeLabels) {
        var sb = new StringBuilder();
        sb.append("digraph {\n");
        sb.append("    rankdir=TB;\n");
        sb.append("    ranksep=0.7;\n");
        sb.append("    nodesep=0.6;\n");
        sb.append("    ordering=out;\n");
        sb.append("    splines=ortho;\n");
        sb.append("    mclimit=4.0;\n");
        sb.append("    newrank=true;\n");
        sb.append("    node [shape=box, style=\"filled,rounded\", fontname=\"Helvetica\"];\n");
        sb.append("    edge [fontname=\"Helvetica\", fontsize=10];\n");

        if (title != null) {
            sb.append("    label=\"").append(escapeDot(title)).append("\";\n");
            sb.append("    labelloc=t;\n");
            sb.append("    fontsize=14;\n");
        }

        // Determine counterexample states
        int cxA = -1, cxB = -1;
        if (result != null && result.counterexample() != null) {
            cxA = result.counterexample().a();
            cxB = result.counterexample().b();
        }

        // Nodes
        for (int sid : ss.states().stream().sorted().toList()) {
            String nodeLabel = nodeLabel(ss, sid, labels);
            String fillColor;
            if (sid == ss.top()) {
                fillColor = "#bfdbfe";
            } else if (sid == ss.bottom()) {
                fillColor = "#bbf7d0";
            } else {
                fillColor = "#f8fafc";
            }

            sb.append("    ").append(sid).append(" [");
            sb.append("label=\"").append(escapeDot(nodeLabel)).append("\"");
            sb.append(", fillcolor=\"").append(fillColor).append("\"");

            if (sid == cxA || sid == cxB) {
                sb.append(", color=\"red\", penwidth=\"2\"");
            }

            sb.append("];\n");
        }

        // Edges — forward edges vs back-edges (recursion)
        var depth = bfsDepth(ss);
        for (var t : ss.transitions()) {
            int srcDepth = depth.getOrDefault(t.source(), 0);
            int tgtDepth = depth.getOrDefault(t.target(), 0);
            boolean isBackEdge = tgtDepth <= srcDepth && t.target() != ss.bottom();
            sb.append("    ").append(t.source()).append(" -> ").append(t.target());
            sb.append(" [");
            if (edgeLabels) {
                sb.append("xlabel=\"").append(escapeDot(t.label())).append("\"");
            }
            if (isBackEdge) {
                sb.append(edgeLabels ? ", " : "").append("constraint=false, style=dashed");
            }
            sb.append("];\n");
        }

        // Rank groups
        appendRankGroups(sb, ss, depth);

        sb.append("}");
        return sb.toString();
    }

    /**
     * Emits rank subgraphs ensuring:
     * - Top is alone at rank=source (above everything)
     * - Bottom is alone at rank=sink (below everything)
     * - All other nodes grouped by BFS depth with rank=same (siblings side-by-side)
     */
    private static void appendRankGroups(StringBuilder sb, StateSpace ss,
                                          Map<Integer, Integer> depth) {
        // Top always alone at the very top
        sb.append("    {rank=source; ").append(ss.top()).append("}\n");

        // Bottom always alone at the very bottom
        if (ss.bottom() != ss.top()) {
            sb.append("    {rank=sink; ").append(ss.bottom()).append("}\n");
        }

        // Group remaining nodes by BFS depth
        var byDepth = new TreeMap<Integer, List<Integer>>();
        for (var entry : depth.entrySet()) {
            int sid = entry.getKey();
            if (sid == ss.top() || sid == ss.bottom()) continue;
            byDepth.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(sid);
        }
        for (var nodes : byDepth.values()) {
            Collections.sort(nodes);
            sb.append("    {rank=same; ");
            sb.append(String.join("; ", nodes.stream().map(String::valueOf).toList()));
            sb.append("}\n");
        }
    }

    /** BFS from top to compute shortest-path depth for each state. */
    private static Map<Integer, Integer> bfsDepth(StateSpace ss) {
        var depth = new HashMap<Integer, Integer>();
        var queue = new ArrayDeque<Integer>();
        depth.put(ss.top(), 0);
        queue.add(ss.top());
        while (!queue.isEmpty()) {
            int s = queue.poll();
            int d = depth.get(s);
            for (var t : ss.transitions()) {
                if (t.source() == s && !depth.containsKey(t.target())) {
                    depth.put(t.target(), d + 1);
                    queue.add(t.target());
                }
            }
        }
        return depth;
    }

    private static String nodeLabel(StateSpace ss, int sid, boolean labels) {
        String raw;
        if (!labels) {
            raw = String.valueOf(sid);
        } else {
            raw = ss.labels().getOrDefault(sid, String.valueOf(sid));
        }

        if (sid == ss.top()) {
            raw = "\u22a4 " + raw;
        } else if (sid == ss.bottom()) {
            raw = "\u22a5 " + raw;
        }

        return truncate(raw);
    }

    private static String truncate(String label) {
        if (label.length() <= MAX_LABEL_LEN) {
            return label;
        }
        return label.substring(0, MAX_LABEL_LEN) + "\u2026";
    }

    /**
     * Builds DOT source with coverage coloring: covered edges are green, uncovered are gray.
     * Covered states get a green-tinted fill; uncovered non-top/bottom states get gray.
     */
    public static String buildDotWithCoverage(StateSpace ss, LatticeResult result,
                                                Set<StateSpace.Transition> coveredTransitions,
                                                Set<Integer> coveredStates) {
        var sb = new StringBuilder();
        sb.append("digraph {\n");
        sb.append("    rankdir=TB;\n");
        sb.append("    ranksep=0.7;\n");
        sb.append("    nodesep=0.6;\n");
        sb.append("    ordering=out;\n");
        sb.append("    splines=ortho;\n");
        sb.append("    mclimit=4.0;\n");
        sb.append("    newrank=true;\n");
        sb.append("    node [shape=box, style=\"filled,rounded\", fontname=\"Helvetica\"];\n");
        sb.append("    edge [fontname=\"Helvetica\", fontsize=10];\n");

        // Nodes
        for (int sid : ss.states().stream().sorted().toList()) {
            String nodeLabel = nodeLabel(ss, sid, true);
            String fillColor;
            if (sid == ss.top()) {
                fillColor = "#bfdbfe";
            } else if (sid == ss.bottom()) {
                fillColor = "#bbf7d0";
            } else if (coveredStates.contains(sid)) {
                fillColor = "#dcfce7";
            } else {
                fillColor = "#e2e8f0";
            }

            sb.append("    ").append(sid).append(" [");
            sb.append("label=\"").append(escapeDot(nodeLabel)).append("\"");
            sb.append(", fillcolor=\"").append(fillColor).append("\"");
            sb.append("];\n");
        }

        // Edges — separate forward edges from back-edges
        var depth = bfsDepth(ss);
        for (var t : ss.transitions()) {
            int srcDepth = depth.getOrDefault(t.source(), 0);
            int tgtDepth = depth.getOrDefault(t.target(), 0);
            boolean covered = coveredTransitions.contains(t);
            String color = covered ? "#16a34a" : "#94a3b8";
            String penwidth = covered ? "2.0" : "1.0";
            boolean isBackEdge = tgtDepth <= srcDepth && t.target() != ss.bottom();

            sb.append("    ").append(t.source()).append(" -> ").append(t.target());
            sb.append(" [xlabel=\"").append(escapeDot(t.label())).append("\"");
            sb.append(", color=\"").append(color).append("\"");
            sb.append(", penwidth=\"").append(penwidth).append("\"");
            sb.append(", fontcolor=\"").append(color).append("\"");
            if (isBackEdge) {
                sb.append(", constraint=false, style=dashed");
            }
            sb.append("];\n");
        }

        // Rank groups
        appendRankGroups(sb, ss, depth);

        sb.append("}");
        return sb.toString();
    }

    private static String escapeDot(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
