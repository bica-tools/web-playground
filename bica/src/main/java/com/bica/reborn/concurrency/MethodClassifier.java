package com.bica.reborn.concurrency;

import java.util.Map;
import java.util.Objects;

/**
 * Classifies method names to concurrency levels.
 *
 * <p>This is a functional interface to decouple the thread safety checker
 * from annotation processing. Tests use {@link #fromMap}, while the
 * annotation processor provides its own implementation.
 */
@FunctionalInterface
public interface MethodClassifier {

    /**
     * Returns the classification for the given method, or {@code null}
     * if the method is unknown to this classifier.
     */
    MethodClassification classify(String methodName);

    /**
     * Creates a classifier backed by a map from method names to concurrency levels.
     * Unknown methods return {@code null}.
     *
     * @param levels Map from method name to its concurrency level.
     * @return A classifier that looks up the given map.
     * @throws NullPointerException if {@code levels} is null
     */
    static MethodClassifier fromMap(Map<String, ConcurrencyLevel> levels) {
        Objects.requireNonNull(levels, "levels must not be null");
        var copy = Map.copyOf(levels);
        return methodName -> {
            ConcurrencyLevel level = copy.get(methodName);
            return level == null ? null
                    : new MethodClassification(methodName, level, "map");
        };
    }
}
