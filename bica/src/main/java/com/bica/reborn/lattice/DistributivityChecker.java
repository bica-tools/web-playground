package com.bica.reborn.lattice;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Distributivity checking for session type lattices.
 *
 * <p>Determines whether a lattice is distributive, modular, or Boolean by searching
 * for forbidden sublattices N₅ (pentagon) and M₃ (diamond) per Birkhoff's
 * characterization theorem (Theorem IX.2).
 *
 * <p>Key findings from the research programme:
 * <ul>
 *   <li>20/22 non-distributive benchmarks contain N₅ — non-distributivity is about
 *       path divergence, not symmetry</li>
 *   <li>Only 2/22 contain M₃ (diamond) — complementation conflict is rare</li>
 *   <li>Non-distributivity comes from parallel+synchronization (10/22) and
 *       L₂ branch/select nesting (10/22), NOT from recursion (2/22)</li>
 * </ul>
 */
public final class DistributivityChecker {

    private DistributivityChecker() {}

    /**
     * Check whether the reachability poset of {@code ss} is a distributive lattice.
     *
     * <p>First checks lattice property, then searches for forbidden N₅ and M₃
     * sublattices. Classifies into: boolean, distributive, modular, lattice, not_lattice.
     */
    public static DistributivityResult checkDistributive(StateSpace ss) {
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) {
            return new DistributivityResult(
                    false, false, false, false, false,
                    null, null, "not_lattice", lr);
        }

        QuotientPoset q = QuotientPoset.build(ss);

        List<Integer> n5 = findN5(q);
        List<Integer> m3 = findM3(q);

        boolean hasN5 = n5 != null;
        boolean hasM3 = m3 != null;

        // Birkhoff's characterization:
        // Modular iff no N₅; Distributive iff no N₅ and no M₃
        boolean isModular = !hasN5;
        boolean isDistributive = isModular && !hasM3;

        String classification;
        if (isDistributive) {
            boolean isBoolean = checkBoolean(q);
            classification = isBoolean ? "boolean" : "distributive";
        } else if (isModular) {
            classification = "modular";
        } else {
            classification = "lattice";
        }

        // Map witnesses back to original state IDs
        List<Integer> m3Orig = m3 != null
                ? m3.stream().map(q.rep::get).toList() : null;
        List<Integer> n5Orig = n5 != null
                ? n5.stream().map(q.rep::get).toList() : null;

