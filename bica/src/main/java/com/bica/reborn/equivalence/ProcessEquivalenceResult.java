package com.bica.reborn.equivalence;

/**
 * Result of comparing three process equivalences on a pair of state spaces.
 *
 * <p>For session type state spaces (deterministic, finite, lattice-structured),
 * CCS strong bisimulation, CSP trace equivalence, and CSP failure equivalence
 * all coincide. This record captures each verdict and whether they agree.
 *
 * @param bisimilar         verdict from CCS strong bisimulation
 * @param traceEquivalent   verdict from CSP trace equivalence
 * @param failureEquivalent verdict from CSP failure equivalence
 * @param allAgree          true iff all three verdicts are the same
 * @param ss1Deterministic  true iff the first state space is deterministic
 * @param ss2Deterministic  true iff the second state space is deterministic
 * @param details           human-readable summary
 */
public record ProcessEquivalenceResult(
        boolean bisimilar,
        boolean traceEquivalent,
        boolean failureEquivalent,
        boolean allAgree,
        boolean ss1Deterministic,
        boolean ss2Deterministic,
        String details) {

    public ProcessEquivalenceResult {
        java.util.Objects.requireNonNull(details, "details must not be null");
    }
}
