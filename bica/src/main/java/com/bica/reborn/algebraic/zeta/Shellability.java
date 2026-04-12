package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;

/**
 * Shellability and EL-labeling for session type lattices (Step 30w).
 *
 * <p>Faithful Java port of Python {@code reticulate.shellability} (46 tests).
 *
 * <p>A finite lattice is <b>shellable</b> if its order complex (the simplicial
 * complex of all chains) can be assembled face-by-face such that each new face
 * attaches along a topologically nice boundary. Shellable lattices have
 * particularly clean homological structure: the homology is torsion-free and
 * concentrated in a single degree.
 *
 * <p>The primary tool for proving shellability is <b>EL-labeling</b>
 * (edge-lexicographic labeling), introduced by Björner. An EL-labeling of a
 * bounded poset P assigns a label λ(x, y) to each edge x ⋗ y of the Hasse
 * diagram such that in every interval [a, b]:
 * <ol>
 *   <li>There is a <b>unique increasing maximal chain</b> (labels strictly
 *       increase).</li>
 *   <li>This chain is <b>lexicographically first</b> among all maximal chains.</li>
 * </ol>
 *
 * <p>For session type lattices, the transition labels (method names) provide a
 * natural candidate for the EL-labeling. We investigate when this labeling is
 * an EL-labeling and what it implies for the topological structure of the
 * protocol.
 *
 * <p>NOTE: this port mirrors the semantics of the Python module exactly; the
 * f-vector here is indexed from {@code f_0 = |states|}, NOT from {@code f_{-1}}
 * like {@link OrderComplex#fVector}.
 */
public final class Shellability {

    private Shellability() {}

    // =======================================================================
    // Result types
    // =======================================================================

    /** A labeled edge in the Hasse diagram. */
    public record HasseEdge(int source, int target, String label) {}

    /** A maximal chain with its edge labels. */
    public record LabeledChain(
            List<Integer> states,
            List<String> labels,
            List<Integer> descents,
            boolean isIncreasing) {}

    /** Result of EL-labeling verification. */
    public record ELResult(
            boolean isElLabeling,
            int numIntervals,
            int intervalsWithUniqueIncreasing,
            int[] failingInterval,
            String failingReason) {}

    /** Result of CL-labeling (chain-edge labeling) verification. */
    public record CLResult(
            boolean isClLabeling,
            int numIntervals,
            int[] failingInterval) {}

    /** Result of shellability analysis. */
    public record ShellabilityResult(
            boolean isShellable,
            boolean isElShellable,
            boolean isClShellable,
            boolean isGraded,
            ELResult elResult,
            CLResult clResult,
            int numMaximalChains,
            int height,
            List<Integer> fVector,
            List<Integer> hVector,
            int numDescents) {}

    // =======================================================================
    // Internal: Hasse diagram extraction
    // =======================================================================

