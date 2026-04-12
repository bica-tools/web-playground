package com.bica.reborn.ect_fingerprint;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Euler Characteristic Transform (ECT) fingerprinting for session type
 * lattices (Step 32i) -- Java port of {@code reticulate/reticulate/ect_fingerprint.py}.
 *
 * <p>The ECT records the Euler characteristic of each sublevel set of a
 * rank-based filtration on the session type lattice {@code L(S)}. For each
 * level {@code k} the induced sub-poset is built and the Euler characteristic
 * of its order complex is computed (alternating sum of chain counts). The
 * sequence
 * <pre>
 *     ECT(S) = (chi(L_0), chi(L_1), ..., chi(L_K))
 * </pre>
 * is the ECT fingerprint of the protocol.
 *
 * <p>BICA convention: {@code ss.top()} is the initial protocol state and
 * {@code ss.bottom()} is the terminal state. The rank function used here is
 * the length of the longest forward chain starting at {@code top}, mirroring
 * the Python {@code compute_rank} which measures depth from the initial state.
 */
public final class EctFingerprintChecker {

    private EctFingerprintChecker() {}

    // -----------------------------------------------------------------------
    // Result types
    // -----------------------------------------------------------------------

    /**
     * Euler characteristic transform fingerprint of a session type lattice.
     *
     * @param chiSequence (chi(L_0), chi(L_1), ..., chi(L_K))
     * @param rankMax     maximum rank value K
     * @param fVectors    face vectors of the order complex at each sublevel
     * @param levelSizes  number of states (SCC reps) at each sublevel
     */
    public record ECTFingerprint(
            List<Integer> chiSequence,
            int rankMax,
            List<List<Integer>> fVectors,
            List<Integer> levelSizes) {

        public ECTFingerprint {
            Objects.requireNonNull(chiSequence, "chiSequence must not be null");
            Objects.requireNonNull(fVectors, "fVectors must not be null");
            Objects.requireNonNull(levelSizes, "levelSizes must not be null");
            chiSequence = List.copyOf(chiSequence);
            List<List<Integer>> copy = new ArrayList<>(fVectors.size());
            for (List<Integer> fv : fVectors) copy.add(List.copyOf(fv));
            fVectors = List.copyOf(copy);
            levelSizes = List.copyOf(levelSizes);
        }

        /** L1 distance between fingerprints (zero-padded to equal length). */
        public int distance(ECTFingerprint other) {
            int n = Math.max(chiSequence.size(), other.chiSequence.size());
            int total = 0;
            for (int i = 0; i < n; i++) {
                int x = i < chiSequence.size() ? chiSequence.get(i) : 0;
                int y = i < other.chiSequence.size() ? other.chiSequence.get(i) : 0;
                total += Math.abs(x - y);
            }
            return total;
        }

        /** Length of the chi sequence. */
        public int length() {
            return chiSequence.size();
        }
    }

    /**
     * Result of comparing two ECT fingerprints.
     *
     * @param left            left operand
     * @param right           right operand
     * @param equal           true iff fingerprints agree
     * @param l1Distance      L1 distance between chi sequences
     * @param firstDivergence index of first differing level, -1 if equal
     */
    public record ECTComparison(
            ECTFingerprint left,
            ECTFingerprint right,
            boolean equal,
            int l1Distance,
            int firstDivergence) {}

    /** Classification of the bidirectional ECT morphism pair (phi, psi). */
    public record MorphismClassification(
            boolean phiSurjectiveOntoImage,
            boolean phiOrderPreserving,
            boolean psiRightAdjoint,
            boolean galoisConnection,
            String classification) {}

    /** Recovered psi-invariants of a fingerprint. */
    public record PsiInvariants(int totalChi, int rank, int topChi, int maxLevelSize) {}

    // -----------------------------------------------------------------------
    // Internal: SCC + reachability + rank
    // -----------------------------------------------------------------------

    private record QuotientData(
            Map<Integer, Integer> sccMap,
            Map<Integer, Set<Integer>> reach,
            List<Integer> reps) {}

