package com.bica.reborn.csp;

import java.util.Objects;
import java.util.Set;
import java.util.List;

/**
 * Complete CSP semantic characterisation of a state space.
 *
 * @param traces         all finite traces (prefixes of paths from top)
 * @param completeTraces traces that reach the bottom state (complete executions)
 * @param failures       set of (trace, refusal_set) pairs
 * @param alphabet       the full event alphabet (all transition labels)
 */
public record CSPResult(
        Set<List<String>> traces,
        Set<List<String>> completeTraces,
        Set<Failure> failures,
        Set<String> alphabet) {

    public CSPResult {
        Objects.requireNonNull(traces, "traces must not be null");
        Objects.requireNonNull(completeTraces, "completeTraces must not be null");
        Objects.requireNonNull(failures, "failures must not be null");
        Objects.requireNonNull(alphabet, "alphabet must not be null");
        traces = Set.copyOf(traces);
        completeTraces = Set.copyOf(completeTraces);
        failures = Set.copyOf(failures);
        alphabet = Set.copyOf(alphabet);
    }
}
