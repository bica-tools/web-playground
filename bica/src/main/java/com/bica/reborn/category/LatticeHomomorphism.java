package com.bica.reborn.category;

import com.bica.reborn.statespace.StateSpace;

import java.util.Map;
import java.util.Objects;

/**
 * A lattice homomorphism between two state-space lattices.
 *
 * <p>A lattice homomorphism preserves both meets ({@code ∧}) and joins ({@code ∨}):
 * {@code f(a ∧ b) = f(a) ∧ f(b)} and {@code f(a ∨ b) = f(a) ∨ f(b)}.
 *
 * @param mapping        map from source state IDs to target state IDs
 * @param source         the domain state-space lattice
 * @param target         the codomain state-space lattice
 * @param preservesMeets true iff the mapping preserves all meets
 * @param preservesJoins true iff the mapping preserves all joins
 */
public record LatticeHomomorphism(
        Map<Integer, Integer> mapping,
        StateSpace source,
        StateSpace target,
        boolean preservesMeets,
        boolean preservesJoins) {

    public LatticeHomomorphism {
        Objects.requireNonNull(mapping, "mapping must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
        mapping = Map.copyOf(mapping);
    }
}
