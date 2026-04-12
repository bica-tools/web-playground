package com.bica.reborn.endomorphism;

import java.util.Objects;

/**
 * Result of checking whether a transition map is a lattice endomorphism.
 *
 * <p>A transition label induces a partial function f_m on the state space.
 * This record captures whether f_m is:
 * <ul>
 *   <li><b>Order-preserving (monotone):</b> s₁ ≥ s₂ ⟹ f(s₁) ≥ f(s₂)</li>
 *   <li><b>Meet-preserving:</b> f(s₁ ∧ s₂) = f(s₁) ∧ f(s₂)</li>
 *   <li><b>Join-preserving:</b> f(s₁ ∨ s₂) = f(s₁) ∨ f(s₂)</li>
 *   <li><b>Endomorphism:</b> meet- AND join-preserving (lattice homomorphism to itself)</li>
 * </ul>
 *
 * @param label               the transition label
 * @param domainSize           number of states where this label is enabled
 * @param isOrderPreserving    true iff monotone
 * @param isMeetPreserving     true iff f(a∧b) = f(a)∧f(b)
 * @param isJoinPreserving     true iff f(a∨b) = f(a)∨f(b)
 * @param isEndomorphism       true iff meet- and join-preserving
 * @param orderCounterexample  pair (a,b) violating monotonicity, or null
 * @param meetCounterexample   pair (a,b) violating meet preservation, or null
 * @param joinCounterexample   pair (a,b) violating join preservation, or null
 */
public record EndomorphismResult(
        String label,
        int domainSize,
        boolean isOrderPreserving,
        boolean isMeetPreserving,
        boolean isJoinPreserving,
        boolean isEndomorphism,
        int[] orderCounterexample,
        int[] meetCounterexample,
        int[] joinCounterexample) {

    public EndomorphismResult {
        Objects.requireNonNull(label, "label must not be null");
    }
}
