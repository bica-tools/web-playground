package com.bica.reborn.extensions;

import com.bica.reborn.statespace.ProductStateSpace;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Bounded replication analysis: S^n -- n copies of S in parallel.
 *
 * <p>Semantics: {@code L(S^n) = L(S)^n} (n-fold product lattice).
 * Provides diagonal state identification and S_n symmetry orbit counting.
 */
public final class ReplicationChecker {

    private ReplicationChecker() {}

    /**
     * Build the n-fold product state space from a base state space.
     *
     * <p>Applies {@link ProductStateSpace#product} iteratively:
     * {@code L(S^1) = L(S)}, {@code L(S^k) = L(S^{k-1}) x L(S)}.
     *
     * @param base the base state space L(S)
     * @param n    replication count (must be >= 1)
     * @return the n-fold product state space
     * @throws IllegalArgumentException if n < 1
     */
    public static StateSpace buildReplicatedStateSpace(StateSpace base, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Replication count must be >= 1, got " + n);
        }
        StateSpace result = base;
        for (int i = 1; i < n; i++) {
            result = ProductStateSpace.product(result, base);
        }
        return result;
    }

    /**
     * Identify diagonal states: those where all n components are equal.
     *
     * <p>In a product lattice L(S)^n, a state (s, s, ..., s) is on the
     * diagonal. Since our product construction labels states as
     * {@code "(label1, label2)"}, we parse these recursively to find
     * states where all leaf components are identical.
     *
     * @param product the n-fold product state space
     * @param base    the base state space L(S)
     * @param n       number of factors
     * @return set of coordinate lists for diagonal states
     */
    public static Set<List<Integer>> diagonalStates(StateSpace product, StateSpace base, int n) {
        if (n <= 1) {
            // Every state is trivially diagonal
            return product.states().stream()
                    .map(List::of)
                    .collect(Collectors.toSet());
        }

        // Parse product labels to extract coordinates
        Map<Integer, List<String>> stateCoords = extractCoordinates(product, n);
        Set<List<Integer>> result = new HashSet<>();

        for (var entry : stateCoords.entrySet()) {
            List<String> coords = entry.getValue();
            if (coords.size() == n && new HashSet<>(coords).size() == 1) {
                // All components equal => diagonal state
                result.add(List.of(entry.getKey()));
            }
        }
        return result;
    }

    /**
     * Count the number of distinct S_n symmetry orbits.
     *
     * <p>Two states are in the same orbit if one can be obtained from
     * the other by permuting the n factors. The orbit count measures
     * how much symmetry can compress the state space.
     *
     * @param product the n-fold product state space
     * @param base    the base state space L(S)
     * @param n       number of factors
     * @return number of distinct orbits
     */
    public static int symmetryOrbits(StateSpace product, StateSpace base, int n) {
        if (n <= 1) {
            return product.states().size();
        }

        Map<Integer, List<String>> stateCoords = extractCoordinates(product, n);
        Set<List<String>> canonicalForms = new HashSet<>();

        for (var entry : stateCoords.entrySet()) {
            List<String> coords = entry.getValue();
            if (coords.size() == n) {
                // Canonical form: sorted coordinates (since S_n acts by permutation)
                List<String> sorted = new ArrayList<>(coords);
                Collections.sort(sorted);
                canonicalForms.add(sorted);
            }
        }

        // States without parseable coordinates are each their own orbit
        int unparsed = product.states().size() - stateCoords.size();
        return canonicalForms.size() + unparsed;
    }

    /**
     * Perform full replication analysis for a base state space replicated n times.
     *
     * @param base the base state space L(S)
     * @param n    replication count (must be >= 1)
     * @return complete analysis result
     */
    public static ReplicationResult analyzeReplication(StateSpace base, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Replication count must be >= 1, got " + n);
        }

        StateSpace replicated = buildReplicatedStateSpace(base, n);
        Set<List<Integer>> diag = diagonalStates(replicated, base, n);
        int orbits = symmetryOrbits(replicated, base, n);
        double reduction = orbits > 0
                ? (double) replicated.states().size() / orbits
                : 1.0;

        return new ReplicationResult(
                replicated,
                base.states().size(),
                replicated.states().size(),
                n,
                diag,
                orbits,
                reduction);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Extract n-component coordinates from product state labels.
     *
     * <p>Product labels are nested like {@code "((a, b), c)"} for a 3-fold product
     * built left-to-right. We flatten them to {@code ["a", "b", "c"]}.
     */
    static Map<Integer, List<String>> extractCoordinates(StateSpace product, int n) {
        Map<Integer, List<String>> result = new HashMap<>();
        for (int sid : product.states()) {
            String label = product.labels().getOrDefault(sid, String.valueOf(sid));
            List<String> flat = flattenLabel(label);
            if (flat.size() == n) {
                result.put(sid, flat);
            }
        }
        return result;
    }

    /**
     * Flatten a nested product label like {@code "((a, b), c)"} into
     * leaf components {@code ["a", "b", "c"]}.
     */
    static List<String> flattenLabel(String label) {
        label = label.trim();
        if (label.startsWith("(") && label.endsWith(")")) {
            // Split at the top-level comma
            String inner = label.substring(1, label.length() - 1).trim();
            int splitPos = findTopLevelComma(inner);
            if (splitPos < 0) {
                // No comma found -- single element in parens
                return flattenLabel(inner);
            }
            String leftPart = inner.substring(0, splitPos).trim();
            String rightPart = inner.substring(splitPos + 1).trim();
            List<String> result = new ArrayList<>();
            result.addAll(flattenLabel(leftPart));
            result.addAll(flattenLabel(rightPart));
            return result;
        }
        // Leaf label
        return List.of(label);
    }

    /**
     * Find the position of the top-level comma in a string,
     * respecting nested parentheses.
     */
    private static int findTopLevelComma(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(') depth++;
            else if (ch == ')') depth--;
            else if (ch == ',' && depth == 0) return i;
        }
        return -1;
    }
}
