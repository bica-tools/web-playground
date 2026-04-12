package com.bica.reborn.termination;

import java.util.List;
import java.util.Objects;

/**
 * Result of a WF-Par well-formedness check.
 *
 * @param isWellFormed True iff every {@code Parallel} node satisfies WF-Par.
 * @param errors       Human-readable descriptions of each violation found.
 */
public record WFParallelResult(boolean isWellFormed, List<String> errors) {

    public WFParallelResult {
        Objects.requireNonNull(errors, "errors must not be null");
        errors = List.copyOf(errors);
    }
}
