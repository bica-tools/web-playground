package com.bica.reborn.polarity;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Polarity analysis via Formal Concept Analysis (Birkhoff IV.9-10).
 *
 * <p>Every session type state space has a natural binary relation
 * {@code R ⊆ States × Labels} (state s is related to label m iff m is
 * enabled at s). This relation induces a Galois connection between
 * powersets of states and labels via closure operators, and its fixed
 * points form a concept lattice.
 */
public final class PolarityChecker {

    private PolarityChecker() {}

    // =========================================================================
    // Incidence relation
    // =========================================================================

    /**
     * Build the binary relation {@code R ⊆ States × Labels}.
     *
     * <p>{@code R(s) = {m | transition (s, m, _) exists in ss}}.
     *
     * @return map from state ID to set of enabled labels at that state
     */
    public static Map<Integer, Set<String>> buildIncidenceRelation(StateSpace ss) {
        Map<Integer, Set<String>> relation = new HashMap<>();
        for (int s : ss.states()) {
            relation.put(s, new HashSet<>());
        }
        for (var t : ss.transitions()) {
            relation.get(t.source()).add(t.label());
        }
        return relation;
    }

    // =========================================================================
    // Closure operators
    // =========================================================================

    /**
     * State closure (↓-closure): states enabling ALL given labels.
     *
     * <p>{@code closureStates(B) = {s ∈ States | B ⊆ R(s)}}.
     * For the empty set of labels, returns all states.
     */
    public static Set<Integer> closureStates(StateSpace ss, Set<String> labels) {
        Map<Integer, Set<String>> relation = buildIncidenceRelation(ss);
        return closureStatesInternal(labels, relation);
    }

    /**
     * Label closure (↑-closure): labels enabled at ALL given states.
     *
     * <p>{@code closureLabels(A) = ⋂{R(s) | s ∈ A}}.
     * For the empty set of states, returns all labels.
     */
    public static Set<String> closureLabels(StateSpace ss, Set<Integer> states) {
        Map<Integer, Set<String>> relation = buildIncidenceRelation(ss);
        return closureLabelsInternal(states, relation);
    }

    // =========================================================================
    // Concept computation
    // =========================================================================

