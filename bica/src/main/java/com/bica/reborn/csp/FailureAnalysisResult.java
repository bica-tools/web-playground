package com.bica.reborn.csp;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Complete failure analysis result for a state space.
 *
 * @param refusalMap           mapping from state ID to the set of refused labels
 * @param acceptanceMap        mapping from state ID to the set of accepted labels
 * @param deterministic        true iff each (state, label) has at most one successor
 * @param latticeDetermines    true iff lattice meet/join determine failure structure
 * @param details              human-readable description of the lattice-determines check
 */
public record FailureAnalysisResult(
        Map<Integer, Set<String>> refusalMap,
        Map<Integer, Set<String>> acceptanceMap,
        boolean deterministic,
        boolean latticeDetermines,
        String details) {

    public FailureAnalysisResult {
        Objects.requireNonNull(refusalMap, "refusalMap must not be null");
        Objects.requireNonNull(acceptanceMap, "acceptanceMap must not be null");
        Objects.requireNonNull(details, "details must not be null");
        refusalMap = Map.copyOf(refusalMap);
        acceptanceMap = Map.copyOf(acceptanceMap);
    }
}
