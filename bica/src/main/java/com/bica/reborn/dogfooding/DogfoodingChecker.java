package com.bica.reborn.dogfooding;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
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
import java.util.Objects;
import java.util.Set;

/**
 * Dogfooding: model BICA Reborn's own checker modules (parser, statespace,
 * lattice, subtyping) as session-type protocols (Step 80i). Java port of
 * {@code reticulate/reticulate/dogfooding.py}.
 *
 * <p>Each BICA checker module has a public entry point whose internal control
 * flow follows a protocol. Each such protocol is encoded here as a session
 * type, its state space is built, its lattice structure is verified, and
 * bidirectional morphisms between the module call graph (phases ordered by
 * happens-before) and the protocol lattice L(S) are computed.
 *
 * <p>Pure Java; uses BICA's {@link Parser}, {@link StateSpaceBuilder},
 * {@link LatticeChecker}.
 */
public final class DogfoodingChecker {

    private DogfoodingChecker() {}

    // -----------------------------------------------------------------------
    // Checker protocol registry
    // -----------------------------------------------------------------------

    /** A session-type description of a BICA checker module. */
    public record CheckerProtocol(
            String moduleName,
            String entryPoint,
            List<String> phases,
            String sessionType,
            List<String[]> edges) {

        public CheckerProtocol {
            Objects.requireNonNull(moduleName);
            Objects.requireNonNull(entryPoint);
            Objects.requireNonNull(sessionType);
            phases = List.copyOf(phases);
            List<String[]> copy = new ArrayList<>(edges.size());
            for (String[] e : edges) copy.add(new String[] {e[0], e[1]});
            edges = List.copyOf(copy);
        }
    }

    /** The canonical set of dogfooded BICA checker protocols. */
    public static final List<CheckerProtocol> CHECKER_PROTOCOLS = List.of(
            new CheckerProtocol(
                    "parser",
                    "parse",
                    List.of("tokenize", "parse_ast", "validate"),
                    "&{tokenize: &{parse_ast: +{ok: &{validate: end}, error: end}}}",
                    List.of(new String[] {"tokenize", "parse_ast"},
                            new String[] {"parse_ast", "validate"})),
            new CheckerProtocol(
                    "statespace",
                    "build",
                    List.of("collect", "transitions", "close"),
                    "&{collect: &{transitions: &{close: end}}}",
                    List.of(new String[] {"collect", "transitions"},
                            new String[] {"transitions", "close"})),
            new CheckerProtocol(
                    "lattice",
                    "checkLattice",
                    List.of("quotient", "reach", "bounds", "pairs"),
                    "&{quotient: &{reach: &{bounds: "
                            + "+{hasBounds: &{pairs: +{ok: end, counterexample: end}},"
                            + " noBounds: end}}}}",
                    List.of(new String[] {"quotient", "reach"},
                            new String[] {"reach", "bounds"},
                            new String[] {"bounds", "pairs"})),
            new CheckerProtocol(
                    "subtyping",
                    "isSubtype",
                    List.of("normalise", "coinductive_check"),
                    "&{normalise: &{coinductive_check: +{yes: end, no: end}}}",
                    List.<String[]>of(new String[] {"normalise", "coinductive_check"})));

    // -----------------------------------------------------------------------
    // Result
    // -----------------------------------------------------------------------

    /** Result of dogfooding a single BICA checker. */
    public record DogfoodResult(
            CheckerProtocol protocol,
            StateSpace statespace,
            LatticeResult latticeResult,
            Map<String, Integer> phi,
            Map<Integer, String> psi,
            boolean phiValid,
            boolean psiValid,
            boolean bidirectional) {

        public DogfoodResult {
            phi = Map.copyOf(phi);
            psi = Map.copyOf(psi);
        }
    }

    // -----------------------------------------------------------------------
    // Call-graph poset
    // -----------------------------------------------------------------------

