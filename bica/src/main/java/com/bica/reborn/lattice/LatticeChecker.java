package com.bica.reborn.lattice;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Lattice property checking for session type state spaces.
 *
 * <p>Given a {@link StateSpace}, checks whether the reachability poset forms a lattice
 * by verifying: (1) a unique top, (2) a unique bottom, (3) all pairwise meets, and
 * (4) all pairwise joins exist.
 *
 * <p>Ordering convention: {@code s1 >= s2} iff there is a directed path from s1 to s2.
 * Top = initial state, Bottom = end state. Cycles from recursive types are handled
 * by quotienting by strongly connected components (SCCs).
 */
public final class LatticeChecker {

    private LatticeChecker() {}

    /**
     * Check whether the reachability poset of {@code ss} forms a lattice.
     */
    public static LatticeResult checkLattice(StateSpace ss) {
        // Degenerate case: bottom state not reachable (e.g., non-terminating rec X . X)
        if (!ss.states().contains(ss.bottom())) {
            return new LatticeResult(false, false, false, false, false,
                    0, null, Map.of());
        }

        QuotientPoset q = buildQuotientPoset(ss);

        Set<Integer> allNodes = new HashSet<>(q.nodes);

        boolean hasTop = q.fwdReach.get(q.top).equals(allNodes);
        boolean hasBottom = true;
        for (int n : allNodes) {
            if (!q.fwdReach.get(n).contains(q.bottom)) {
                hasBottom = false;
                break;
            }
        }

        boolean allMeets = true;
        boolean allJoins = true;
        LatticeResult.Counterexample counterexample = null;

        List<Integer> nodesList = new ArrayList<>(q.nodes);
        Collections.sort(nodesList);

        outer:
        for (int i = 0; i < nodesList.size(); i++) {
            int a = nodesList.get(i);
            for (int j = i + 1; j < nodesList.size(); j++) {
                int b = nodesList.get(j);
                if (allMeets && meetOnQuotient(q, a, b) == null) {
                    allMeets = false;
                    if (counterexample == null) {
                        counterexample = new LatticeResult.Counterexample(
                                q.rep.get(a), q.rep.get(b), "no_meet");
                    }
                }
                if (allJoins && joinOnQuotient(q, a, b) == null) {
                    allJoins = false;
                    if (counterexample == null) {
                        counterexample = new LatticeResult.Counterexample(
                                q.rep.get(a), q.rep.get(b), "no_join");
                    }
                }
                if (!allMeets && !allJoins) break outer;
            }
        }

        boolean isLattice = hasTop && hasBottom && allMeets && allJoins;

        Map<Integer, Integer> sccMap = new HashMap<>();
        for (var entry : q.stateToNode.entrySet()) {
            sccMap.put(entry.getKey(), q.rep.get(entry.getValue()));
        }

        return new LatticeResult(isLattice, hasTop, hasBottom, allMeets, allJoins,
                q.nodes.size(), counterexample, sccMap);
    }

    /**
     * Compute the meet (greatest lower bound) of states {@code a} and {@code b}.
     *
     * @return a representative original state ID, or {@code null} if no meet exists
     */
    public static Integer computeMeet(StateSpace ss, int a, int b) {
        QuotientPoset q = buildQuotientPoset(ss);
        Integer qa = q.stateToNode.get(a);
        Integer qb = q.stateToNode.get(b);
        if (qa == null || qb == null) return null;
        Integer m = meetOnQuotient(q, qa, qb);
        if (m == null) return null;
        return q.rep.get(m);
    }

    /**
     * Compute the join (least upper bound) of states {@code a} and {@code b}.
     *
     * @return a representative original state ID, or {@code null} if no join exists
     */
    public static Integer computeJoin(StateSpace ss, int a, int b) {
        QuotientPoset q = buildQuotientPoset(ss);
        Integer qa = q.stateToNode.get(a);
        Integer qb = q.stateToNode.get(b);
        if (qa == null || qb == null) return null;
        Integer j = joinOnQuotient(q, qa, qb);
        if (j == null) return null;
        return q.rep.get(j);
    }

    // -- Internal: quotient poset -----------------------------------------------

    private record QuotientPoset(
            Set<Integer> nodes,
            int top,
            int bottom,
            Map<Integer, Set<Integer>> fwdAdj,
            Map<Integer, Set<Integer>> revAdj,
            Map<Integer, Set<Integer>> fwdReach,
            Map<Integer, Set<Integer>> revReach,
            Map<Integer, Integer> rep,
            Map<Integer, Integer> stateToNode) {}

    private static QuotientPoset buildQuotientPoset(StateSpace ss) {
        // Step 1: Compute adjacency and SCCs
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }

        List<Set<Integer>> sccs = computeSccs(ss.states(), adj);

        // Map each original state to its SCC index
        Map<Integer, Integer> stateToScc = new HashMap<>();
        Map<Integer, Integer> rep = new HashMap<>();
        for (int idx = 0; idx < sccs.size(); idx++) {
            Set<Integer> scc = sccs.get(idx);
            int r = Collections.min(scc);
            rep.put(idx, r);
            for (int s : scc) {
                stateToScc.put(s, idx);
            }
        }

