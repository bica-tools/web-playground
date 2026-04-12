package com.bica.reborn.equalizer;

import com.bica.reborn.statespace.StateSpace;

import java.util.Map;
import java.util.Objects;

/**
 * Result of equalizer analysis for a pair of morphisms.
 *
 * <p>An equalizer of f, g: L → R is a lattice E with a morphism equ: E → L
 * such that f ∘ equ = g ∘ equ, and E is universal.
 *
 * @param hasEqualizer              True iff the equalizer exists.
 * @param equalizerSpace            The equalizer state space, or null.
 * @param inclusion                 The inclusion map E → L, or null.
 * @param fMapping                  The mapping f.
 * @param gMapping                  The mapping g.
 * @param universalPropertyHolds    True iff the universal property was verified.
 * @param counterexample            Description of failure, or null.
 */
public record EqualizerResult(
        boolean hasEqualizer,
        StateSpace equalizerSpace,
        Map<Integer, Integer> inclusion,
        Map<Integer, Integer> fMapping,
        Map<Integer, Integer> gMapping,
        boolean universalPropertyHolds,
        String counterexample) {

    public EqualizerResult {
        Objects.requireNonNull(fMapping);
        Objects.requireNonNull(gMapping);
        fMapping = Map.copyOf(fMapping);
        gMapping = Map.copyOf(gMapping);
        if (inclusion != null) inclusion = Map.copyOf(inclusion);
    }

    /**
     * The kernel pair of a morphism f: {(a,b) ∈ L×L : f(a)=f(b)}.
     */
    public record KernelPair(
            java.util.Set<long[]> pairs,
            boolean isCongruence) {}

    /**
     * Result of checking finite completeness of SessLat.
     */
    public record FiniteCompletenessResult(
            boolean hasProducts,
            boolean hasEqualizers,
            boolean isFinitelyComplete,
            int numSpacesTested,
            String counterexample) {}
}
