package com.bica.reborn.reticular;

import java.util.List;
import java.util.Objects;

/**
 * Result of reticular form analysis.
 *
 * @param isReticulate    true iff the state space has reticular form
 * @param classifications per-state classification
 * @param reason          human-readable explanation if not a reticulate, or null
 * @param reconstructed   pretty-printed reconstructed type if successful, or null
 */
public record ReticularFormResult(
        boolean isReticulate,
        List<StateClassification> classifications,
        String reason,
        String reconstructed) {

    public ReticularFormResult {
        Objects.requireNonNull(classifications, "classifications must not be null");
        classifications = List.copyOf(classifications);
    }
}