        Set<Integer> nodes = new HashSet<>();
        for (int i = 0; i < sccs.size(); i++) {
            nodes.add(i);
        }

        // Step 2: Build quotient DAG edges
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

        // Step 3: Compute reachability via topological-order DP
        List<Integer> topoOrder = topologicalSort(nodes, fwdAdj);

        // Forward reachability: process sinks first (reverse topo order)
        Map<Integer, Set<Integer>> fwdReach = new HashMap<>();
        for (int n : nodes) {
            fwdReach.put(n, new HashSet<>(Set.of(n)));
        }
        for (int i = topoOrder.size() - 1; i >= 0; i--) {
            int n = topoOrder.get(i);
            for (int succ : fwdAdj.get(n)) {
                fwdReach.get(n).addAll(fwdReach.get(succ));
            }
        }

        // Reverse reachability: process sources first (topo order)
        Map<Integer, Set<Integer>> revReach = new HashMap<>();
        for (int n : nodes) {
            revReach.put(n, new HashSet<>(Set.of(n)));
        }
        for (int n : topoOrder) {
            for (int pred : revAdj.get(n)) {
                revReach.get(n).addAll(revReach.get(pred));
            }
        }

        int qTop = stateToScc.get(ss.top());
        int qBottom = stateToScc.get(ss.bottom());

        return new QuotientPoset(nodes, qTop, qBottom, fwdAdj, revAdj,
                fwdReach, revReach, rep, stateToScc);
    }

    // -- Internal: SCC computation (iterative Tarjan's algorithm) ----------------

    private static List<Set<Integer>> computeSccs(
            Set<Integer> states, Map<Integer, List<Integer>> adj) {

        int[] indexCounter = {0};
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> onStack = new HashSet<>();
        Map<Integer, Integer> index = new HashMap<>();
        Map<Integer, Integer> lowlink = new HashMap<>();
        List<Set<Integer>> result = new ArrayList<>();

        List<Integer> sortedStates = new ArrayList<>(states);
        Collections.sort(sortedStates);

        for (int start : sortedStates) {
            if (index.containsKey(start)) continue;

            // Iterative DFS using explicit call stack
            Deque<int[]> callStack = new ArrayDeque<>();
            index.put(start, indexCounter[0]);
            lowlink.put(start, indexCounter[0]);
            indexCounter[0]++;
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
                        index.put(w, indexCounter[0]);
                        lowlink.put(w, indexCounter[0]);
                        indexCounter[0]++;
                        stack.push(w);
                        onStack.add(w);
                        callStack.push(new int[]{w, 0});
                    } else if (onStack.contains(w)) {
                        lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
                    }
                } else {
                    if (lowlink.get(v).equals(index.get(v))) {
                        Set<Integer> sccMembers = new HashSet<>();
                        while (true) {
                            int w = stack.pop();
                            onStack.remove(w);
                            sccMembers.add(w);
                            if (w == v) break;
                        }
                        result.add(sccMembers);
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

    // -- Internal: topological sort (Kahn's algorithm) ---------------------------

    private static List<Integer> topologicalSort(
            Set<Integer> nodes, Map<Integer, Set<Integer>> fwdAdj) {

        Map<Integer, Integer> inDegree = new HashMap<>();
        for (int n : nodes) {
            inDegree.put(n, 0);
        }
        for (int n : nodes) {
            for (int m : fwdAdj.get(n)) {
                inDegree.merge(m, 1, Integer::sum);
            }
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
                if (inDegree.get(m) == 0) {
                    queue.add(m);
                }
            }
        }

        return order;
    }

    // -- Internal: meet and join on the quotient DAG -----------------------------

    private static Integer meetOnQuotient(QuotientPoset q, int a, int b) {
        if (a == b) return a;

        // Lower bounds = nodes reachable from both a and b
        Set<Integer> lowerBounds = new HashSet<>(q.fwdReach.get(a));
        lowerBounds.retainAll(q.fwdReach.get(b));
        if (lowerBounds.isEmpty()) return null;

        // Greatest lower bound: reaches all other lower bounds
        for (int m : lowerBounds) {
            if (q.fwdReach.get(m).containsAll(lowerBounds)) {
                return m;
            }
        }
        return null;
    }

    private static Integer joinOnQuotient(QuotientPoset q, int a, int b) {
        if (a == b) return a;

        // Upper bounds = nodes that can reach both a and b
        Set<Integer> upperBounds = new HashSet<>(q.revReach.get(a));
        upperBounds.retainAll(q.revReach.get(b));
        if (upperBounds.isEmpty()) return null;

        // Least upper bound: reachable from all other upper bounds
        for (int j : upperBounds) {
            if (q.revReach.get(j).containsAll(upperBounds)) {
                return j;
            }
        }
        return null;
    }
}
