package com.bica.reborn.lattice;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Dogfooding: extract session types from the reticulate/BICA checker modules
 * themselves, build their state spaces, check lattice properties, and compute
 * bidirectional morphisms between the module call-graph poset and the protocol
 * lattice {@code L(S)} (Step 80i).
 *
 * <p>Each checker module exposes a public entry point whose internal control
 * flow follows a protocol:
 * <pre>
 *   parser:     tokenize -&gt; parse_ast -&gt; validate
 *   statespace: collect -&gt; transitions -&gt; close
 *   lattice:    quotient -&gt; reach -&gt; bounds -&gt; pairs
 *   subtyping:  normalise -&gt; coinductive_check
 * </pre>
 *
 * <p>Ported from {@code reticulate/reticulate/dogfooding.py}.
 */
public final class Dogfooding {

    private Dogfooding() {}

    // -- Protocol registry ----------------------------------------------------

    /** A session-type description of a reticulate/BICA checker module. */
    public record CheckerProtocol(
            String moduleName,
            String entryPoint,
            List<String> phases,
            String sessionType,
            List<Edge> edges) {

        public CheckerProtocol {
            phases = List.copyOf(phases);
            edges = List.copyOf(edges);
        }
    }

    /** A call-graph edge {@code (caller, callee)}. */
    public record Edge(String caller, String callee) {}

    /** The canonical set of dogfooded checker protocols. */
    public static final List<CheckerProtocol> CHECKER_PROTOCOLS = List.of(
            new CheckerProtocol(
                    "parser",
                    "parse",
                    List.of("tokenize", "parse_ast", "validate"),
                    "&{tokenize: &{parse_ast: +{ok: &{validate: end}, error: end}}}",
                    List.of(new Edge("tokenize", "parse_ast"),
                            new Edge("parse_ast", "validate"))),
            new CheckerProtocol(
                    "statespace",
                    "build_statespace",
                    List.of("collect", "transitions", "close"),
                    "&{collect: &{transitions: &{close: end}}}",
                    List.of(new Edge("collect", "transitions"),
                            new Edge("transitions", "close"))),
            new CheckerProtocol(
                    "lattice",
                    "check_lattice",
                    List.of("quotient", "reach", "bounds", "pairs"),
                    "&{quotient: &{reach: &{bounds: "
                            + "+{hasBounds: &{pairs: +{ok: end, counterexample: end}},"
                            + " noBounds: end}}}}",
                    List.of(new Edge("quotient", "reach"),
                            new Edge("reach", "bounds"),
                            new Edge("bounds", "pairs"))),
            new CheckerProtocol(
                    "subtyping",
                    "is_subtype",
                    List.of("normalise", "coinductive_check"),
                    "&{normalise: &{coinductive_check: +{yes: end, no: end}}}",
                    List.of(new Edge("normalise", "coinductive_check")))
    );

    // -- Result types ---------------------------------------------------------

    /** Result of dogfooding a single checker. */
    public record DogfoodResult(
            CheckerProtocol protocol,
            StateSpace statespace,
            LatticeResult latticeResult,
            boolean phiValid,
            boolean psiValid,
            boolean bidirectional) {}

    // -- Call-graph poset -----------------------------------------------------

    /**
     * Return reachability closure over the call-graph edges. {@code result.get(p)}
     * is the set of phases reachable from {@code p} (including {@code p}).
     */
    public static Map<String, Set<String>> callGraphReachability(
            List<String> phases, List<Edge> edges) {
        Map<String, Set<String>> reach = new LinkedHashMap<>();
        for (String p : phases) {
            Set<String> s = new HashSet<>();
            s.add(p);
            reach.put(p, s);
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Edge e : edges) {
                Set<String> src = reach.get(e.caller());
                Set<String> tgt = reach.get(e.callee());
                if (src == null || tgt == null) continue;
                int before = src.size();
                src.addAll(tgt);
                if (src.size() > before) changed = true;
            }
        }
        Map<String, Set<String>> frozen = new LinkedHashMap<>();
        for (var entry : reach.entrySet()) {
            frozen.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return frozen;
    }

    /**
     * Return the {@code <=} relation of the call-graph poset
     * ({@code p <= q} iff {@code q} reachable from {@code p}).
     */
    public static BiPredicate<String, String> callGraphLeq(
            List<String> phases, List<Edge> edges) {
        Map<String, Set<String>> reach = callGraphReachability(phases, edges);
        return (p, q) -> {
            Set<String> rp = reach.get(p);
            return rp != null && rp.contains(q);
        };
    }

    // -- Bidirectional morphisms ----------------------------------------------

    /**
     * Map each phase name to the state reached by executing its prefix.
     * We walk transitions from the top state, matching phase names to transition
     * labels. The phase is identified with the decision point (the state before
     * its transition fires).
     */
    public static Map<String, Integer> buildPhi(CheckerProtocol protocol, StateSpace ss) {
        Map<String, Integer> mapping = new LinkedHashMap<>();
        int current = ss.top();
        for (String phase : protocol.phases()) {
            mapping.put(phase, current);
            Integer next = null;
            for (var t : ss.transitions()) {
                if (t.source() == current && t.label().equals(phase)) {
                    next = t.target();
                    break;
                }
            }
            if (next == null) break;
            current = next;
        }
        return mapping;
    }

