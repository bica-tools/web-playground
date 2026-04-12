package com.bica.reborn.concurrency;

import java.util.Objects;

/**
 * Classification of a single method's concurrency level.
 *
 * @param methodName The method name as it appears in the session type.
 * @param level      The concurrency level assigned to this method.
 * @param source     Human-readable description of how the level was determined
 *                   (e.g. "annotation @Shared", "synchronized modifier", "default").
 */
public record MethodClassification(String methodName, ConcurrencyLevel level, String source) {

    public MethodClassification {
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(source, "source must not be null");
    }
}