        return new DistributivityResult(
                true, isDistributive, isModular, hasM3, hasN5,
                m3Orig, n5Orig, classification, lr);
    }

    // -- N₅ (pentagon) search ---------------------------------------------------

    /**
     * Search for an N₅ (pentagon) sublattice.
     *
     * <p>N₅ has 5 elements {0, a, b, c, 1} with:
     * 1 &gt; a &gt; b &gt; 0, 1 &gt; c &gt; 0, and a ∥ c, b ∥ c
     * (c is incomparable with both a and b).
     *
     * <p>The chain shape {@code top > a > b > bot, top > c > bot} is a
     * necessary but NOT sufficient condition: in a larger lattice, the
     * five elements may sit in the right places without forming an
     * actual N₅ sublattice (the sublattice closure under meet/join may
     * pull in extra elements). The full N₅ sublattice condition
     * additionally requires:
     *
     * <pre>
     *   meet(a, c) = bot     (not some intermediate element)
     *   join(b, c) = top     (not some intermediate element)
     * </pre>
     *
     * <p>Without these two checks the search returns false positives
     * on perfectly distributive lattices — Task 44 tracked one such
     * false positive on the 3×3 grid lattice
     * {@code (a . b . end || c . d . end)}: the antichain pattern
     * {@code (a=(0,2), b=(1,2), c=(2,0))} satisfies the chain shape
     * but {@code join(b, c) = (1, 0) ≠ top = (0, 0)}, so the five
     * elements do not form a sublattice. The fix below mirrors
     * the {@code reticulate.lattice._find_n5} reference implementation
     * (Python lines 482–484).
     *
     * @return 5-element list [top, a, b, c, bot] of quotient node IDs, or null
     */
    static List<Integer> findN5(QuotientPoset q) {
        List<Integer> nodes = new ArrayList<>(q.nodes);
        Collections.sort(nodes);
        int n = nodes.size();

        for (int i = 0; i < n; i++) {
            int a = nodes.get(i);
            for (int j = 0; j < n; j++) {
                int b = nodes.get(j);
                if (a == b || !q.le(a, b)) continue;
                // a > b in the ordering
                for (int k = 0; k < n; k++) {
                    int c = nodes.get(k);
                    if (c == a || c == b) continue;
                    if (!q.incomparable(a, c) || !q.incomparable(b, c)) continue;

                    // Found a, b, c with a > b, c ∥ a, c ∥ b
                    Integer top = q.join(a, c);
                    Integer bot = q.meet(b, c);
                    if (top == null || bot == null) continue;

                    // Necessary chain shape: top > a > b > bot, top > c > bot
                    if (!(q.le(top, a) && q.le(a, b) && q.le(b, bot)
                            && q.le(top, c) && q.le(c, bot))) {
                        continue;
                    }

                    // Sublattice closure: in N₅ we additionally need
                    // meet(a, c) = bot and join(b, c) = top, otherwise
                    // the five elements are not closed under meet/join
                    // and do not form an actual sublattice. See Task 44.
                    Integer mac = q.meet(a, c);
                    Integer jbc = q.join(b, c);
                    if (mac != null && mac.equals(bot)
                            && jbc != null && jbc.equals(top)) {
                        return List.of(top, a, b, c, bot);
                    }
                }
            }
        }
        return null;
    }

    // -- M₃ (diamond) search ----------------------------------------------------

    /**
     * Search for an M₃ (diamond) sublattice.
     *
     * <p>M₃ has 5 elements {0, a, b, c, 1} with:
     * 1 &gt; a, b, c &gt; 0; a, b, c pairwise incomparable;
     * meet(a,b) = meet(a,c) = meet(b,c) = 0;
     * join(a,b) = join(a,c) = join(b,c) = 1.
     *
     * @return 5-element list [top, a, b, c, bot] of quotient node IDs, or null
     */
    static List<Integer> findM3(QuotientPoset q) {
        List<Integer> nodes = new ArrayList<>(q.nodes);
        Collections.sort(nodes);
        int n = nodes.size();

        for (int i = 0; i < n; i++) {
            int a = nodes.get(i);
            for (int j = i + 1; j < n; j++) {
                int b = nodes.get(j);
                if (!q.incomparable(a, b)) continue;

                Integer jab = q.join(a, b);
                Integer mab = q.meet(a, b);
                if (jab == null || mab == null) continue;

                for (int k = j + 1; k < n; k++) {
                    int c = nodes.get(k);
                    if (!q.incomparable(a, c) || !q.incomparable(b, c)) continue;

                    Integer jac = q.join(a, c);
                    Integer mac = q.meet(a, c);
                    Integer jbc = q.join(b, c);
                    Integer mbc = q.meet(b, c);

                    if (jac != null && jac.equals(jab) && jac.equals(jbc)
                            && mac != null && mac.equals(mab) && mac.equals(mbc)) {
                        return List.of(jab, a, b, c, mab);
                    }
                }
            }
        }
        return null;
    }

    // -- Boolean check -----------------------------------------------------------

    /**
     * Check whether a distributive lattice is Boolean (every element complemented).
     */
    static boolean checkBoolean(QuotientPoset q) {
        for (int a : q.nodes) {
            boolean hasComplement = false;
            for (int b : q.nodes) {
                Integer m = q.meet(a, b);
                Integer j = q.join(a, b);
                if (m != null && m == q.bottom && j != null && j == q.top) {
                    hasComplement = true;
                    break;
                }
            }
            if (!hasComplement) return false;
        }
        return true;
    }

    // -- Quotient Poset (shared with LatticeChecker) ---------------------------

    /**
     * SCC-quotient poset with reachability and meet/join operations.
     * Extracted so DistributivityChecker can reuse it without re-building.
     */
    public record QuotientPoset(
            Set<Integer> nodes,
            int top,
            int bottom,
            Map<Integer, Set<Integer>> fwdAdj,
            Map<Integer, Set<Integer>> revAdj,
            Map<Integer, Set<Integer>> fwdReach,
            Map<Integer, Set<Integer>> revReach,
            Map<Integer, Integer> rep,
            Map<Integer, Integer> stateToNode) {

        /** Build from a state space (SCC-quotient, reachability). */
        public static QuotientPoset build(StateSpace ss) {
            // Step 1: adjacency
            Map<Integer, List<Integer>> adj = new HashMap<>();
            for (int s : ss.states()) {
                adj.put(s, new ArrayList<>());
            }
            for (var t : ss.transitions()) {
                adj.get(t.source()).add(t.target());
            }

            // Step 2: SCCs
            List<Set<Integer>> sccs = computeSccs(ss.states(), adj);

            Map<Integer, Integer> stateToScc = new HashMap<>();
            Map<Integer, Integer> rep = new HashMap<>();
            for (int idx = 0; idx < sccs.size(); idx++) {
                Set<Integer> scc = sccs.get(idx);
                int r = Collections.min(scc);
                rep.put(idx, r);
                for (int s : scc) stateToScc.put(s, idx);
            }

            Set<Integer> nodes = new HashSet<>();
            for (int i = 0; i < sccs.size(); i++) nodes.add(i);

            // Step 3: quotient DAG edges
            Map<Integer, Set<Integer>> fwdAdj = new HashMap<>();
            Map<Integer, Set<Integer>> revAdj = new HashMap<>();
            for (int n : nodes) {
                fwdAdj.put(n, new HashSet<>());
                revAdj.put(n, new HashSet<>());
            }
            for (var t : ss.transitions()) {
                int sScc = stateToScc.get(t.source());
                int tScc = stateToScc.get(t.target());
                if (sScc != tScc) {
                    fwdAdj.get(sScc).add(tScc);
                    revAdj.get(tScc).add(sScc);
                }
            }

            // Step 4: reachability via topo-order DP
            List<Integer> topo = topologicalSort(nodes, fwdAdj);

            Map<Integer, Set<Integer>> fwdReach = new HashMap<>();
            for (int n : nodes) fwdReach.put(n, new HashSet<>(Set.of(n)));
            for (int i = topo.size() - 1; i >= 0; i--) {
                int n = topo.get(i);
                for (int succ : fwdAdj.get(n)) fwdReach.get(n).addAll(fwdReach.get(succ));
            }

            Map<Integer, Set<Integer>> revReach = new HashMap<>();
            for (int n : nodes) revReach.put(n, new HashSet<>(Set.of(n)));
            for (int n : topo) {
                for (int pred : revAdj.get(n)) revReach.get(n).addAll(revReach.get(pred));
            }

            int qTop = stateToScc.get(ss.top());
            int qBottom = stateToScc.get(ss.bottom());

            return new QuotientPoset(nodes, qTop, qBottom, fwdAdj, revAdj,
                    fwdReach, revReach, rep, stateToScc);
        }

        /** x ≤ y in the poset ordering (y reachable from x). */
        boolean le(int x, int y) {
            return fwdReach.get(x).contains(y);
        }

        /** x and y are incomparable. */
        boolean incomparable(int x, int y) {
            return !le(x, y) && !le(y, x);
        }

        /** Meet (greatest lower bound) on the quotient, or null. */
        Integer meet(int a, int b) {
            if (a == b) return a;
            Set<Integer> lb = new HashSet<>(fwdReach.get(a));
            lb.retainAll(fwdReach.get(b));
            if (lb.isEmpty()) return null;
            for (int m : lb) {
                if (fwdReach.get(m).containsAll(lb)) return m;
            }
            return null;
        }

        /** Join (least upper bound) on the quotient, or null. */
        Integer join(int a, int b) {
            if (a == b) return a;
            Set<Integer> ub = new HashSet<>(revReach.get(a));
            ub.retainAll(revReach.get(b));
            if (ub.isEmpty()) return null;
            for (int j : ub) {
                if (revReach.get(j).containsAll(ub)) return j;
            }
            return null;
        }

        // -- SCC computation (iterative Tarjan's) --

        private static List<Set<Integer>> computeSccs(
                Set<Integer> states, Map<Integer, List<Integer>> adj) {

            int[] counter = {0};
            Deque<Integer> stack = new ArrayDeque<>();
            Set<Integer> onStack = new HashSet<>();
            Map<Integer, Integer> index = new HashMap<>();
            Map<Integer, Integer> lowlink = new HashMap<>();
            List<Set<Integer>> result = new ArrayList<>();

            List<Integer> sorted = new ArrayList<>(states);
            Collections.sort(sorted);

            for (int start : sorted) {
                if (index.containsKey(start)) continue;

                Deque<int[]> callStack = new ArrayDeque<>();
                index.put(start, counter[0]);
                lowlink.put(start, counter[0]);
                counter[0]++;
                stack.push(start);
                onStack.add(start);
                callStack.push(new int[]{start, 0});

                while (!callStack.isEmpty()) {
                    int[] frame = callStack.peek();
                    int v = frame[0];
                    int ni = frame[1];
                    List<Integer> neighbors = adj.getOrDefault(v, List.of());

                    if (ni < neighbors.size()) {
                        frame[1] = ni + 1;
                        int w = neighbors.get(ni);
                        if (!index.containsKey(w)) {
                            index.put(w, counter[0]);
                            lowlink.put(w, counter[0]);
                            counter[0]++;
                            stack.push(w);
                            onStack.add(w);
                            callStack.push(new int[]{w, 0});
                        } else if (onStack.contains(w)) {
                            lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
                        }
                    } else {
                        if (lowlink.get(v).equals(index.get(v))) {
                            Set<Integer> scc = new HashSet<>();
                            while (true) {
                                int w = stack.pop();
                                onStack.remove(w);
                                scc.add(w);
                                if (w == v) break;
                            }
                            result.add(scc);
                        }
                        callStack.pop();
                        if (!callStack.isEmpty()) {
                            int parent = callStack.peek()[0];
                            lowlink.put(parent,
                                    Math.min(lowlink.get(parent), lowlink.get(v)));
                        }
                    }
                }
            }
            return result;
        }

        // -- Topological sort (Kahn's algorithm) --

        private static List<Integer> topologicalSort(
                Set<Integer> nodes, Map<Integer, Set<Integer>> fwdAdj) {

            Map<Integer, Integer> inDegree = new HashMap<>();
            for (int n : nodes) inDegree.put(n, 0);
            for (int n : nodes) {
                for (int m : fwdAdj.get(n)) inDegree.merge(m, 1, Integer::sum);
            }

            List<Integer> queue = new ArrayList<>();
            for (int n : nodes) {
                if (inDegree.get(n) == 0) queue.add(n);
            }
            Collections.sort(queue);

            List<Integer> order = new ArrayList<>();
            while (!queue.isEmpty()) {
                int n = queue.removeFirst();
                order.add(n);
                List<Integer> succs = new ArrayList<>(fwdAdj.get(n));
                Collections.sort(succs);
                for (int m : succs) {
                    inDegree.merge(m, -1, Integer::sum);
                    if (inDegree.get(m) == 0) queue.add(m);
                }
            }
            return order;
        }
    }
}