    /**
     * Inverse map: each state in the image of phi is labelled by its phase.
     * For states not in the image of phi we assign the nearest preceding phase
     * (the phase whose phi-image reaches this state).
     */
    public static Map<Integer, String> buildPsi(
            CheckerProtocol protocol, StateSpace ss, Map<String, Integer> phi) {
        Map<Integer, Set<Integer>> reachable = computeReachability(ss);
        Map<Integer, String> psi = new LinkedHashMap<>();
        // phase_order preserves declaration order (deepest == last inserted).
        List<Map.Entry<String, Integer>> phaseOrder = new ArrayList<>(phi.entrySet());
        for (int state : ss.states()) {
            String best = null;
            for (var entry : phaseOrder) {
                Set<Integer> r = reachable.get(entry.getValue());
                if (r != null && r.contains(state)) {
                    best = entry.getKey();
                }
            }
            if (best != null) psi.put(state, best);
        }
        return psi;
    }

    /** phi is order-preserving if {@code p <=_cg q} implies {@code phi(p) <=_L phi(q)}. */
    public static boolean checkPhiOrderPreserving(
            CheckerProtocol protocol, StateSpace ss, Map<String, Integer> phi) {
        BiPredicate<String, String> leq = callGraphLeq(protocol.phases(), protocol.edges());
        Map<Integer, Set<Integer>> reach = computeReachability(ss);
        for (String p : protocol.phases()) {
            for (String q : protocol.phases()) {
                if (leq.test(p, q) && phi.containsKey(p) && phi.containsKey(q)) {
                    Set<Integer> r = reach.get(phi.get(p));
                    if (r == null || !r.contains(phi.get(q))) return false;
                }
            }
        }
        return true;
    }

    /** psi is order-preserving if {@code s <=_L t} implies {@code psi(s) <=_cg psi(t)}. */
    public static boolean checkPsiOrderPreserving(
            CheckerProtocol protocol, StateSpace ss, Map<Integer, String> psi) {
        BiPredicate<String, String> leq = callGraphLeq(protocol.phases(), protocol.edges());
        Map<Integer, Set<Integer>> reach = computeReachability(ss);
        for (int s : ss.states()) {
            Set<Integer> rs = reach.get(s);
            if (rs == null) continue;
            for (int t : ss.states()) {
                if (rs.contains(t) && psi.containsKey(s) && psi.containsKey(t)) {
                    if (!leq.test(psi.get(s), psi.get(t))) return false;
                }
            }
        }
        return true;
    }

    // -- Top-level driver -----------------------------------------------------

    /** Build state space, check lattice, and verify bidirectional morphisms. */
    public static DogfoodResult dogfoodChecker(CheckerProtocol protocol) {
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(protocol.sessionType()));
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        Map<String, Integer> phi = buildPhi(protocol, ss);
        Map<Integer, String> psi = buildPsi(protocol, ss, phi);
        boolean phiValid = checkPhiOrderPreserving(protocol, ss, phi);
        boolean psiValid = checkPsiOrderPreserving(protocol, ss, psi);
        return new DogfoodResult(protocol, ss, lr, phiValid, psiValid, phiValid && psiValid);
    }

    /** Dogfood every registered checker protocol. */
    public static List<DogfoodResult> dogfoodAll() {
        List<DogfoodResult> out = new ArrayList<>();
        for (CheckerProtocol p : CHECKER_PROTOCOLS) out.add(dogfoodChecker(p));
        return Collections.unmodifiableList(out);
    }

    /** Aggregate statistics: how many checkers pass, how many yield lattices. */
    public static Map<String, Integer> dogfoodSummary() {
        List<DogfoodResult> results = dogfoodAll();
        int lattices = 0, bidir = 0, phiOk = 0, psiOk = 0;
        for (DogfoodResult r : results) {
            if (r.latticeResult().isLattice()) lattices++;
            if (r.bidirectional()) bidir++;
            if (r.phiValid()) phiOk++;
            if (r.psiValid()) psiOk++;
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("total", results.size());
        out.put("lattices", lattices);
        out.put("bidirectional", bidir);
        out.put("phi_ok", phiOk);
        out.put("psi_ok", psiOk);
        return out;
    }

    // -- Helpers --------------------------------------------------------------

    private static Map<Integer, Set<Integer>> computeReachability(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> r = new HashSet<>();
            r.add(s);
            reach.put(s, r);
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (var t : ss.transitions()) {
                Set<Integer> src = reach.get(t.source());
                Set<Integer> tgt = reach.get(t.target());
                if (src == null || tgt == null) continue;
                int before = src.size();
                src.addAll(tgt);
                if (src.size() > before) changed = true;
            }
        }
        return reach;
    }
}
