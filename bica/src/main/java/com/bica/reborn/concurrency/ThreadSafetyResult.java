package com.bica.reborn.concurrency;

import java.util.List;
import java.util.Objects;

/**
 * Result of a thread safety check on a session type.
 *
 * @param isSafe          True iff all concurrent method pairs are compatible.
 * @param violations      Incompatible method pairs found in parallel branches.
 * @param trustBoundaries Methods annotated as thread-safe (trust boundaries).
 * @param classifications All method classifications used during the check.
 */
public record ThreadSafetyResult(
        boolean isSafe,
        List<Violation> violations,
        List<String> trustBoundaries,
        List<MethodClassification> classifications) {

    /**
     * A pair of methods that are concurrently reachable but incompatible.
     *
     * @param method1 First method name.
     * @param level1  Concurrency level of the first method.
     * @param method2 Second method name.
     * @param level2  Concurrency level of the second method.
     */
    public record Violation(
            String method1, ConcurrencyLevel level1,
            String method2, ConcurrencyLevel level2) {

        public Violation {
            Objects.requireNonNull(method1, "method1 must not be null");
            Objects.requireNonNull(level1, "level1 must not be null");
            Objects.requireNonNull(method2, "method2 must not be null");
            Objects.requireNonNull(level2, "level2 must not be null");
        }
    }

    public ThreadSafetyResult {
        Objects.requireNonNull(violations, "violations must not be null");
        Objects.requireNonNull(trustBoundaries, "trustBoundaries must not be null");
        Objects.requireNonNull(classifications, "classifications must not be null");
        violations = List.copyOf(violations);
        trustBoundaries = List.copyOf(trustBoundaries);
        classifications = List.copyOf(classifications);
    }
}