    /** Iterative Tarjan SCC. Representative = smallest state ID in each SCC. */
    private static Map<Integer, Integer> computeSccs(StateSpace ss) {
        Map<Integer, List<Integer>> adj = adjacency(ss);
        Map<Integer, Integer> index = new HashMap<>();
        Map<Integer, Integer> lowlink = new HashMap<>();
        Map<Integer, Boolean> onStack = new HashMap<>();
        Deque<Integer> stack = new ArrayDeque<>();
        int[] counter = {0};
        Map<Integer, Integer> sccMap = new HashMap<>();

        List<Integer> order = new ArrayList<>(ss.states());
        order.sort(Comparator.naturalOrder());

        for (int start : order) {
            if (index.containsKey(start)) continue;
            // (node, child-index)
            Deque<int[]> work = new ArrayDeque<>();
            work.push(new int[]{start, 0});
            while (!work.isEmpty()) {
                int[] frame = work.peek();
                int v = frame[0];
                int ci = frame[1];
                if (!index.containsKey(v)) {
                    index.put(v, counter[0]);
                    lowlink.put(v, counter[0]);
                    counter[0]++;
                    stack.push(v);
                    onStack.put(v, true);
                }
                List<Integer> nbrs = adj.getOrDefault(v, List.of());
                boolean recurse = false;
                while (ci < nbrs.size()) {
                    int w = nbrs.get(ci);
                    if (!index.containsKey(w)) {
                        frame[1] = ci + 1;
                        work.push(new int[]{w, 0});
                        recurse = true;
                        break;
                    } else if (Boolean.TRUE.equals(onStack.get(w))) {
                        lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
                    }
                    ci++;
                }
                if (recurse) continue;
                if (lowlink.get(v).equals(index.get(v))) {
                    Set<Integer> scc = new HashSet<>();
                    while (true) {
                        int w = stack.pop();
                        onStack.put(w, false);
                        scc.add(w);
                        if (w == v) break;
                    }
                    int rep = scc.stream().min(Integer::compareTo).orElse(v);
                    for (int w : scc) sccMap.put(w, rep);
                }
                work.pop();
                if (!work.isEmpty()) {
                    int parent = work.peek()[0];
                    lowlink.put(parent, Math.min(lowlink.get(parent), lowlink.get(v)));
                }
            }
        }
        return sccMap;
    }

