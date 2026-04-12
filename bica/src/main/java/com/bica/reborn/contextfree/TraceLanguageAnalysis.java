package com.bica.reborn.contextfree;

import java.util.Objects;

/**
 * Trace language properties of a session type.
 *
 * @param chomskyClass     "regular" or "context-free"
 * @param isFinite         true if trace set is finite (no recursion)
 * @param isRegular        true if trace language is regular
 * @param usesRecursion    true if type has rec binders
 * @param usesParallel     true if type has parallel constructor
 * @param usesSequence     true if type has sequence constructor
 * @param stateCount       number of states in state space
 * @param transitionCount  number of transitions
 */
public record TraceLanguageAnalysis(
        String chomskyClass,
        boolean isFinite,
        boolean isRegular,
        boolean usesRecursion,
        boolean usesParallel,
        boolean usesSequence,
        int stateCount,
        int transitionCount) {

    public TraceLanguageAnalysis {
        Objects.requireNonNull(chomskyClass, "chomskyClass must not be null");
    }
}
