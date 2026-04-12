package com.bica.reborn.morphism;

import com.bica.reborn.statespace.StateSpace;

import java.util.Map;
import java.util.Objects;

/**
 * A classified morphism between two state spaces.
 *
 * @param source  the domain state space
 * @param target  the codomain state space
 * @param mapping map from source state IDs to target state IDs
 * @param kind    classification of the morphism
 */
public record Morphism(
        StateSpace source,
        StateSpace target,
        Map<Integer, Integer> mapping,
        MorphismKind kind) {

    public Morphism {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(mapping, "mapping must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        mapping = Map.copyOf(mapping);
    }
}