    /**
     * Compute all formal concepts (fixed points of the closure operators).
     *
     * <p>A concept is a pair (A, B) where:
     * <ul>
     *   <li>A = closureStates(B) — states enabling all labels in B</li>
     *   <li>B = closureLabels(A) — labels enabled at all states in A</li>
     * </ul>
     */
    public static List<Concept> computeConcepts(StateSpace ss) {
        Map<Integer, Set<String>> relation = buildIncidenceRelation(ss);
        Set<String> allLabels = allLabels(ss);
        List<Concept> concepts = new ArrayList<>();
        Set<Set<Integer>> seenExtents = new HashSet<>();

        if (allLabels.size() <= 20) {
            enumerateByLabels(relation, new ArrayList<>(allLabels), concepts, seenExtents);
        } else {
            enumerateByStates(relation, concepts, seenExtents);
        }

        // Sort concepts by extent size (largest first = top of concept lattice)
        concepts.sort((a, b) -> {
            int cmp = Integer.compare(b.extent().size(), a.extent().size());
            if (cmp != 0) return cmp;
            return a.extent().toString().compareTo(b.extent().toString());
        });
        return concepts;
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Full polarity analysis: build relation, compute concepts, verify Galois connection.
     */
    public static PolarityResult analyzePolarity(StateSpace ss) {
        Map<Integer, Set<String>> relation = buildIncidenceRelation(ss);
        List<Concept> concepts = computeConcepts(ss);
        List<int[]> edges = buildConceptLattice(concepts);
        boolean galois = isGaloisPair(relation, ss);

        return new PolarityResult(relation, concepts, edges, galois, concepts.size());
    }

    // =========================================================================
    // Internal: closure operator helpers
    // =========================================================================

    private static Set<Integer> closureStatesInternal(
            Set<String> labels, Map<Integer, Set<String>> relation) {
        if (labels.isEmpty()) {
            return new HashSet<>(relation.keySet());
        }
        Set<Integer> result = new HashSet<>();
        for (var entry : relation.entrySet()) {
            if (entry.getValue().containsAll(labels)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static Set<String> closureLabelsInternal(
            Set<Integer> states, Map<Integer, Set<String>> relation) {
        if (states.isEmpty()) {
            Set<String> allLabs = new HashSet<>();
            for (Set<String> labs : relation.values()) {
                allLabs.addAll(labs);
            }
            return allLabs;
        }
        Set<String> result = null;
        for (int s : states) {
            Set<String> sLabels = relation.getOrDefault(s, Set.of());
            if (result == null) {
                result = new HashSet<>(sLabels);
            } else {
                result.retainAll(sLabels);
            }
        }
        return result != null ? result : Set.of();
    }

    // =========================================================================
    // Internal: concept enumeration
    // =========================================================================

    private static void enumerateByLabels(
            Map<Integer, Set<String>> relation,
            List<String> labelList,
            List<Concept> concepts,
            Set<Set<Integer>> seenExtents) {

        int n = labelList.size();
        for (int mask = 0; mask < (1 << n); mask++) {
            Set<String> labelSubset = new HashSet<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    labelSubset.add(labelList.get(i));
                }
            }

            Set<Integer> extent = closureStatesInternal(labelSubset, relation);
            if (seenExtents.contains(extent)) continue;

            Set<String> intent = closureLabelsInternal(extent, relation);
            Set<Integer> extent2 = closureStatesInternal(intent, relation);
            if (extent2.equals(extent)) {
                seenExtents.add(extent);
                concepts.add(new Concept(extent, intent));
            }
        }

        // Also add the concept from closing the empty label set
        Set<Integer> extent = closureStatesInternal(Set.of(), relation);
        if (!seenExtents.contains(extent)) {
            Set<String> intent = closureLabelsInternal(extent, relation);
            Set<Integer> extent2 = closureStatesInternal(intent, relation);
            if (extent2.equals(extent)) {
                seenExtents.add(extent);
                concepts.add(new Concept(extent, intent));
            }
        }
    }

    private static void enumerateByStates(
            Map<Integer, Set<String>> relation,
            List<Concept> concepts,
            Set<Set<Integer>> seenExtents) {

        List<Integer> states = new ArrayList<>(relation.keySet());
        Collections.sort(states);

        // Close each individual state
        for (int s : states) {
            Set<String> intent = closureLabelsInternal(Set.of(s), relation);
            Set<Integer> extent = closureStatesInternal(intent, relation);
            if (!seenExtents.contains(extent)) {
                seenExtents.add(extent);
                concepts.add(new Concept(extent, intent));
            }
        }

        // Close all pairs
        for (int i = 0; i < states.size(); i++) {
            for (int j = i + 1; j < states.size(); j++) {
                Set<String> intent = closureLabelsInternal(
                        Set.of(states.get(i), states.get(j)), relation);
                Set<Integer> extent = closureStatesInternal(intent, relation);
                if (!seenExtents.contains(extent)) {
                    seenExtents.add(extent);
                    concepts.add(new Concept(extent, intent));
                }
            }
        }

        // Top concept (all states)
        Set<String> intent = closureLabelsInternal(new HashSet<>(states), relation);
        Set<Integer> extent = closureStatesInternal(intent, relation);
        if (!seenExtents.contains(extent)) {
            seenExtents.add(extent);
            concepts.add(new Concept(extent, intent));
        }

        // Bottom concept
        extent = closureStatesInternal(Set.of(), relation);
        if (!seenExtents.contains(extent)) {
            intent = closureLabelsInternal(extent, relation);
            Set<Integer> extent2 = closureStatesInternal(intent, relation);
            if (extent2.equals(extent)) {
                seenExtents.add(extent);
                concepts.add(new Concept(extent, intent));
            }
        }
    }

    // =========================================================================
    // Internal: concept lattice construction
    // =========================================================================

    private static List<int[]> buildConceptLattice(List<Concept> concepts) {
        int n = concepts.size();
        if (n <= 1) return List.of();

        // Precompute ordering: i ≥ j iff concepts[i].extent ⊇ concepts[j].extent
        List<Set<Integer>> greater = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            greater.add(new HashSet<>());
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && concepts.get(i).extent().containsAll(concepts.get(j).extent())) {
                    greater.get(i).add(j);
                }
            }
        }

        // Covering: i covers j iff i > j and there's no k with i > k > j
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j : greater.get(i)) {
                boolean isCover = true;
                for (int k : greater.get(i)) {
                    if (k != j && greater.get(k).contains(j)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) {
                    edges.add(new int[]{i, j});
                }
            }
        }
        return edges;
    }

    // =========================================================================
    // Internal: Galois pair verification
    // =========================================================================

    private static boolean isGaloisPair(Map<Integer, Set<String>> relation, StateSpace ss) {
        Set<String> allLabels = allLabels(ss);
        List<Integer> states = new ArrayList<>(relation.keySet());

        for (int s : states) {
            Set<String> lcA = closureLabelsInternal(Set.of(s), relation);
            for (String m : allLabels) {
                Set<Integer> scB = closureStatesInternal(Set.of(m), relation);
                boolean lhs = lcA.contains(m);
                boolean rhs = scB.contains(s);
                if (lhs != rhs) return false;
            }
        }
        return true;
    }

    private static Set<String> allLabels(StateSpace ss) {
        return ss.transitions().stream()
                .map(StateSpace.Transition::label)
                .collect(Collectors.toSet());
    }
}
