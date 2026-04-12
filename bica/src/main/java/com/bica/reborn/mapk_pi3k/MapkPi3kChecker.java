package com.bica.reborn.mapk_pi3k;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
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
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * MAPK and PI3K/AKT signaling cascades as session types (Step 61) -- Java port
 * of {@code reticulate/reticulate/mapk_pi3k.py}.
 *
 * <p>Models sequential protein phosphorylation cascades (Ras-&gt;Raf-&gt;MEK-&gt;ERK and
 * RTK-&gt;PI3K-&gt;AKT-&gt;mTOR) as recursive session types with phosphatase resets
 * (recursion back-edges), and exposes drug-discovery analyses:
 * <ul>
 *   <li>{@link #pathwayStates}: phosphorylation vector per state-space node</li>
 *   <li>{@link #drugTargetImpact}: pruned reachable states after blocking a node</li>
 *   <li>{@link #computeBottleneck}: ranked single-node cuts</li>
 *   <li>{@link #phiMap} / {@link #psiMap}: bidirectional morphism between
 *       the session-type lattice and the biological phosphorylation lattice</li>
 * </ul>
 */
public final class MapkPi3kChecker {

    private MapkPi3kChecker() {}

    // -----------------------------------------------------------------------
    // Canonical cascade session types
    // -----------------------------------------------------------------------

    public static final String MAPK_CASCADE =
            "rec X . &{"
                    + "phosphorylateRas: &{"
                    + "phosphorylateRaf: &{"
                    + "phosphorylateMEK: &{"
                    + "phosphorylateERK: end, "
                    + "dephosphorylate: X}, "
                    + "dephosphorylate: X}, "
                    + "dephosphorylate: X}, "
                    + "dephosphorylate: X}";

    public static final String PI3K_AKT_AXIS =
            "rec Y . &{"
                    + "phosphorylateRTK: &{"
                    + "phosphorylatePI3K: &{"
                    + "phosphorylateAKT: &{"
                    + "phosphorylateMTOR: end, "
                    + "dephosphorylate: Y}, "
                    + "dephosphorylate: Y}, "
                    + "dephosphorylate: Y}, "
                    + "dephosphorylate: Y}";

    public static final String CROSSTALK =
            "(" + MAPK_CASCADE + " || " + PI3K_AKT_AXIS + ")";

    public static final List<String> MAPK_ROLES = List.of("Ras", "Raf", "MEK", "ERK");
    public static final List<String> PI3K_ROLES = List.of("RTK", "PI3K", "AKT", "MTOR");

    // -----------------------------------------------------------------------
    // PathwayState: phosphorylation vector lattice
    // -----------------------------------------------------------------------

    /**
     * Phosphorylation vector over a fixed role ordering. Partial order is
     * componentwise: {@code s <= t} iff every active protein in {@code s} is
     * also active in {@code t}.
     */
    public record PathwayState(List<String> roles, List<Boolean> active) {

        public PathwayState {
            Objects.requireNonNull(roles, "roles must not be null");
            Objects.requireNonNull(active, "active must not be null");
            if (roles.size() != active.size()) {
                throw new IllegalArgumentException(
                        "roles and active must have equal length");
            }
            roles = List.copyOf(roles);
            active = List.copyOf(active);
        }

        public boolean leq(PathwayState other) {
            if (!this.roles.equals(other.roles)) {
                throw new IllegalArgumentException("states over different roles");
            }
            for (int i = 0; i < active.size(); i++) {
                if (active.get(i) && !other.active.get(i)) return false;
            }
            return true;
        }

        public PathwayState meet(PathwayState other) {
            List<Boolean> out = new ArrayList<>(active.size());
            for (int i = 0; i < active.size(); i++) {
                out.add(active.get(i) && other.active.get(i));
            }
            return new PathwayState(roles, out);
        }

        public PathwayState join(PathwayState other) {
            List<Boolean> out = new ArrayList<>(active.size());
            for (int i = 0; i < active.size(); i++) {
                out.add(active.get(i) || other.active.get(i));
            }
            return new PathwayState(roles, out);
        }

        public Set<String> phosphorylated() {
            Set<String> out = new TreeSet<>();
            for (int i = 0; i < roles.size(); i++) {
                if (active.get(i)) out.add(roles.get(i));
            }
            return Collections.unmodifiableSet(out);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("P[");
            for (boolean a : active) sb.append(a ? '1' : '0');
            sb.append(']');
            return sb.toString();
        }
    }

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    public record DrugTargetImpact(
            String target,
            int reachableBefore,
            int reachableAfter,
            int pruned,
            double efficacy) {
        public DrugTargetImpact {
            Objects.requireNonNull(target, "target must not be null");
        }
    }

    public record BottleneckReport(
            String target,
            double efficacy,
            List<Map.Entry<String, Double>> ranked) {
        public BottleneckReport {
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(ranked, "ranked must not be null");
            ranked = List.copyOf(ranked);
        }
    }

    // -----------------------------------------------------------------------
    // State-space constructors
    // -----------------------------------------------------------------------

    public static StateSpace mapkStateSpace() {
        return buildSS(MAPK_CASCADE);
    }

    public static StateSpace pi3kStateSpace() {
        return buildSS(PI3K_AKT_AXIS);
    }

    public static StateSpace crosstalkStateSpace() {
        return buildSS(CROSSTALK);
    }

    private static StateSpace buildSS(String src) {
        SessionType ast = Parser.parse(src);
        return StateSpaceBuilder.build(ast);
    }

    // -----------------------------------------------------------------------
    // Label / history helpers
    // -----------------------------------------------------------------------

    private static boolean isBackEdge(String label) {
        return label.startsWith("dephosphorylate") || label.startsWith("reset");
    }

    /** Extract the protein activated by a phosphorylation label, or null. */
    static String extractProtein(String label) {
        final String prefix = "phosphorylate";
        if (label.startsWith(prefix)) return label.substring(prefix.length());
        return null;
    }

    /**
     * Monotone label-set history per state along the pathway DAG (back-edges removed).
     */
    private static Map<Integer, Set<String>> labelsOnPath(StateSpace ss) {
        List<Transition> forward = new ArrayList<>();
        for (Transition t : ss.transitions()) {
            if (!isBackEdge(t.label())) forward.add(t);
        }
        Map<Integer, Set<String>> history = new HashMap<>();
        for (int s : ss.states()) history.put(s, new HashSet<>());
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Transition t : forward) {
                Set<String> src = history.get(t.source());
                Set<String> tgt = history.get(t.target());
                if (src == null || tgt == null) continue;
                Set<String> union = new HashSet<>(src);
                union.add(t.label());
                if (!tgt.containsAll(union)) {
                    tgt.addAll(union);
                    changed = true;
                }
            }
        }
        return history;
    }

    // -----------------------------------------------------------------------
    // Pathway states and morphisms
    // -----------------------------------------------------------------------

    /** Project every state-space node into a phosphorylation vector. */
    public static List<PathwayState> pathwayStates(StateSpace ss, List<String> roles) {
        Map<Integer, Set<String>> histories = labelsOnPath(ss);
        int maxId = -1;
        for (int s : ss.states()) if (s > maxId) maxId = s;
        List<PathwayState> out = new ArrayList<>();
        for (int i = 0; i <= maxId; i++) {
            Set<String> h = histories.getOrDefault(i, Collections.emptySet());
            List<Boolean> active = new ArrayList<>(roles.size());
            for (String r : roles) {
                boolean on = false;
                for (String lbl : h) {
                    if (r.equals(extractProtein(lbl))) { on = true; break; }
                }
                active.add(on);
            }
            out.add(new PathwayState(roles, active));
        }
        return out;
    }

    /** phi: L(S) -&gt; PathwayState lattice. */
    public static Map<Integer, PathwayState> phiMap(StateSpace ss, List<String> roles) {
        List<PathwayState> ps = pathwayStates(ss, roles);
        Map<Integer, PathwayState> phi = new TreeMap<>();
        for (int s : new TreeSet<>(ss.states())) {
            if (s < ps.size()) phi.put(s, ps.get(s));
        }
        return phi;
    }

    /** psi: PathwayState lattice -&gt; L(S); smallest state id whose phi dominates p. */
    public static Map<PathwayState, Integer> psiMap(StateSpace ss, List<String> roles) {
        Map<Integer, PathwayState> phi = phiMap(ss, roles);
        Map<PathwayState, Integer> inverse = new LinkedHashMap<>();
        for (PathwayState ps : enumerateVectors(roles)) {
            Integer best = null;
            for (Map.Entry<Integer, PathwayState> e : phi.entrySet()) {
                if (ps.leq(e.getValue())) {
                    if (best == null || e.getKey() < best) best = e.getKey();
                }
            }
            if (best != null) inverse.put(ps, best);
        }
        return inverse;
    }

    static List<PathwayState> enumerateVectors(List<String> roles) {
        int n = roles.size();
        int total = 1 << n;
        List<PathwayState> out = new ArrayList<>(total);
        for (int mask = 0; mask < total; mask++) {
            List<Boolean> bits = new ArrayList<>(n);
            for (int i = 0; i < n; i++) bits.add(((mask >> i) & 1) == 1);
            out.add(new PathwayState(roles, bits));
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Drug target impact / bottleneck
    // -----------------------------------------------------------------------

    /** Reachable-state count from {@code ss.top()}. */
    private static int reachableCount(Set<Integer> states, List<Transition> trans, int top) {
        Set<Integer> seen = new HashSet<>();
        seen.add(top);
        ArrayList<Integer> frontier = new ArrayList<>();
        frontier.add(top);
        while (!frontier.isEmpty()) {
            int s = frontier.remove(frontier.size() - 1);
            for (Transition t : trans) {
                if (t.source() == s && seen.add(t.target())) {
                    frontier.add(t.target());
                }
            }
        }
        return seen.size();
    }

    public static DrugTargetImpact drugTargetImpact(
            StateSpace ss, List<String> roles, String target) {
        if (!roles.contains(target)) {
            throw new IllegalArgumentException("unknown target '" + target + "'");
        }
        int before = reachableCount(ss.states(), ss.transitions(), ss.top());
        String blocked = "phosphorylate" + target;
        List<Transition> pruned = new ArrayList<>();
        for (Transition t : ss.transitions()) {
            if (!t.label().equals(blocked)) pruned.add(t);
        }
        int after = reachableCount(ss.states(), pruned, ss.top());
        int prunedCount = before - after;
        double efficacy = before == 0 ? 0.0 : (double) prunedCount / before;
        efficacy = Math.round(efficacy * 10000.0) / 10000.0;
        return new DrugTargetImpact(target, before, after, prunedCount, efficacy);
    }

    public static BottleneckReport computeBottleneck(StateSpace ss, List<String> roles) {
        List<Map.Entry<String, Double>> ranked = new ArrayList<>();
        for (String r : roles) {
            DrugTargetImpact impact = drugTargetImpact(ss, roles, r);
            ranked.add(Map.entry(r, impact.efficacy()));
        }
        ranked.sort((a, b) -> {
            int c = Double.compare(b.getValue(), a.getValue());
            return c != 0 ? c : a.getKey().compareTo(b.getKey());
        });
        Map.Entry<String, Double> best = ranked.get(0);
        return new BottleneckReport(best.getKey(), best.getValue(), ranked);
    }

    // -----------------------------------------------------------------------
    // Galois connection check
    // -----------------------------------------------------------------------

    /** Verify phi(s) &gt;= p iff s &gt;= psi(p) for all enumerated p and s. */
    public static boolean isGaloisConnectionPhiPsi(StateSpace ss, List<String> roles) {
        Map<Integer, PathwayState> phi = phiMap(ss, roles);
        Map<PathwayState, Integer> psi = psiMap(ss, roles);

        // Forward reachability closure on L(S).
        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int i : ss.states()) {
            Set<Integer> r = new HashSet<>();
            r.add(i);
            reach.put(i, r);
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Transition t : ss.transitions()) {
                Set<Integer> src = reach.get(t.source());
                Set<Integer> tgt = reach.get(t.target());
                if (src == null || tgt == null) continue;
                if (!src.containsAll(tgt)) {
                    src.addAll(tgt);
                    changed = true;
                }
            }
        }

        for (Map.Entry<Integer, PathwayState> eS : phi.entrySet()) {
            int s = eS.getKey();
            PathwayState phis = eS.getValue();
            for (Map.Entry<PathwayState, Integer> eP : psi.entrySet()) {
                boolean lhs = eP.getKey().leq(phis);
                int psp = eP.getValue();
                boolean rhs = reach.get(psp).contains(s);
                if (lhs != rhs) return false;
            }
        }
        return true;
    }
}