    /** Reachability closure over the call-graph edges. */
    public static Map<String, Set<String>> callGraphReachability(
            List<String> phases, List<String[]> edges) {
        Map<String, Set<String>> reach = new LinkedHashMap<>();
        for (String p : phases) {
            Set<String> s = new HashSet<>();
            s.add(p);
            reach.put(p, s);
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String[] e : edges) {
                Set<String> src = reach.get(e[0]);
                Set<String> tgt = reach.get(e[1]);
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

    /** Returns the leq relation of the call-graph poset (p leq q iff q reachable from p). */
    public static java.util.function.BiPredicate<String, String> callGraphLeq(
            List<String> phases, List<String[]> edges) {
        Map<String, Set<String>> reach = callGraphReachability(phases, edges);
        return (p, q) -> {
            Set<String> s = reach.get(p);
            return s != null && s.contains(q);
        };
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphisms
    // -----------------------------------------------------------------------

    /** Map each phase name to the state reached by executing its prefix. */
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

    /** Inverse map: each state in the image of phi is labelled by its phase. */
    public static Map<Integer, String> buildPsi(
            CheckerProtocol protocol, StateSpace ss, Map<String, Integer> phi) {
        Map<Integer, Set<Integer>> reach = stateReachability(ss);
        List<Map.Entry<String, Integer>> phaseOrder = new ArrayList<>(phi.entrySet());
        Map<Integer, String> psi = new LinkedHashMap<>();
        for (int state : ss.states()) {
            String best = null;
            for (var entry : phaseOrder) {
                int ps = entry.getValue();
                Set<Integer> fromPs = reach.get(ps);
                if (fromPs != null && fromPs.contains(state)) {
                    best = entry.getKey();
                }
            }
            if (best != null) psi.put(state, best);
        }
        return psi;
    }

    private static Map<Integer, Set<Integer>> stateReachability(StateSpace ss) {
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

    /** phi is order-preserving if p leq_cg q implies phi(p) leq_L phi(q). */
    public static boolean checkPhiOrderPreserving(
            CheckerProtocol protocol, StateSpace ss, Map<String, Integer> phi) {
        var leq = callGraphLeq(protocol.phases(), protocol.edges());
        Map<Integer, Set<Integer>> reach = stateReachability(ss);
        for (String p : protocol.phases()) {
            for (String q : protocol.phases()) {
                if (leq.test(p, q) && phi.containsKey(p) && phi.containsKey(q)) {
                    Set<Integer> fromP = reach.get(phi.get(p));
                    if (fromP == null || !fromP.contains(phi.get(q))) return false;
                }
            }
        }
        return true;
    }

    /** psi is order-preserving if s leq_L t implies psi(s) leq_cg psi(t). */
    public static boolean checkPsiOrderPreserving(
            CheckerProtocol protocol, StateSpace ss, Map<Integer, String> psi) {
        var leq = callGraphLeq(protocol.phases(), protocol.edges());
        Map<Integer, Set<Integer>> reach = stateReachability(ss);
        for (int s : ss.states()) {
            for (int t : ss.states()) {
                Set<Integer> fromS = reach.get(s);
                if (fromS != null && fromS.contains(t)
                        && psi.containsKey(s) && psi.containsKey(t)) {
                    if (!leq.test(psi.get(s), psi.get(t))) return false;
                }
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Top-level dogfood driver
    // -----------------------------------------------------------------------

    /** Build state space, check lattice, verify bidirectional morphisms. */
    public static DogfoodResult dogfoodChecker(CheckerProtocol protocol) {
        SessionType ast = Parser.parse(protocol.sessionType());
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        Map<String, Integer> phi = buildPhi(protocol, ss);
        Map<Integer, String> psi = buildPsi(protocol, ss, phi);
        boolean phiValid = checkPhiOrderPreserving(protocol, ss, phi);
        boolean psiValid = checkPsiOrderPreserving(protocol, ss, psi);
        return new DogfoodResult(protocol, ss, lr, phi, psi,
                phiValid, psiValid, phiValid && psiValid);
    }

    /** Dogfood every registered checker protocol. */
    public static List<DogfoodResult> dogfoodAll() {
        List<DogfoodResult> out = new ArrayList<>();
        for (CheckerProtocol p : CHECKER_PROTOCOLS) out.add(dogfoodChecker(p));
        return Collections.unmodifiableList(out);
    }

    /** Aggregate statistics across all checkers. */
    public static Map<String, Integer> dogfoodSummary() {
        List<DogfoodResult> results = dogfoodAll();
        int lattices = 0, bidirectional = 0, phiOk = 0, psiOk = 0;
        for (DogfoodResult r : results) {
            if (r.latticeResult().isLattice()) lattices++;
            if (r.bidirectional()) bidirectional++;
            if (r.phiValid()) phiOk++;
            if (r.psiValid()) psiOk++;
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("total", results.size());
        out.put("lattices", lattices);
        out.put("bidirectional", bidirectional);
        out.put("phi_ok", phiOk);
        out.put("psi_ok", psiOk);
        return Collections.unmodifiableMap(out);
    }
}
