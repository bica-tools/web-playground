package com.bica.reborn.recursion;

import java.util.List;
import java.util.Objects;

/**
 * Result of guardedness analysis.
 *
 * <p>A recursive variable X is <b>guarded</b> in {@code rec X . S} if every
 * occurrence of X in S is under at least one constructor (Branch, Select,
 * Parallel, Sequence).
 *
 * @param isGuarded     true iff every recursive variable is under a constructor
 * @param unguardedVars variables that appear unguarded
 */
public record GuardednessResult(
        boolean isGuarded,
        List<String> unguardedVars) {

    public GuardednessResult {
        Objects.requireNonNull(unguardedVars, "unguardedVars must not be null");
        unguardedVars = List.copyOf(unguardedVars);
    }
}
