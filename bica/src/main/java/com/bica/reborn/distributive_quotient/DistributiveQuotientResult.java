package com.bica.reborn.distributive_quotient;

import com.bica.reborn.statespace.StateSpace;

import java.util.List;
import java.util.Objects;

/**
 * Result of computing the distributive quotient of a session type lattice.
 *
 * @param originalStates        number of states in the original lattice
 * @param quotientStates        number of states in the quotient lattice
 * @param isAlreadyDistributive true if the lattice was already distributive
 * @param isQuotientPossible    false for M3 (simple lattice) or when no congruence works
 * @param congruenceClasses     the congruence classes of the quotient
 * @param mergedPairs           pairs of states that were merged
 * @param entropyBefore         log2(originalStates)
 * @param entropyAfter          log2(quotientStates)
 * @param entropyLost           entropyBefore - entropyAfter
 * @param quotientStateSpace    the quotient state space, or null if impossible
 * @param numMinimalQuotients   1=unique, >1=non-unique, 0=impossible
 * @param explanation           human-readable explanation
 */
public record DistributiveQuotientResult(
        int originalStates,
        int quotientStates,
        boolean isAlreadyDistributive,
        boolean isQuotientPossible,
        List<CongruenceClass> congruenceClasses,
        List<int[]> mergedPairs,
        double entropyBefore,
        double entropyAfter,
        double entropyLost,
        StateSpace quotientStateSpace,
        int numMinimalQuotients,
        String explanation) {

    public DistributiveQuotientResult {
        Objects.requireNonNull(congruenceClasses, "congruenceClasses must not be null");
        Objects.requireNonNull(mergedPairs, "mergedPairs must not be null");
        Objects.requireNonNull(explanation, "explanation must not be null");
        congruenceClasses = List.copyOf(congruenceClasses);
        mergedPairs = List.copyOf(mergedPairs);
    }
}
