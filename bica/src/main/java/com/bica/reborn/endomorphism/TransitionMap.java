package com.bica.reborn.endomorphism;

import java.util.Map;
import java.util.Objects;

/**
 * A partial function induced by a transition label.
 *
 * <p>For label {@code m}, {@code f_m(s) = t} iff there exists a transition
 * {@code (s, m, t)} in the state space. The domain is the set of states
 * where label {@code m} is enabled.
 *
 * @param label      the transition label
 * @param mapping    source state → target state
 * @param domainSize number of states where this label is enabled
 * @param isSelection true if this label is a selection (internal choice) transition
 */
public record TransitionMap(
        String label,
        Map<Integer, Integer> mapping,
        int domainSize,
        boolean isSelection) {

    public TransitionMap {
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(mapping, "mapping must not be null");
        mapping = Map.copyOf(mapping);
    }
}
