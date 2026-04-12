package com.bica.reborn.monitor;

import java.util.Objects;
import java.util.Set;

/**
 * Result of a single monitor transition attempt.
 *
 * @param success     True if the transition was valid and executed.
 * @param fromState   The state before the transition attempt.
 * @param toState     The state after (same as fromState if not successful).
 * @param label       The method/label attempted.
 * @param wasEnabled  The set of methods that were enabled before the attempt.
 */
public record MonitorResult(
        boolean success,
        int fromState,
        int toState,
        String label,
        Set<String> wasEnabled) {

    public MonitorResult {
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(wasEnabled, "wasEnabled must not be null");
        wasEnabled = Set.copyOf(wasEnabled);
    }
}
