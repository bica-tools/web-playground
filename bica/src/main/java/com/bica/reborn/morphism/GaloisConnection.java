package com.bica.reborn.morphism;

import com.bica.reborn.statespace.StateSpace;

import java.util.Map;
import java.util.Objects;

/**
 * A Galois connection between two state spaces.
 *
 * @param source the "concrete" domain
 * @param target the "abstract" codomain
 * @param alpha  abstraction map (source &rarr; target)
 * @param gamma  concretization map (target &rarr; source)
 */
public record GaloisConnection(
        StateSpace source,
        StateSpace target,
        Map<Integer, Integer> alpha,
        Map<Integer, Integer> gamma) {

    public GaloisConnection {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(alpha, "alpha must not be null");
        Objects.requireNonNull(gamma, "gamma must not be null");
        alpha = Map.copyOf(alpha);
        gamma = Map.copyOf(gamma);
    }
}
