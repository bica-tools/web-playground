package com.bica.reborn.coverage;

import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.Objects;
import java.util.Set;

/**
 * Coverage analysis of a state space against enumerated test paths.
 */
public record CoverageResult(
        Set<Transition> coveredTransitions,
        Set<Transition> uncoveredTransitions,
        Set<Integer> coveredStates,
        Set<Integer> uncoveredStates,
        double transitionCoverage,
        double stateCoverage) {

    public CoverageResult {
        Objects.requireNonNull(coveredTransitions);
        Objects.requireNonNull(uncoveredTransitions);
        Objects.requireNonNull(coveredStates);
        Objects.requireNonNull(uncoveredStates);
        coveredTransitions = Set.copyOf(coveredTransitions);
        uncoveredTransitions = Set.copyOf(uncoveredTransitions);
        coveredStates = Set.copyOf(coveredStates);
        uncoveredStates = Set.copyOf(uncoveredStates);
    }
}
