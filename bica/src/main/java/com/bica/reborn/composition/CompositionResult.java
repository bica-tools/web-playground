package com.bica.reborn.composition;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.statespace.StateSpace;

import java.util.Map;
import java.util.Objects;

/**
 * Result of composing independent session types via free product.
 *
 * @param participants  name → session type mapping
 * @param stateSpaces   name → lattice mapping
 * @param product       the composite N-ary product lattice
 * @param isLattice     true iff the product is a lattice (always true by theorem)
 * @param compatibility pairwise compatibility (ParticipantPair → bool)
 */
public record CompositionResult(
        Map<String, SessionType> participants,
        Map<String, StateSpace> stateSpaces,
        StateSpace product,
        boolean isLattice,
        Map<ParticipantPair, Boolean> compatibility) {

    public CompositionResult {
        Objects.requireNonNull(participants, "participants must not be null");
        Objects.requireNonNull(stateSpaces, "stateSpaces must not be null");
        Objects.requireNonNull(product, "product must not be null");
        Objects.requireNonNull(compatibility, "compatibility must not be null");
        participants = Map.copyOf(participants);
        stateSpaces = Map.copyOf(stateSpaces);
        compatibility = Map.copyOf(compatibility);
    }
}