    private static Map<Integer, Set<Integer>> buildReachability(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (Transition t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new HashSet<>()).add(t.target());
        }

        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int v = stack.pop();
                if (!visited.add(v)) continue;
                for (int u : adj.getOrDefault(v, Set.of())) {
                    stack.push(u);
                }
            }
            reach.put(s, visited);
        }
        return reach;
    }

    private static final class Cover {
        final int source;
        final int target;
        final String label;
        Cover(int s, int t, String l) { source = s; target = t; label = l; }
    }

    private static List<Cover> buildCovering(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = buildReachability(ss);

        Map<Integer, List<int[]>> adjIdx = new HashMap<>(); // state -> list of [targetIdx, labelIdx] not used
        // adjacency: state -> list of (target, label)
        Map<Integer, List<Object[]>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new ArrayList<>());
        for (Transition t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new ArrayList<>())
               .add(new Object[] {t.target(), t.label()});
        }

        List<Cover> covers = new ArrayList<>();
        for (int s : ss.states()) {
            List<Object[]> sAdj = adj.getOrDefault(s, List.of());
            for (Object[] pair : sAdj) {
                int t = (int) pair[0];
                String label = (String) pair[1];
                boolean isCover = true;
                for (Object[] other : sAdj) {
                    int u = (int) other[0];
                    if (u != t && u != s && reach.getOrDefault(u, Set.of()).contains(t)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) covers.add(new Cover(s, t, label));
            }
        }
        return covers;
    }

    // =======================================================================
    // Public API: Hasse diagram
    // =======================================================================

    /** Extract the labeled Hasse diagram of the state space. */
    public static List<HasseEdge> hasseLabels(StateSpace ss) {
        List<Cover> covers = buildCovering(ss);
        List<HasseEdge> edges = new ArrayList<>(covers.size());
        for (Cover c : covers) edges.add(new HasseEdge(c.source, c.target, c.label));
        return edges;
    }

    // =======================================================================
    // Public API: Chain enumeration
    // =======================================================================

    /** Enumerate all maximal chains from top to bottom (default limit 10000). */
    public static List<LabeledChain> maximalChains(StateSpace ss) {
        return maximalChains(ss, 10000);
    }

    /** Enumerate all maximal chains from top to bottom. */
    public static List<LabeledChain> maximalChains(StateSpace ss, int maxChains) {
        List<Cover> covers = buildCovering(ss);
        Map<Integer, List<Object[]>> covAdj = new HashMap<>();
        for (int s : ss.states()) covAdj.put(s, new ArrayList<>());
        for (Cover c : covers) {
            covAdj.computeIfAbsent(c.source, k -> new ArrayList<>())
                  .add(new Object[] {c.target, c.label});
        }

        List<LabeledChain> chains = new ArrayList<>();

        // Stack frames: (state, path, labels, visited)
        final class Frame {
            final int state;
            final List<Integer> path;
            final List<String> labels;
            final Set<Integer> visited;
            Frame(int s, List<Integer> p, List<String> l, Set<Integer> v) {
                state = s; path = p; labels = l; visited = v;
            }
        }

        Deque<Frame> stack = new ArrayDeque<>();
        Set<Integer> initVis = new HashSet<>();
        initVis.add(ss.top());
        List<Integer> initPath = new ArrayList<>();
        initPath.add(ss.top());
        stack.push(new Frame(ss.top(), initPath, new ArrayList<>(), initVis));

        while (!stack.isEmpty() && chains.size() < maxChains) {
            Frame f = stack.pop();
            if (f.state == ss.bottom()) {
                List<Integer> desc = new ArrayList<>();
                for (int i = 0; i < f.labels.size() - 1; i++) {
                    if (f.labels.get(i).compareTo(f.labels.get(i + 1)) > 0) desc.add(i);
                }
                chains.add(new LabeledChain(
                        List.copyOf(f.path),
                        List.copyOf(f.labels),
                        List.copyOf(desc),
                        desc.isEmpty()));
                continue;
            }
            // Sort by label descending so lex-smallest is popped first (matches Python)
            List<Object[]> next = new ArrayList<>(covAdj.getOrDefault(f.state, List.of()));
            next.sort((a, b) -> ((String) b[1]).compareTo((String) a[1]));
            for (Object[] pair : next) {
                int t = (int) pair[0];
                String l = (String) pair[1];
                if (!f.visited.contains(t)) {
                    List<Integer> newPath = new ArrayList<>(f.path);
                    newPath.add(t);
                    List<String> newLabels = new ArrayList<>(f.labels);
                    newLabels.add(l);
                    Set<Integer> newVis = new HashSet<>(f.visited);
                    newVis.add(t);
                    stack.push(new Frame(t, newPath, newLabels, newVis));
                }
            }
        }
        return chains;
    }

    // =======================================================================
    // Public API: Descent analysis
    // =======================================================================

    /** Descent set of a labeled chain. */
    public static Set<Integer> descentSet(LabeledChain chain) {
        return new HashSet<>(chain.descents());
    }

    /** Count chains by number of descents. */
    public static Map<Integer, Integer> descentStatistic(List<LabeledChain> chains) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (LabeledChain c : chains) {
            int n = c.descents().size();
            counts.merge(n, 1, Integer::sum);
        }
        return counts;
    }

    // =======================================================================
    // Public API: EL-labeling check
    // =======================================================================

    /** Check whether the transition labels form an EL-labeling. */
    public static ELResult checkElLabeling(StateSpace ss) {
        List<Cover> covers = buildCovering(ss);
        Map<Integer, Set<Integer>> reach = buildReachability(ss);

        Map<Integer, List<Object[]>> covAdj = new HashMap<>();
        for (int s : ss.states()) covAdj.put(s, new ArrayList<>());
        for (Cover c : covers) {
            covAdj.computeIfAbsent(c.source, k -> new ArrayList<>())
                  .add(new Object[] {c.target, c.label});
        }

        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);

        int numIntervals = 0;
        int numOk = 0;
        int[] failingInterval = null;
        String failingReason = null;

        for (int a : states) {
            for (int b : states) {
                if (a == b) continue;
                if (!reach.getOrDefault(a, Set.of()).contains(b)) continue;
                numIntervals++;

                // Enumerate maximal chains from a to b (bounded)
                List<List<String>> intervalLabels = new ArrayList<>();

                final class Frame {
                    final int state;
                    final List<String> lbs;
                    final Set<Integer> vis;
                    Frame(int s, List<String> l, Set<Integer> v) {
                        state = s; lbs = l; vis = v;
                    }
                }

                Deque<Frame> st = new ArrayDeque<>();
                Set<Integer> initV = new HashSet<>();
                initV.add(a);
                st.push(new Frame(a, new ArrayList<>(), initV));

                while (!st.isEmpty() && intervalLabels.size() <= 100) {
                    Frame f = st.pop();
                    if (f.state == b) {
                        intervalLabels.add(List.copyOf(f.lbs));
                        continue;
                    }
                    List<Object[]> next = new ArrayList<>(covAdj.getOrDefault(f.state, List.of()));
                    next.sort((x, y) -> ((String) y[1]).compareTo((String) x[1]));
                    for (Object[] pair : next) {
                        int tt = (int) pair[0];
                        String ll = (String) pair[1];
                        if (!f.vis.contains(tt) && reach.getOrDefault(tt, Set.of()).contains(b)) {
                            List<String> newLbs = new ArrayList<>(f.lbs);
                            newLbs.add(ll);
                            Set<Integer> newV = new HashSet<>(f.vis);
                            newV.add(tt);
                            st.push(new Frame(tt, newLbs, newV));
                        }
                    }
                }

                if (intervalLabels.isEmpty()) continue;

                List<List<String>> increasing = new ArrayList<>();
                for (List<String> labs : intervalLabels) {
                    boolean inc = true;
                    for (int i = 0; i < labs.size() - 1; i++) {
                        if (labs.get(i).compareTo(labs.get(i + 1)) >= 0) { inc = false; break; }
                    }
                    if (inc) increasing.add(labs);
                }

                if (increasing.size() != 1) {
                    if (failingInterval == null) {
                        failingInterval = new int[] {a, b};
                        if (increasing.isEmpty()) {
                            failingReason = "No increasing chain in interval [" + a + ", " + b + "]";
                        } else {
                            failingReason = increasing.size()
                                    + " increasing chains in [" + a + ", " + b + "]";
                        }
                    }
                    continue;
                }

                List<String> incLabs = increasing.get(0);
                List<List<String>> sorted = new ArrayList<>(intervalLabels);
                sorted.sort(Shellability::compareLabelLists);
                if (!sorted.get(0).equals(incLabs)) {
                    if (failingInterval == null) {
                        failingInterval = new int[] {a, b};
                        failingReason = "Increasing chain " + incLabs
                                + " is not lex-first in interval [" + a + ", " + b
                                + "]; lex-first is " + sorted.get(0);
                    }
                    continue;
                }

                numOk++;
            }
        }

        boolean isEl = failingInterval == null && numIntervals > 0;
        return new ELResult(isEl, numIntervals, numOk, failingInterval, failingReason);
    }

    private static int compareLabelLists(List<String> a, List<String> b) {
        int n = Math.min(a.size(), b.size());
        for (int i = 0; i < n; i++) {
            int c = a.get(i).compareTo(b.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(a.size(), b.size());
    }

    /** Check whether the labeling is a CL-labeling (rooted at top). */
    public static CLResult checkClLabeling(StateSpace ss) {
        List<Cover> covers = buildCovering(ss);
        Map<Integer, Set<Integer>> reach = buildReachability(ss);

        Map<Integer, List<Object[]>> covAdj = new HashMap<>();
        for (int s : ss.states()) covAdj.put(s, new ArrayList<>());
        for (Cover c : covers) {
            covAdj.computeIfAbsent(c.source, k -> new ArrayList<>())
                  .add(new Object[] {c.target, c.label});
        }

        int numIntervals = 0;
        int[] failingInterval = null;

        List<Integer> sortedStates = new ArrayList<>(ss.states());
        Collections.sort(sortedStates);

        for (int b : sortedStates) {
            if (b == ss.top()) continue;
            if (!reach.getOrDefault(ss.top(), Set.of()).contains(b)) continue;
            numIntervals++;

            List<List<String>> intervalLabels = new ArrayList<>();

            final class Frame {
                final int state;
                final List<String> lbs;
                final Set<Integer> vis;
                Frame(int s, List<String> l, Set<Integer> v) {
                    state = s; lbs = l; vis = v;
                }
            }

            Deque<Frame> stk = new ArrayDeque<>();
            Set<Integer> initV = new HashSet<>();
            initV.add(ss.top());
            stk.push(new Frame(ss.top(), new ArrayList<>(), initV));

            while (!stk.isEmpty() && intervalLabels.size() <= 100) {
                Frame f = stk.pop();
                if (f.state == b) {
                    intervalLabels.add(List.copyOf(f.lbs));
                    continue;
                }
                List<Object[]> next = new ArrayList<>(covAdj.getOrDefault(f.state, List.of()));
                next.sort((x, y) -> ((String) y[1]).compareTo((String) x[1]));
                for (Object[] pair : next) {
                    int tt = (int) pair[0];
                    String ll = (String) pair[1];
                    if (!f.vis.contains(tt)
                            && (reach.getOrDefault(tt, Set.of()).contains(b) || tt == b)) {
                        List<String> newLbs = new ArrayList<>(f.lbs);
                        newLbs.add(ll);
                        Set<Integer> newV = new HashSet<>(f.vis);
                        newV.add(tt);
                        stk.push(new Frame(tt, newLbs, newV));
                    }
                }
            }

            if (intervalLabels.isEmpty()) continue;

            int nInc = 0;
            for (List<String> labs : intervalLabels) {
                boolean inc = true;
                for (int i = 0; i < labs.size() - 1; i++) {
                    if (labs.get(i).compareTo(labs.get(i + 1)) >= 0) { inc = false; break; }
                }
                if (inc) nInc++;
            }

            if (nInc != 1) {
                if (failingInterval == null) {
                    failingInterval = new int[] {ss.top(), b};
                }
            }
        }

        boolean isCl = failingInterval == null && numIntervals > 0;
        return new CLResult(isCl, numIntervals, failingInterval);
    }

    // =======================================================================
    // Public API: f-vector and h-vector
    // =======================================================================

    /**
     * Compute the f-vector of the order complex.
     *
     * <p>Indexed from {@code f_0 = |states|} (matches Python
     * {@code reticulate.shellability.compute_f_vector}).
     */
    public static List<Integer> computeFVector(StateSpace ss) {
        List<Cover> covers = buildCovering(ss);
        Map<Integer, List<Object[]>> covAdj = new HashMap<>();
        for (int s : ss.states()) covAdj.put(s, new ArrayList<>());
        for (Cover c : covers) {
            covAdj.computeIfAbsent(c.source, k -> new ArrayList<>())
                  .add(new Object[] {c.target, c.label});
        }

        Map<Integer, Integer> chainCounts = new HashMap<>();
        chainCounts.put(0, ss.states().size());

        for (int start : ss.states()) {
            final class Frame {
                final int state;
                final int length;
                final Set<Integer> visited;
                Frame(int s, int l, Set<Integer> v) { state = s; length = l; visited = v; }
            }
            Deque<Frame> stack = new ArrayDeque<>();
            Set<Integer> initV = new HashSet<>();
            initV.add(start);
            stack.push(new Frame(start, 1, initV));
            while (!stack.isEmpty()) {
                Frame f = stack.pop();
                for (Object[] pair : covAdj.getOrDefault(f.state, List.of())) {
                    int t = (int) pair[0];
                    if (!f.visited.contains(t)) {
                        chainCounts.merge(f.length, 1, Integer::sum);
                        Set<Integer> nv = new HashSet<>(f.visited);
                        nv.add(t);
                        stack.push(new Frame(t, f.length + 1, nv));
                    }
                }
            }
        }

        int maxDim = chainCounts.isEmpty() ? 0 : Collections.max(chainCounts.keySet());
        List<Integer> f = new ArrayList<>(maxDim + 1);
        for (int i = 0; i <= maxDim; i++) f.add(chainCounts.getOrDefault(i, 0));
        return f;
    }

    /** Compute the h-vector from the f-vector. */
    public static List<Integer> computeHVector(StateSpace ss) {
        List<Integer> f = computeFVector(ss);
        int d = f.size() - 1;
        if (d < 0) return List.of(1);

        List<Integer> h = new ArrayList<>();
        for (int k = 0; k <= d; k++) {
            int val = 0;
            for (int i = 0; i <= k; i++) {
                int sign = ((k - i) % 2 == 0) ? 1 : -1;
                int fi = (i < f.size()) ? f.get(i) : 0;
                val += sign * binomial(d - i, k - i) * fi;
            }
            h.add(val);
        }
        return h;
    }

    private static int binomial(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        int kk = Math.min(k, n - k);
        long result = 1;
        for (int i = 0; i < kk; i++) {
            result = result * (n - i) / (i + 1);
        }
        return (int) result;
    }

    // =======================================================================
    // Public API: Shellability check
    // =======================================================================

    /** Full shellability analysis of a session type state space. */
    public static ShellabilityResult checkShellability(StateSpace ss) {
        List<LabeledChain> chains = maximalChains(ss);
        int numChains = chains.size();

        int height = 0;
        for (LabeledChain c : chains) height = Math.max(height, c.states().size() - 1);

        Set<Integer> chainLengths = new HashSet<>();
        for (LabeledChain c : chains) chainLengths.add(c.states().size());
        boolean isGraded = chainLengths.size() <= 1;

        ELResult el = checkElLabeling(ss);
        CLResult cl = checkClLabeling(ss);

        List<Integer> fVec = computeFVector(ss);
        List<Integer> hVec = computeHVector(ss);

        boolean isElShellable = el.isElLabeling();
        boolean isClShellable = cl.isClLabeling();
        boolean isShellable = isElShellable || isClShellable;

        int totalDescents = 0;
        for (LabeledChain c : chains) totalDescents += c.descents().size();

        return new ShellabilityResult(
                isShellable, isElShellable, isClShellable, isGraded,
                el, cl, numChains, height, fVec, hVec, totalDescents);
    }

    /** Alias for {@link #checkShellability}. */
    public static ShellabilityResult analyzeShellability(StateSpace ss) {
        return checkShellability(ss);
    }

    /** Quick check: is the order complex shellable? */
    public static boolean isShellable(StateSpace ss) {
        return checkShellability(ss).isShellable();
    }

    /** Quick check: do the transition labels form an EL-labeling? */
    public static boolean isElLabelable(StateSpace ss) {
        return checkElLabeling(ss).isElLabeling();
    }
}