    private static Map<Integer, List<Integer>> adjacency(StateSpace ss) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new ArrayList<>());
        for (var t : ss.transitions()) {
            List<Integer> bucket = adj.get(t.source());
            if (!bucket.contains(t.target())) bucket.add(t.target());
        }
        return adj;
    }

    private static Map<Integer, Set<Integer>> reachability(StateSpace ss) {
        Map<Integer, List<Integer>> adj = adjacency(ss);
        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (!visited.add(u)) continue;
                for (int v : adj.getOrDefault(u, List.of())) stack.push(v);
            }
            reach.put(s, visited);
        }
        return reach;
    }

    private static QuotientData quotientNodesAndReach(StateSpace ss) {
        Map<Integer, Integer> sccMap = computeSccs(ss);
        Map<Integer, Set<Integer>> reach = reachability(ss);
        List<Integer> reps = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        List<Integer> order = new ArrayList<>(ss.states());
        order.sort(Comparator.naturalOrder());
        for (int s : order) {
            int r = sccMap.get(s);
            if (seen.add(r)) reps.add(r);
        }
        return new QuotientData(sccMap, reach, reps);
    }

    /**
     * Rank function: length of longest forward chain from {@code top}.
     * Quotients by SCCs (so cycles do not blow up). Mirrors Python
     * {@code compute_rank}, which uses {@code ss.bottom} as the initial state
     * (Python convention) -- in BICA the initial state is {@code ss.top()}.
     * Unreachable states get rank 0.
     */
    static Map<Integer, Integer> computeRank(StateSpace ss) {
        Map<Integer, Integer> sccMap = computeSccs(ss);
        Map<Integer, List<Integer>> adj = adjacency(ss);

        // Quotient DAG
        Set<Integer> repSet = new TreeSet<>(sccMap.values());
        Map<Integer, Set<Integer>> qAdj = new HashMap<>();
        for (int r : repSet) qAdj.put(r, new HashSet<>());
        for (int s : ss.states()) {
            int sr = sccMap.get(s);
            for (int t : adj.getOrDefault(s, List.of())) {
                int tr = sccMap.get(t);
                if (sr != tr) qAdj.get(sr).add(tr);
            }
        }

        int initialRep = sccMap.getOrDefault(ss.top(),
                sccMap.values().stream().min(Integer::compareTo).orElse(0));

        // Longest path from initialRep to each rep in the quotient DAG.
        Map<Integer, Integer> qRank = new HashMap<>();
        qRank.put(initialRep, 0);
        // BFS/relaxation in topological-ish order: iterate to fixpoint (DAG).
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r : repSet) {
                if (!qRank.containsKey(r)) continue;
                int dr = qRank.get(r);
                for (int t : qAdj.get(r)) {
                    int candidate = dr + 1;
                    Integer cur = qRank.get(t);
                    if (cur == null || candidate > cur) {
                        qRank.put(t, candidate);
                        changed = true;
                    }
                }
            }
        }

        Map<Integer, Integer> result = new HashMap<>();
        for (int s : ss.states()) {
            result.put(s, qRank.getOrDefault(sccMap.get(s), 0));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Sublevel + chain enumeration
    // -----------------------------------------------------------------------

    /** SCC representatives whose rank is at most {@code k}. */
    public static List<Integer> sublevelStates(StateSpace ss, int k) {
        Map<Integer, Integer> ranks = computeRank(ss);
        QuotientData q = quotientNodesAndReach(ss);
        List<Integer> out = new ArrayList<>();
        for (int r : q.reps()) {
            if (ranks.getOrDefault(r, 0) <= k) out.add(r);
        }
        return out;
    }

    /**
     * Enumerate all non-empty chains x_0 > x_1 > ... > x_k inside {@code subset}
     * under the strict SCC-quotient order, where {@code a > b} iff
     * {@code b in reach[a]} and {@code a, b} sit in different SCCs.
     */
    private static List<List<Integer>> chainsInSubset(
            List<Integer> subset,
            Map<Integer, Set<Integer>> reach,
            Map<Integer, Integer> sccMap) {
        Set<Integer> sub = new HashSet<>(subset);
        List<List<Integer>> chains = new ArrayList<>();
        for (int x : subset) {
            List<Integer> prefix = new ArrayList<>();
            prefix.add(x);
            extendChain(prefix, subset, sub, reach, sccMap, chains);
        }
        return chains;
    }

    private static void extendChain(
            List<Integer> prefix,
            List<Integer> subset,
            Set<Integer> sub,
            Map<Integer, Set<Integer>> reach,
            Map<Integer, Integer> sccMap,
            List<List<Integer>> chains) {
        chains.add(new ArrayList<>(prefix));
        int last = prefix.get(prefix.size() - 1);
        for (int y : subset) {
            if (!sub.contains(y)) continue;
            if (last == y) continue;
            Set<Integer> rLast = reach.get(last);
            if (rLast == null || !rLast.contains(y)) continue;
            if (sccMap.get(last).equals(sccMap.get(y))) continue;
            prefix.add(y);
            extendChain(prefix, subset, sub, reach, sccMap, chains);
            prefix.remove(prefix.size() - 1);
        }
    }

    private static List<Integer> faceVectorOfSubset(
            List<Integer> subset,
            Map<Integer, Set<Integer>> reach,
            Map<Integer, Integer> sccMap) {
        List<List<Integer>> chains = chainsInSubset(subset, reach, sccMap);
        if (chains.isEmpty()) return List.of();
        int maxLen = 0;
        for (List<Integer> c : chains) maxLen = Math.max(maxLen, c.size());
        int[] fv = new int[maxLen];
        for (List<Integer> c : chains) fv[c.size() - 1]++;
        List<Integer> out = new ArrayList<>(maxLen);
        for (int v : fv) out.add(v);
        return out;
    }

    private static int eulerFromFVector(List<Integer> fv) {
        int total = 0;
        for (int k = 0; k < fv.size(); k++) {
            total += ((k & 1) == 0 ? 1 : -1) * fv.get(k);
        }
        return total;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Compute the ECT fingerprint of a session type state space. */
    public static ECTFingerprint computeEct(StateSpace ss) {
        Map<Integer, Integer> ranks = computeRank(ss);
        QuotientData q = quotientNodesAndReach(ss);
        if (q.reps().isEmpty()) {
            return new ECTFingerprint(List.of(), 0, List.of(), List.of());
        }
        int K = 0;
        for (int r : q.reps()) K = Math.max(K, ranks.getOrDefault(r, 0));

        List<Integer> chis = new ArrayList<>();
        List<List<Integer>> fvs = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();
        for (int k = 0; k <= K; k++) {
            List<Integer> subset = new ArrayList<>();
            for (int r : q.reps()) {
                if (ranks.getOrDefault(r, 0) <= k) subset.add(r);
            }
            List<Integer> fv = faceVectorOfSubset(subset, q.reach(), q.sccMap());
            chis.add(eulerFromFVector(fv));
            fvs.add(fv);
            sizes.add(subset.size());
        }

        return new ECTFingerprint(chis, K, fvs, sizes);
    }

    /** Compare two ECT fingerprints. */
    public static ECTComparison compareEct(ECTFingerprint a, ECTFingerprint b) {
        int n = Math.max(a.chiSequence().size(), b.chiSequence().size());
        int first = -1;
        int l1 = 0;
        for (int i = 0; i < n; i++) {
            int x = i < a.chiSequence().size() ? a.chiSequence().get(i) : 0;
            int y = i < b.chiSequence().size() ? b.chiSequence().get(i) : 0;
            if (first == -1 && x != y) first = i;
            l1 += Math.abs(x - y);
        }
        return new ECTComparison(a, b, first == -1, l1, first);
    }

    /** Recovery map psi: fingerprint to a tuple of derived lattice invariants. */
    public static PsiInvariants psiRecoverInvariants(ECTFingerprint fp) {
        int total = 0;
        for (int v : fp.chiSequence()) total += v;
        int top = fp.chiSequence().isEmpty() ? 0
                : fp.chiSequence().get(fp.chiSequence().size() - 1);
        int maxSize = 0;
        for (int v : fp.levelSizes()) maxSize = Math.max(maxSize, v);
        return new PsiInvariants(total, fp.rankMax(), top, maxSize);
    }

    /** Classify the (phi, psi) pair on a collection of state spaces. */
    public static MorphismClassification classifyEctMorphism(List<StateSpace> samples) {
        List<ECTFingerprint> fps = new ArrayList<>();
        for (StateSpace s : samples) fps.add(computeEct(s));

        boolean phiSurj = true;

        boolean phiOp = true;
        outer:
        for (int i = 0; i < fps.size(); i++) {
            ECTFingerprint a = fps.get(i);
            for (int j = i + 1; j < fps.size(); j++) {
                ECTFingerprint b = fps.get(j);
                if (a.length() <= b.length()) {
                    int ak = a.length() == 0 ? -1 : Math.min(a.length() - 1, b.length() - 1);
                    if (ak >= 0
                            && a.chiSequence().get(ak) > b.chiSequence().get(ak) + b.length()) {
                        phiOp = false;
                        break outer;
                    }
                }
            }
        }

        boolean psiAdj = true;
        for (ECTFingerprint f : fps) {
            PsiInvariants inv = psiRecoverInvariants(f);
            if (inv.rank() != f.rankMax()) {
                psiAdj = false;
                break;
            }
        }

        boolean galois = phiOp && psiAdj;
        String classification = galois ? "galois" : (phiOp ? "projection" : "injection");
        return new MorphismClassification(phiSurj, phiOp, psiAdj, galois, classification);
    }

    // -----------------------------------------------------------------------
    // Convenience
    // -----------------------------------------------------------------------

    /** Parse a session type string and compute its ECT fingerprint. */
    public static ECTFingerprint computeEct(String sessionTypeString) {
        SessionType ast = Parser.parse(sessionTypeString);
        StateSpace ss = StateSpaceBuilder.build(ast);
        return computeEct(ss);
    }
}
