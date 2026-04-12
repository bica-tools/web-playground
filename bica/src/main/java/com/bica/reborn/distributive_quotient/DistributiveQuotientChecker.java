package com.bica.reborn.distributive_quotient;

import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Distributive quotients of session type lattices (Step 301).
 *
 * <p>Given a session type whose state space forms a lattice L, compute the
 * smallest congruence Theta such that L/Theta is distributive. Key results:
 *
 * <ul>
 *   <li>M3 (diamond) lattices are simple: no non-trivial distributive quotient exists.</li>
 *   <li>N5 (pentagon) lattices from branch nesting have a unique minimal quotient.</li>
 *   <li>Product lattices from parallel composition are always distributive (no quotient needed).</li>
 *   <li>Entropy loss = log2(states_before / states_after).</li>
 * </ul>
 *
 * <p>Algorithm overview:
 * <ol>
 *   <li>Check if already distributive -&gt; identity (no quotient needed).</li>
 *   <li>Check if M3-simple -&gt; return impossible.</li>
 *   <li>For small lattices (&lt;=12 states): exhaustive congruence enumeration.</li>
 *   <li>For larger: iterative N5 collapsing heuristic.</li>
 * </ol>
 *
 * <p>Java port of Python {@code reticulate.distributive_quotient}.
 */
public final class DistributiveQuotientChecker {

    private DistributiveQuotientChecker() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Compute the minimal distributive quotient of a lattice.
     */
    public static DistributiveQuotientResult computeDistributiveQuotient(StateSpace ss) {
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) {
            return notLatticeResult(ss);
        }

        DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
        if (dr.isDistributive()) {
            // Already distributive: identity quotient
            List<Set<Integer>> partition = identityPartition(ss);
            List<CongruenceClass> classes = partitionToClasses(ss, partition);
            return new DistributiveQuotientResult(
                    ss.states().size(),
                    ss.states().size(),
                    true, true,
                    classes,
                    List.of(),
                    log2Safe(ss.states().size()),
                    log2Safe(ss.states().size()),
                    0.0,
                    ss,
                    1,
                    "Already distributive; no quotient needed.");
        }

        // Non-distributive lattice: try to find a distributive quotient.
        if (dr.hasM3() && !dr.hasN5()) {
            int nStates = ss.states().size();
            if (nStates <= 5) {
                return impossibleResult(ss,
                        "M3 (diamond) lattice is simple; no non-trivial distributive quotient exists.");
            }
        }

        int nStates = ss.states().size();

        if (nStates <= 12) {
            DistributiveQuotientResult exhaustive = exhaustiveDistributiveQuotient(ss);
            if (exhaustive != null) {
                return exhaustive;
            }
            return impossibleResult(ss,
                    "No non-trivial distributive quotient exists (exhaustive search).");
        }

