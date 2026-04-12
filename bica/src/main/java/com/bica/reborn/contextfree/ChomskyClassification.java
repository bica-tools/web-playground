package com.bica.reborn.contextfree;

import java.util.Objects;

/**
 * Classification of a session type in the Chomsky hierarchy.
 *
 * @param level                      "regular" or "context-free"
 * @param isRegular                  true iff the type is regular (tail-recursive or non-recursive)
 * @param hasContinuationRecursion   true if recursion occurs inside Sequence left-hand side
 * @param traceLanguageClass         human-readable description
 * @param numRecBinders              number of recursion binders
 * @param maxRecDepth                maximum nesting depth of rec binders
 * @param stackDepthBound            upper bound on needed stack depth (0 for regular)
 */
public record ChomskyClassification(
        String level,
        boolean isRegular,
        boolean hasContinuationRecursion,
        String traceLanguageClass,
        int numRecBinders,
        int maxRecDepth,
        int stackDepthBound) {

    public ChomskyClassification {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(traceLanguageClass, "traceLanguageClass must not be null");
    }
}
