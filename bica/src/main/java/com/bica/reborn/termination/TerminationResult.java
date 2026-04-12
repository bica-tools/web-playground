package com.bica.reborn.termination;

import java.util.List;
import java.util.Objects;

/**
 * Result of a termination check on a session type AST.
 *
 * @param isTerminating      True iff every {@code Rec} node has an exit path.
 * @param nonTerminatingVars Names of recursion variables whose {@code Rec} body
 *                           has no exit path.
 */
public record TerminationResult(boolean isTerminating, List<String> nonTerminatingVars) {

    public TerminationResult {
        Objects.requireNonNull(nonTerminatingVars, "nonTerminatingVars must not be null");
        nonTerminatingVars = List.copyOf(nonTerminatingVars);
    }
}
