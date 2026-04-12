package com.bica.reborn.composition;

import java.util.Map;
import java.util.Objects;

/**
 * Result of comparing bottom-up composition with top-down projection.
 *
 * @param globalTypeString       the global type that was parsed
 * @param globalStates           number of states in the global state space
 * @param productStates          number of states in the product of projections
 * @param globalIsLattice        true iff L(G) is a lattice
 * @param productIsLattice       true iff the product is a lattice
 * @param embeddingExists        true iff L(G) embeds into the product
 * @param overApproximationRatio product_states / global_states
 * @param roleTypeMatches        role → whether participant type ≅ projected type
 */
public record ComparisonResult(
        String globalTypeString,
        int globalStates,
        int productStates,
        boolean globalIsLattice,
        boolean productIsLattice,
        boolean embeddingExists,
        double overApproximationRatio,
        Map<String, Boolean> roleTypeMatches) {

    public ComparisonResult {
        Objects.requireNonNull(globalTypeString, "globalTypeString must not be null");
        Objects.requireNonNull(roleTypeMatches, "roleTypeMatches must not be null");
        roleTypeMatches = Map.copyOf(roleTypeMatches);
    }
}
