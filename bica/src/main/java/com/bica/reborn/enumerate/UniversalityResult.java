package com.bica.reborn.enumerate;

import com.bica.reborn.lattice.LatticeResult;

import java.util.List;
import java.util.Objects;

/**
 * Result of universality checking: is L(S) a lattice for all enumerated types?
 */
public record UniversalityResult(
        int totalTypes,
        int totalLattices,
        List<Counterexample> counterexamples,
        EnumerationConfig config) {

    public record Counterexample(String typeString, LatticeResult latticeResult) {}

    public UniversalityResult {
        Objects.requireNonNull(counterexamples);
        counterexamples = List.copyOf(counterexamples);
    }

    /** True iff no counterexamples were found. */
    public boolean isUniversal() {
        return counterexamples.isEmpty();
    }
}
