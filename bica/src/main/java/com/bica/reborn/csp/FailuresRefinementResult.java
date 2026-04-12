package com.bica.reborn.csp;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Result of a failures refinement check between two state spaces.
 *
 * <p>In CSP, {@code P} refines {@code Q} in the failures model iff
 * {@code failures(P) ⊆ failures(Q)}. This is stronger than trace refinement.
 *
 * @param isRefinement          true iff ss1 failures-refines ss2
 * @param tracesRefined         true iff traces(ss1) ⊆ traces(ss2)
 * @param failuresRefined       true iff failures(ss1) ⊆ failures(ss2)
 * @param traceCounterexamples  traces in ss1 not in ss2 (empty if traces refine)
 * @param failureCounterexamples failures in ss1 not in ss2 (empty if failures refine)
 */
public record FailuresRefinementResult(
        boolean isRefinement,
        boolean tracesRefined,
        boolean failuresRefined,
        Set<List<String>> traceCounterexamples,
        Set<Failure> failureCounterexamples) {

    public FailuresRefinementResult {
        Objects.requireNonNull(traceCounterexamples, "traceCounterexamples must not be null");
        Objects.requireNonNull(failureCounterexamples, "failureCounterexamples must not be null");
        traceCounterexamples = Set.copyOf(traceCounterexamples);
        failureCounterexamples = Set.copyOf(failureCounterexamples);
    }
}