        // For larger lattices: iterative N5 collapsing heuristic
        return iterativeN5Collapse(ss);
    }

    /**
     * Check if a partition is a valid lattice congruence.
     *
     * <p>A partition Theta is a congruence on lattice L iff for all (a, b) in Theta:
     * meet(a, x) Theta meet(b, x) for all x in L, and
     * join(a, x) Theta join(b, x) for all x in L.
     */
    public static boolean isCongruence(StateSpace ss, List<Set<Integer>> partition) {
        // Build lookup: state -> class index
        Map<Integer, Integer> classOf = new HashMap<>();
        for (int idx = 0; idx < partition.size(); idx++) {
            for (int s : partition.get(idx)) {
                classOf.put(s, idx);
            }
        }

        // Check all states are covered
        if (!classOf.keySet().equals(ss.states())) {
            return false;
        }

        List<Integer> statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);

        for (Set<Integer> block : partition) {
            if (block.size() <= 1) continue;
            List<Integer> members = new ArrayList<>(block);
            Collections.sort(members);

            for (int i = 0; i < members.size(); i++) {
                for (int j = i + 1; j < members.size(); j++) {
                    int a = members.get(i);
                    int b = members.get(j);
                    for (int x : statesList) {
                        // Check meet substitution
                        Integer ma = LatticeChecker.computeMeet(ss, a, x);
                        Integer mb = LatticeChecker.computeMeet(ss, b, x);
                        if (ma != null && mb != null) {
                            Integer cma = classOf.get(ma);
                            Integer cmb = classOf.get(mb);
                            if (cma == null || cmb == null || !cma.equals(cmb)) {
                                return false;
                            }
                        }

                        // Check join substitution
                        Integer ja = LatticeChecker.computeJoin(ss, a, x);
                        Integer jb = LatticeChecker.computeJoin(ss, b, x);
                        if (ja != null && jb != null) {
                            Integer cja = classOf.get(ja);
                            Integer cjb = classOf.get(jb);
                            if (cja == null || cjb == null || !cja.equals(cjb)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Build quotient state space from a congruence partition.
     *
     * <p>Each congruence class becomes a single state. Transitions are induced
     * from the original: if (a, label, b) is a transition and a is in class C1,
     * b is in class C2, then (C1_rep, label, C2_rep) is a quotient transition.
     */
    public static StateSpace buildQuotientStateSpace(StateSpace ss, List<Set<Integer>> partition) {
        // Map each state to its class representative (min element)
        Map<Integer, Integer> stateToRep = new HashMap<>();
        for (Set<Integer> block : partition) {
            int rep = Collections.min(block);
            for (int s : block) {
                stateToRep.put(s, rep);
            }
        }

        Set<Integer> newStates = new HashSet<>();
        for (Set<Integer> block : partition) {
            newStates.add(Collections.min(block));
        }
        int newTop = stateToRep.get(ss.top());
        int newBottom = stateToRep.get(ss.bottom());

        // Build transitions (deduplicated)
        Set<String> seenTransitions = new HashSet<>();
        List<StateSpace.Transition> newTransitions = new ArrayList<>();

        for (var t : ss.transitions()) {
            int newSrc = stateToRep.get(t.source());
            int newTgt = stateToRep.get(t.target());
            String key = newSrc + ":" + t.label() + ":" + newTgt;
            if (!seenTransitions.contains(key)) {
                seenTransitions.add(key);
                newTransitions.add(new StateSpace.Transition(
                        newSrc, t.label(), newTgt, t.kind()));
            }
        }

        // Build labels
        Map<Integer, String> newLabels = new HashMap<>();
        for (Set<Integer> block : partition) {
            int rep = Collections.min(block);
            if (block.size() == 1) {
                newLabels.put(rep, ss.labels().getOrDefault(rep, String.valueOf(rep)));
            } else {
                String combined = block.stream()
                        .sorted()
                        .map(s -> ss.labels().getOrDefault(s, String.valueOf(s)))
                        .collect(Collectors.joining(",", "{", "}"));
                newLabels.put(rep, combined);
            }
        }

        return new StateSpace(newStates, newTransitions, newTop, newBottom, newLabels);
    }

    /**
     * Count the number of minimal distributive quotients (for small lattices).
     *
     * @return 0 if no distributive quotient exists, 1 if unique, &gt;1 if multiple
     */
    public static int countMinimalQuotients(StateSpace ss) {
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) return 0;

        DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
        if (dr.isDistributive()) return 1;

        int nStates = ss.states().size();
        if (nStates > 12) {
            DistributiveQuotientResult result = computeDistributiveQuotient(ss);
            return result.numMinimalQuotients();
        }

        List<List<Set<Integer>>> distCongruences = findAllDistributiveCongruences(ss);
        if (distCongruences.isEmpty()) return 0;

        int maxClasses = distCongruences.stream()
                .mapToInt(List::size)
                .max().orElse(0);
        return (int) distCongruences.stream()
                .filter(p -> p.size() == maxClasses)
                .count();
    }

    /**
     * Direct check: a ^ (b v c) = (a ^ b) v (a ^ c) for all triples.
     *
     * <p>This is the ground-truth distributivity check (slower but correct).
     */
    public static boolean directDistributivityCheck(StateSpace ss) {
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) return false;

        List<Integer> statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);

        for (int a : statesList) {
            for (int b : statesList) {
                for (int c : statesList) {
                    Integer bJoinC = LatticeChecker.computeJoin(ss, b, c);
                    if (bJoinC == null) return false;
                    Integer lhs = LatticeChecker.computeMeet(ss, a, bJoinC);
                    if (lhs == null) return false;

                    Integer aMeetB = LatticeChecker.computeMeet(ss, a, b);
                    Integer aMeetC = LatticeChecker.computeMeet(ss, a, c);
                    if (aMeetB == null || aMeetC == null) return false;
                    Integer rhs = LatticeChecker.computeJoin(ss, aMeetB, aMeetC);
                    if (rhs == null) return false;

                    if (!lhs.equals(rhs)) return false;
                }
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    static double log2Safe(int n) {
        if (n <= 0) return 0.0;
        return Math.log(n) / Math.log(2);
    }

    private static List<Set<Integer>> identityPartition(StateSpace ss) {
        return ss.states().stream()
                .sorted()
                .map(s -> (Set<Integer>) Set.of(s))
                .collect(Collectors.toList());
    }

    private static List<CongruenceClass> partitionToClasses(
            StateSpace ss, List<Set<Integer>> partition) {
        List<CongruenceClass> classes = new ArrayList<>();
        for (Set<Integer> block : partition) {
            int rep = Collections.min(block);
            Set<String> labels = new HashSet<>();
            for (var t : ss.transitions()) {
                if (block.contains(t.source()) && block.contains(t.target())) {
                    labels.add(t.label());
                }
            }
            classes.add(new CongruenceClass(rep, block, labels));
        }
        return classes;
    }

    private static List<int[]> mergedPairsFromPartition(List<Set<Integer>> partition) {
        List<int[]> pairs = new ArrayList<>();
        for (Set<Integer> block : partition) {
            if (block.size() <= 1) continue;
            List<Integer> members = new ArrayList<>(block);
            Collections.sort(members);
            for (int i = 0; i < members.size(); i++) {
                for (int j = i + 1; j < members.size(); j++) {
                    pairs.add(new int[]{members.get(i), members.get(j)});
                }
            }
        }
        return pairs;
    }

    /**
     * Generate all partitions of a set of elements.
     * Only feasible for small sets (&lt;=12 elements).
     */
    static List<List<Set<Integer>>> generatePartitions(List<Integer> elements) {
        if (elements.isEmpty()) {
            List<List<Set<Integer>>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }
        if (elements.size() == 1) {
            List<List<Set<Integer>>> result = new ArrayList<>();
            List<Set<Integer>> single = new ArrayList<>();
            single.add(new HashSet<>(elements));
            result.add(single);
            return result;
        }

        int first = elements.get(0);
        List<Integer> rest = elements.subList(1, elements.size());
        List<List<Set<Integer>>> restPartitions = generatePartitions(rest);

        List<List<Set<Integer>>> result = new ArrayList<>();
        for (List<Set<Integer>> part : restPartitions) {
            // Option 1: first goes into its own singleton block
            List<Set<Integer>> withSingleton = new ArrayList<>();
            withSingleton.add(new HashSet<>(Set.of(first)));
            withSingleton.addAll(part);
            result.add(withSingleton);

            // Option 2: first is added to each existing block
            for (int i = 0; i < part.size(); i++) {
                List<Set<Integer>> newPart = new ArrayList<>();
                for (int k = 0; k < part.size(); k++) {
                    if (k == i) {
                        Set<Integer> expanded = new HashSet<>(part.get(k));
                        expanded.add(first);
                        newPart.add(expanded);
                    } else {
                        newPart.add(new HashSet<>(part.get(k)));
                    }
                }
                result.add(newPart);
            }
        }
        return result;
    }

    private static List<List<Set<Integer>>> findAllDistributiveCongruences(StateSpace ss) {
        List<Integer> statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);
        List<List<Set<Integer>>> allPartitions = generatePartitions(statesList);

        List<List<Set<Integer>>> distCongruences = new ArrayList<>();
        for (List<Set<Integer>> partition : allPartitions) {
            // Skip trivial all-in-one partition
            if (partition.size() == 1 && statesList.size() > 1) continue;
            // Skip identity partition
            if (partition.stream().allMatch(b -> b.size() == 1)) continue;
            if (!isCongruence(ss, partition)) continue;
            // Build quotient and check distributivity
            StateSpace quotient = buildQuotientStateSpace(ss, partition);
            if (directDistributivityCheck(quotient)) {
                distCongruences.add(partition);
            }
        }
        return distCongruences;
    }

    private static DistributiveQuotientResult exhaustiveDistributiveQuotient(StateSpace ss) {
        List<List<Set<Integer>>> distCongruences = findAllDistributiveCongruences(ss);
        if (distCongruences.isEmpty()) return null;

        int maxClasses = distCongruences.stream()
                .mapToInt(List::size)
                .max().orElse(0);
        List<List<Set<Integer>>> minimal = distCongruences.stream()
                .filter(p -> p.size() == maxClasses)
                .collect(Collectors.toList());
        List<Set<Integer>> best = minimal.get(0);

        StateSpace quotient = buildQuotientStateSpace(ss, best);
        List<CongruenceClass> classes = partitionToClasses(ss, best);
        List<int[]> merged = mergedPairsFromPartition(best);
        int nOrig = ss.states().size();
        int nQuot = quotient.states().size();

        return new DistributiveQuotientResult(
                nOrig, nQuot,
                false, true,
                classes, merged,
                log2Safe(nOrig), log2Safe(nQuot),
                log2Safe(nOrig) - log2Safe(nQuot),
                quotient,
                minimal.size(),
                String.format("Found %d distributive congruence(s); %d minimal (with %d classes).",
                        distCongruences.size(), minimal.size(), maxClasses));
    }

    private static DistributiveQuotientResult iterativeN5Collapse(StateSpace ss) {
        int nOrig = ss.states().size();
        StateSpace current = ss;
        List<int[]> allMerged = new ArrayList<>();

        for (int iter = 0; iter < nOrig; iter++) {
            DistributivityResult dr = DistributivityChecker.checkDistributive(current);
            if (dr.isDistributive()) break;
            if (!dr.isLattice()) {
                return impossibleResult(ss,
                        "N5 collapse broke lattice structure; no quotient possible.");
            }

            int mergeA, mergeB;
            if (dr.n5Witness() != null) {
                // N5: (top, a, b, c, bot) -- merge b and c
                mergeA = dr.n5Witness().get(2);
                mergeB = dr.n5Witness().get(3);
            } else if (dr.m3Witness() != null) {
                // M3: (top, a, b, c, bot) -- merge a and b
                mergeA = dr.m3Witness().get(1);
                mergeB = dr.m3Witness().get(2);
            } else {
                break;
            }

            // Build partition that merges these two
            List<Integer> statesList = new ArrayList<>(current.states());
            Collections.sort(statesList);
            List<Set<Integer>> newPartition = new ArrayList<>();
            Set<Integer> mergedBlock = new HashSet<>();
            Set<Integer> used = new HashSet<>();

            for (int s : statesList) {
                if (used.contains(s)) continue;
                if (s == mergeA || s == mergeB) {
                    mergedBlock.add(mergeA);
                    mergedBlock.add(mergeB);
                    used.add(mergeA);
                    used.add(mergeB);
                } else {
                    newPartition.add(Set.of(s));
                    used.add(s);
                }
            }
            if (!mergedBlock.isEmpty()) {
                newPartition.add(Set.copyOf(mergedBlock));
            }

            // Check if valid congruence
            if (!isCongruence(current, newPartition)) {
                List<Set<Integer>> closure = congruenceClosure(current, mergeA, mergeB);
                if (closure == null || !isCongruence(current, closure)) {
                    return impossibleResult(ss,
                            "Cannot form a valid congruence by N5 collapse.");
                }
                newPartition = closure;
            }

            allMerged.add(new int[]{mergeA, mergeB});
            current = buildQuotientStateSpace(current, newPartition);
        }

        // Check final result
        DistributivityResult finalDr = DistributivityChecker.checkDistributive(current);
        if (finalDr.isDistributive()) {
            List<Set<Integer>> finalPartition = reconstructPartition(ss, allMerged);
            List<CongruenceClass> classes = partitionToClasses(ss, finalPartition);
            int nQuot = current.states().size();
            return new DistributiveQuotientResult(
                    nOrig, nQuot,
                    false, true,
                    classes, allMerged,
                    log2Safe(nOrig), log2Safe(nQuot),
                    log2Safe(nOrig) - log2Safe(nQuot),
                    current,
                    1,
                    String.format("Iterative N5 collapse: %d merge(s).", allMerged.size()));
        }

        return impossibleResult(ss,
                "Iterative N5 collapse failed to produce a distributive quotient.");
    }

    private static List<Set<Integer>> congruenceClosure(StateSpace ss, int a, int b) {
        // Union-Find
        Map<Integer, Integer> parent = new HashMap<>();
        for (int s : ss.states()) parent.put(s, s);

        List<Integer> statesList = new ArrayList<>(ss.states());
        Collections.sort(statesList);

        union(parent, a, b);

        boolean changed = true;
        int maxIters = ss.states().size() * ss.states().size();
        int iters = 0;
        while (changed && iters < maxIters) {
            changed = false;
            iters++;
            for (int s1 : statesList) {
                for (int s2 : statesList) {
                    if (find(parent, s1) != find(parent, s2)) continue;
                    if (s1 >= s2) continue;
                    for (int x : statesList) {
                        Integer m1 = LatticeChecker.computeMeet(ss, s1, x);
                        Integer m2 = LatticeChecker.computeMeet(ss, s2, x);
                        if (m1 != null && m2 != null && find(parent, m1) != find(parent, m2)) {
                            union(parent, m1, m2);
                            changed = true;
                        }
                        Integer j1 = LatticeChecker.computeJoin(ss, s1, x);
                        Integer j2 = LatticeChecker.computeJoin(ss, s2, x);
                        if (j1 != null && j2 != null && find(parent, j1) != find(parent, j2)) {
                            union(parent, j1, j2);
                            changed = true;
                        }
                    }
                }
            }
        }

        // Check if everything collapsed to one class
        Set<Integer> roots = new HashSet<>();
        for (int s : ss.states()) roots.add(find(parent, s));
        if (roots.size() == 1) return null;

        // Build partition
        Map<Integer, Set<Integer>> groups = new HashMap<>();
        for (int s : ss.states()) {
            int r = find(parent, s);
            groups.computeIfAbsent(r, k -> new HashSet<>()).add(s);
        }
        return new ArrayList<>(groups.values());
    }

    private static List<Set<Integer>> reconstructPartition(
            StateSpace ss, List<int[]> mergedPairs) {
        Map<Integer, Integer> parent = new HashMap<>();
        for (int s : ss.states()) parent.put(s, s);

        for (int[] pair : mergedPairs) {
            if (parent.containsKey(pair[0]) && parent.containsKey(pair[1])) {
                union(parent, pair[0], pair[1]);
            }
        }

        Map<Integer, Set<Integer>> groups = new HashMap<>();
        for (int s : ss.states()) {
            int r = find(parent, s);
            groups.computeIfAbsent(r, k -> new HashSet<>()).add(s);
        }
        return new ArrayList<>(groups.values());
    }

    // Union-Find helpers
    private static int find(Map<Integer, Integer> parent, int x) {
        while (parent.get(x) != x) {
            parent.put(x, parent.get(parent.get(x)));
            x = parent.get(x);
        }
        return x;
    }

    private static void union(Map<Integer, Integer> parent, int x, int y) {
        int rx = find(parent, x);
        int ry = find(parent, y);
        if (rx != ry) {
            parent.put(Math.max(rx, ry), Math.min(rx, ry));
        }
    }

    // Result constructors
    private static DistributiveQuotientResult notLatticeResult(StateSpace ss) {
        int n = ss.states().size();
        return new DistributiveQuotientResult(
                n, n, false, false,
                List.of(), List.of(),
                log2Safe(n), log2Safe(n), 0.0,
                null, 0,
                "Not a lattice; distributive quotient undefined.");
    }

    private static DistributiveQuotientResult impossibleResult(StateSpace ss, String explanation) {
        int n = ss.states().size();
        return new DistributiveQuotientResult(
                n, n, false, false,
                List.of(), List.of(),
                log2Safe(n), log2Safe(n), 0.0,
                null, 0, explanation);
    }
}
