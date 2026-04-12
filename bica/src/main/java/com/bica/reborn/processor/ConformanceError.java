package com.bica.reborn.processor;

import java.util.Objects;
import java.util.Set;

/**
 * Describes a conformance violation between a class method and its session type.
 *
 * <p>Produced when a method that precedes a selection ({@code +{...}}) has a return
 * type that doesn't cover the selection labels — e.g. returns {@code void} instead of
 * an enum whose constants match the labels.
 *
 * @param method         the method name in the session type
 * @param expectedLabels the selection labels that the method's return type must cover
 * @param message        human-readable error description
 */
public record ConformanceError(String method, Set<String> expectedLabels, String message) {

    public ConformanceError {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(expectedLabels, "expectedLabels must not be null");
        Objects.requireNonNull(message, "message must not be null");
        expectedLabels = Set.copyOf(expectedLabels);
    }
}
