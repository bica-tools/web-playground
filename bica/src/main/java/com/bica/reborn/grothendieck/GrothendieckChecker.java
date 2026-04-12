package com.bica.reborn.grothendieck;

import com.bica.reborn.statespace.StateSpace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Grothendieck group K_0 of protocol lattices (Step 32k) -- Java port of
 * {@code reticulate/reticulate/grothendieck.py}.
 *
 * <p>Forms K_0 of the commutative monoid of (isomorphism classes of) finite
 * protocol lattices under either disjoint sum or Cartesian product. Iso classes
 * are approximated by a canonical signature: (size, height, rank profile,
 * sorted in/out-degree multiset on the SCC quotient). Distinct signatures yield
 * distinct generators.
 *
 * <p>Pure Java; uses BICA's StateSpace.
 */
public final class GrothendieckChecker {

    private GrothendieckChecker() {}

    // -----------------------------------------------------------------------
    // Lattice signature
    // -----------------------------------------------------------------------

    /**
     * Canonical invariant signature of a protocol lattice.
     *
     * @param size       number of nodes in the SCC quotient (rank-profile sum)
     * @param height     longest corank (length of rank profile minus one)
     * @param rankProfile counts of nodes at each corank from the top
     * @param degreeMultiset sorted multiset of (in-degree, out-degree) pairs
     */
    public record LatticeSig(
            int size,
            int height,
            List<Integer> rankProfile,
            List<int[]> degreeMultiset) {

        public LatticeSig {
            Objects.requireNonNull(rankProfile, "rankProfile must not be null");
            Objects.requireNonNull(degreeMultiset, "degreeMultiset must not be null");
            rankProfile = List.copyOf(rankProfile);
            // deep copy degree pairs and lock immutability via List.copyOf
            List<int[]> copy = new ArrayList<>(degreeMultiset.size());
            for (int[] p : degreeMultiset) {
                copy.add(new int[] {p[0], p[1]});
            }
            degreeMultiset = List.copyOf(copy);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LatticeSig other)) return false;
            if (size != other.size || height != other.height) return false;
            if (!rankProfile.equals(other.rankProfile)) return false;
            if (degreeMultiset.size() != other.degreeMultiset.size()) return false;
            for (int i = 0; i < degreeMultiset.size(); i++) {
                int[] a = degreeMultiset.get(i);
                int[] b = other.degreeMultiset.get(i);
                if (a[0] != b[0] || a[1] != b[1]) return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int h = Objects.hash(size, height, rankProfile);
            for (int[] p : degreeMultiset) {
                h = 31 * h + p[0];
                h = 31 * h + p[1];
            }
            return h;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("LatticeSig(size=")
                    .append(size).append(", height=").append(height)
                    .append(", rank=").append(rankProfile).append(", deg=[");
            for (int i = 0; i < degreeMultiset.size(); i++) {
                int[] p = degreeMultiset.get(i);
                if (i > 0) sb.append(", ");
                sb.append("(").append(p[0]).append(",").append(p[1]).append(")");
            }
            return sb.append("])").toString();
        }
    }

    // -----------------------------------------------------------------------
    // SCC computation (iterative Tarjan's)
    // -----------------------------------------------------------------------

    /** SCC map: state -> component id. Component ids are assigned in discovery order. */
    static Map<Integer, Integer> computeSccs(StateSpace ss) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new ArrayList<>());
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new ArrayList<>()).add(t.target());
            adj.computeIfAbsent(t.target(), k -> new ArrayList<>());
        }

        Map<Integer, Integer> index = new HashMap<>();
        Map<Integer, Integer> lowlink = new HashMap<>();
        Set<Integer> onStack = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        Map<Integer, Integer> sccMap = new HashMap<>();
        int[] idxCounter = {0};
        int[] sccCounter = {0};

        // Iterative Tarjan's
        for (int start : new TreeSet<>(adj.keySet())) {
            if (index.containsKey(start)) continue;
            Deque<int[]> work = new ArrayDeque<>(); // {node, iterIndex}
            work.push(new int[] {start, 0});
            index.put(start, idxCounter[0]);
            lowlink.put(start, idxCounter[0]);
            idxCounter[0]++;
            stack.push(start);
            onStack.add(start);

            while (!work.isEmpty()) {
                int[] frame = work.peek();
                int v = frame[0];
                List<Integer> succ = adj.get(v);
                if (frame[1] < succ.size()) {
                    int w = succ.get(frame[1]);
                    frame[1]++;
                    if (!index.containsKey(w)) {
                        index.put(w, idxCounter[0]);
                        lowlink.put(w, idxCounter[0]);
                        idxCounter[0]++;
                        stack.push(w);
                        onStack.add(w);
                        work.push(new int[] {w, 0});
                    } else if (onStack.contains(w)) {
                        lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
                    }
                } else {
                    if (lowlink.get(v).equals(index.get(v))) {
                        int sccId = sccCounter[0]++;
                        while (true) {
                            int w = stack.pop();
                            onStack.remove(w);
                            sccMap.put(w, sccId);
                            if (w == v) break;
                        }
                    }
                    work.pop();
                    if (!work.isEmpty()) {
                        int parent = work.peek()[0];
                        lowlink.put(parent, Math.min(lowlink.get(parent), lowlink.get(v)));
                    }
                }
            }
        }
        return sccMap;
    }

    // -----------------------------------------------------------------------
    // Rank profile and degree multiset
    // -----------------------------------------------------------------------

    static List<Integer> rankProfile(StateSpace ss) {
        Map<Integer, Integer> sccMap = computeSccs(ss);
        Map<Integer, Set<Integer>> fwd = new HashMap<>();
        Set<Integer> nodes = new LinkedHashSet<>();
        for (var t : ss.transitions()) {
            int a = sccMap.get(t.source());
            int b = sccMap.get(t.target());
            if (a == b) continue;
            nodes.add(a);
            nodes.add(b);
            fwd.computeIfAbsent(a, k -> new LinkedHashSet<>()).add(b);
        }
        nodes.add(sccMap.get(ss.top()));
        nodes.add(sccMap.get(ss.bottom()));
        int top = sccMap.get(ss.top());

        Map<Integer, Integer> corank = new HashMap<>();
        corank.put(top, 0);
        Deque<Integer> dq = new ArrayDeque<>();
        dq.add(top);
        while (!dq.isEmpty()) {
            int u = dq.poll();
            for (int v : fwd.getOrDefault(u, Set.of())) {
                if (!corank.containsKey(v) || corank.get(v) > corank.get(u) + 1) {
                    corank.put(v, corank.get(u) + 1);
                    dq.add(v);
                }
            }
        }
        if (corank.isEmpty()) {
            return List.of(1);
        }
        int h = 0;
        for (int v : corank.values()) if (v > h) h = v;
        int[] prof = new int[h + 1];
        for (int n : nodes) {
            Integer r = corank.get(n);
            if (r != null) prof[r]++;
        }
        List<Integer> out = new ArrayList<>(prof.length);
        for (int p : prof) out.add(p);
        return out;
    }

    static List<int[]> degreeMultiset(StateSpace ss) {
        Map<Integer, Integer> sccMap = computeSccs(ss);
        Set<Integer> nodes = new LinkedHashSet<>();
        for (int s : ss.states()) nodes.add(sccMap.get(s));
        nodes.add(sccMap.get(ss.top()));
        nodes.add(sccMap.get(ss.bottom()));

        Map<Integer, Integer> indeg = new HashMap<>();
        Map<Integer, Integer> outdeg = new HashMap<>();
        for (int n : nodes) {
            indeg.put(n, 0);
            outdeg.put(n, 0);
        }
        for (var t : ss.transitions()) {
            int a = sccMap.get(t.source());
            int b = sccMap.get(t.target());
            if (a == b) continue;
            outdeg.merge(a, 1, Integer::sum);
            indeg.merge(b, 1, Integer::sum);
        }
        List<int[]> pairs = new ArrayList<>();
        for (int n : nodes) {
            pairs.add(new int[] {indeg.get(n), outdeg.get(n)});
        }
        pairs.sort((x, y) -> {
            if (x[0] != y[0]) return Integer.compare(x[0], y[0]);
            return Integer.compare(x[1], y[1]);
        });
        return pairs;
    }

    /** Canonical invariant signature used as generator key in K_0. */
    public static LatticeSig latticeSignature(StateSpace ss) {
        List<Integer> prof = rankProfile(ss);
        List<int[]> deg = degreeMultiset(ss);
        int size = 0;
        for (int p : prof) size += p;
        int height = prof.size() - 1;
        return new LatticeSig(size, height, prof, deg);
    }

    // -----------------------------------------------------------------------
    // VirtualLattice -- element of K_0
    // -----------------------------------------------------------------------

    /**
     * Element of K_0 -- a formal difference [positive] - [negative] of
     * multisets of generator signatures, in canonical (cancelled) form.
     */
    public static final class VirtualLattice {
        private final TreeMap<LatticeSig, Integer> positive;
        private final TreeMap<LatticeSig, Integer> negative;

        private VirtualLattice(TreeMap<LatticeSig, Integer> pos, TreeMap<LatticeSig, Integer> neg) {
            this.positive = pos;
            this.negative = neg;
        }

        private static TreeMap<LatticeSig, Integer> emptyMap() {
            return new TreeMap<>(SIG_COMPARATOR);
        }

        public static VirtualLattice zero() {
            return new VirtualLattice(emptyMap(), emptyMap());
        }

        public static VirtualLattice generator(LatticeSig sig) {
            TreeMap<LatticeSig, Integer> p = emptyMap();
            p.put(sig, 1);
            return new VirtualLattice(p, emptyMap());
        }

        /** Construct from raw counters, performing common-part cancellation. */
        public static VirtualLattice fromCounters(
                Map<LatticeSig, Integer> pos, Map<LatticeSig, Integer> neg) {
            TreeMap<LatticeSig, Integer> p = emptyMap();
            TreeMap<LatticeSig, Integer> n = emptyMap();
            p.putAll(pos);
            n.putAll(neg);
            // Cancel common.
            for (LatticeSig k : new ArrayList<>(p.keySet())) {
                if (n.containsKey(k)) {
                    int common = Math.min(p.get(k), n.get(k));
                    int pv = p.get(k) - common;
                    int nv = n.get(k) - common;
                    if (pv > 0) p.put(k, pv); else p.remove(k);
                    if (nv > 0) n.put(k, nv); else n.remove(k);
                }
            }
            // Strip non-positive.
            p.entrySet().removeIf(e -> e.getValue() <= 0);
            n.entrySet().removeIf(e -> e.getValue() <= 0);
            return new VirtualLattice(p, n);
        }

        public Map<LatticeSig, Integer> posCounter() {
            return Collections.unmodifiableMap(positive);
        }

        public Map<LatticeSig, Integer> negCounter() {
            return Collections.unmodifiableMap(negative);
        }

        public VirtualLattice add(VirtualLattice other) {
            TreeMap<LatticeSig, Integer> p = emptyMap();
            TreeMap<LatticeSig, Integer> n = emptyMap();
            p.putAll(this.positive);
            other.positive.forEach((k, v) -> p.merge(k, v, Integer::sum));
            n.putAll(this.negative);
            other.negative.forEach((k, v) -> n.merge(k, v, Integer::sum));
            return fromCounters(p, n);
        }

        public VirtualLattice negate() {
            return fromCounters(this.negative, this.positive);
        }

        public VirtualLattice subtract(VirtualLattice other) {
            return this.add(other.negate());
        }

        public boolean isZero() {
            return positive.isEmpty() && negative.isEmpty();
        }

        /** Sum of size(sig)*multiplicity over positive minus negative. */
        public int totalSize() {
            int s = 0;
            for (var e : positive.entrySet()) s += e.getKey().size() * e.getValue();
            for (var e : negative.entrySet()) s -= e.getKey().size() * e.getValue();
            return s;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VirtualLattice other)) return false;
            return positive.equals(other.positive) && negative.equals(other.negative);
        }

        @Override
        public int hashCode() {
            return Objects.hash(positive, negative);
        }

        @Override
        public String toString() {
            return "VirtualLattice(+" + positive + ", -" + negative + ")";
        }
    }

    /** Total order on LatticeSig used by VirtualLattice's TreeMaps. */
    private static final java.util.Comparator<LatticeSig> SIG_COMPARATOR = (a, b) -> {
        int c = Integer.compare(a.size(), b.size());
        if (c != 0) return c;
        c = Integer.compare(a.height(), b.height());
        if (c != 0) return c;
        c = compareIntList(a.rankProfile(), b.rankProfile());
        if (c != 0) return c;
        int la = a.degreeMultiset().size();
        int lb = b.degreeMultiset().size();
        c = Integer.compare(la, lb);
        if (c != 0) return c;
        for (int i = 0; i < la; i++) {
            int[] x = a.degreeMultiset().get(i);
            int[] y = b.degreeMultiset().get(i);
            c = Integer.compare(x[0], y[0]);
            if (c != 0) return c;
            c = Integer.compare(x[1], y[1]);
            if (c != 0) return c;
        }
        return 0;
    };

    private static int compareIntList(List<Integer> a, List<Integer> b) {
        int n = Math.min(a.size(), b.size());
        for (int i = 0; i < n; i++) {
            int c = Integer.compare(a.get(i), b.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(a.size(), b.size());
    }

    // -----------------------------------------------------------------------
    // Grothendieck group
    // -----------------------------------------------------------------------

    /** The Grothendieck group K_0(M, op). */
    public static final class GrothendieckGroup {
        private final String operation;
        private final LinkedHashMap<LatticeSig, Integer> generators = new LinkedHashMap<>();

        public GrothendieckGroup() {
            this("sum");
        }

        public GrothendieckGroup(String operation) {
            this.operation = Objects.requireNonNull(operation);
        }

        public String operation() {
            return operation;
        }

        public LatticeSig register(StateSpace ss) {
            LatticeSig sig = latticeSignature(ss);
            generators.merge(sig, 1, Integer::sum);
            return sig;
        }

        /** Sorted list of registered generator signatures. */
        public List<LatticeSig> generators() {
            List<LatticeSig> out = new ArrayList<>(generators.keySet());
            out.sort(SIG_COMPARATOR);
            return out;
        }

        /** phi: L(S) -> [sig(L(S))] in K_0. */
        public VirtualLattice phi(StateSpace ss) {
            LatticeSig sig = latticeSignature(ss);
            generators.putIfAbsent(sig, 0);
            return VirtualLattice.generator(sig);
        }

        /** psi: v -> (positive multiset, negative multiset). */
        public PsiResult psi(VirtualLattice v) {
            return new PsiResult(v.posCounter(), v.negCounter());
        }

        /** Combine two signatures under the chosen monoid operation. */
        public LatticeSig combineSignatures(LatticeSig s1, LatticeSig s2) {
            if ("sum".equals(operation)) {
                List<Integer> p1 = s1.rankProfile();
                List<Integer> p2 = s2.rankProfile();
                int L = Math.max(p1.size(), p2.size());
                List<Integer> prof = new ArrayList<>(L);
                for (int k = 0; k < L; k++) {
                    int a = k < p1.size() ? p1.get(k) : 0;
                    int b = k < p2.size() ? p2.get(k) : 0;
                    prof.add(a + b);
                }
                List<int[]> deg = new ArrayList<>(s1.degreeMultiset().size() + s2.degreeMultiset().size());
                for (int[] p : s1.degreeMultiset()) deg.add(new int[] {p[0], p[1]});
                for (int[] p : s2.degreeMultiset()) deg.add(new int[] {p[0], p[1]});
                deg.sort((x, y) -> {
                    if (x[0] != y[0]) return Integer.compare(x[0], y[0]);
                    return Integer.compare(x[1], y[1]);
                });
                int size = 0;
                for (int p : prof) size += p;
                int height = prof.size() - 1;
                return new LatticeSig(size, height, prof, deg);
            } else if ("product".equals(operation)) {
                List<Integer> p1 = s1.rankProfile();
                List<Integer> p2 = s2.rankProfile();
                List<Integer> prof;
                if (p1.isEmpty() || p2.isEmpty()) {
                    prof = List.of();
                } else {
                    int n = p1.size() + p2.size() - 1;
                    int[] out = new int[n];
                    for (int i = 0; i < p1.size(); i++) {
                        for (int j = 0; j < p2.size(); j++) {
                            out[i + j] += p1.get(i) * p2.get(j);
                        }
                    }
                    prof = new ArrayList<>(n);
                    for (int v : out) prof.add(v);
                }
                List<int[]> deg = new ArrayList<>();
                for (int[] x : s1.degreeMultiset()) {
                    for (int[] y : s2.degreeMultiset()) {
                        deg.add(new int[] {x[0] + y[0], x[1] + y[1]});
                    }
                }
                deg.sort((x, y) -> {
                    if (x[0] != y[0]) return Integer.compare(x[0], y[0]);
                    return Integer.compare(x[1], y[1]);
                });
                int size = 0;
                for (int p : prof) size += p;
                int height = prof.isEmpty() ? -1 : prof.size() - 1;
                return new LatticeSig(size, Math.max(height, 0), prof, deg);
            } else {
                throw new IllegalArgumentException("Unknown operation: " + operation);
            }
        }

        /** [a] - [b] in K_0. */
        public VirtualLattice virtualDifference(StateSpace a, StateSpace b) {
            return phi(a).subtract(phi(b));
        }

        /** Equality in K_0 -- both sides are already reduced. */
        public boolean equal(VirtualLattice u, VirtualLattice v) {
            return u.equals(v);
        }
    }

    /** Pair returned by psi: positive and negative generator multisets. */
    public record PsiResult(
            Map<LatticeSig, Integer> positive,
            Map<LatticeSig, Integer> negative) {

        public PsiResult {
            positive = Map.copyOf(positive);
            negative = Map.copyOf(negative);
        }

        public int positiveTotal() {
            int s = 0;
            for (int v : positive.values()) s += v;
            return s;
        }

        public int negativeTotal() {
            int s = 0;
            for (int v : negative.values()) s += v;
            return s;
        }
    }

    // -----------------------------------------------------------------------
    // Public high-level API
    // -----------------------------------------------------------------------

    /** Aggregate result of a {@link #buildK0} run. */
    public record GrothendieckResult(
            String operation,
            List<LatticeSig> generators,
            int numGenerators,
            List<VirtualLattice> virtualClasses,
            int relationsVerified,
            boolean isFreeAbelian) {

        public GrothendieckResult {
            generators = List.copyOf(generators);
            virtualClasses = List.copyOf(virtualClasses);
        }
    }

    /** Build K_0 from a collection of lattices and verify basic laws. */
    public static GrothendieckResult buildK0(Iterable<StateSpace> spaces, String operation) {
        GrothendieckGroup G = new GrothendieckGroup(operation);
        List<LatticeSig> sigs = new ArrayList<>();
        List<VirtualLattice> virtuals = new ArrayList<>();
        int relations = 0;
        for (StateSpace ss : spaces) {
            LatticeSig sig = G.register(ss);
            sigs.add(sig);
            VirtualLattice v = G.phi(ss);
            virtuals.add(v);
            // psi o phi round-trip.
            PsiResult pr = G.psi(v);
            if (pr.positive().size() != 1 || !pr.positive().containsKey(sig)
                    || pr.positive().get(sig) != 1 || !pr.negative().isEmpty()) {
                throw new AssertionError("psi o phi round-trip failed for " + sig);
            }
            relations++;
            // Inverse law.
            if (!v.add(v.negate()).isZero()) {
                throw new AssertionError("Inverse law failed for " + sig);
            }
            relations++;
        }
        Set<LatticeSig> distinct = new HashSet<>(sigs);
        boolean isFree = distinct.size() == G.generators().size();
        return new GrothendieckResult(
                operation,
                G.generators(),
                G.generators().size(),
                virtuals,
                relations,
                isFree);
    }

    /** Convenience overload using the default "sum" operation. */
    public static GrothendieckResult buildK0(Iterable<StateSpace> spaces) {
        return buildK0(spaces, "sum");
    }
}
