package com.bica.reborn.csp;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A CSP failure: a trace paired with a refusal set.
 *
 * <p>A failure {@code (trace, refusals)} means: after performing the sequence
 * of events in {@code trace}, the process can refuse all events in {@code refusals}.
 *
 * @param trace    the finite sequence of event names executed so far
 * @param refusals the set of labels the process refuses after executing the trace
 */
public record Failure(List<String> trace, Set<String> refusals) {

    public Failure {
        Objects.requireNonNull(trace, "trace must not be null");
        Objects.requireNonNull(refusals, "refusals must not be null");
        trace = List.copyOf(trace);
        refusals = Set.copyOf(refusals);
    }
}
