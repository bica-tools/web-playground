package com.bica.reborn.monoidal;

import java.util.Map;
import java.util.Objects;

/**
 * Aggregate result of all monoidal structure checks.
 *
 * @param unit           UnitResult for End as the monoidal unit.
 * @param associativity  AssociativityResult for parallel composition.
 * @param symmetry       SymmetryResult for braiding.
 * @param coherence      CoherenceResult for Mac Lane conditions.
 * @param isMonoidal     True iff all checks pass (symmetric monoidal category).
 */
public record MonoidalResult(
        UnitResult unit,
        AssociativityResult associativity,
        SymmetryResult symmetry,
        CoherenceResult coherence,
        boolean isMonoidal) {

    public MonoidalResult {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(associativity);
        Objects.requireNonNull(symmetry);
        Objects.requireNonNull(coherence);
    }

    public record UnitResult(
            boolean leftUnit,
            boolean rightUnit,
            Map<Integer, Integer> leftIso,
            Map<Integer, Integer> rightIso) {}

    public record AssociativityResult(
            boolean isAssociative,
            Map<Integer, Integer> iso,
            int leftStates,
            int rightStates) {}

    public record SymmetryResult(
            boolean isSymmetric,
            Map<Integer, Integer> iso,
            boolean isInvolution) {}

    public record CoherenceResult(
            boolean pentagon,
            boolean triangle,
            boolean hexagon,
            String counterexample) {}
}
