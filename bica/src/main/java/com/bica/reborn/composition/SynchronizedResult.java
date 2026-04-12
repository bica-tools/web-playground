package com.bica.reborn.composition;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.statespace.StateSpace;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Result of synchronized composition (CSP-style shared-label synchronization).
 *
 * @param participants   name → session type mapping
 * @param stateSpaces    name → lattice mapping
 * @param synced         the synchronized product state space
 * @param freeProduct    the free product (all interleavings) for comparison
 * @param isLattice      true iff the synchronized product is a lattice
 * @param compatibility  pairwise compatibility
 * @param sharedLabels   pairwise shared label sets
 * @param reductionRatio synced states / free product states
 */
public record SynchronizedResult(
        Map<String, SessionType> participants,
        Map<String, StateSpace> stateSpaces,
        StateSpace synced,
        StateSpace freeProduct,
        boolean isLattice,
        Map<ParticipantPair, Boolean> compatibility,
        Map<ParticipantPair, Set<String>> sharedLabels,
        double reductionRatio) {

    public SynchronizedResult {
        Objects.requireNonNull(participants, "participants must not be null");
        Objects.requireNonNull(stateSpaces, "stateSpaces must not be null");
        Objects.requireNonNull(synced, "synced must not be null");
        Objects.requireNonNull(freeProduct, "freeProduct must not be null");
        Objects.requireNonNull(compatibility, "compatibility must not be null");
        Objects.requireNonNull(sharedLabels, "sharedLabels must not be null");
        participants = Map.copyOf(participants);
        stateSpaces = Map.copyOf(stateSpaces);
        compatibility = Map.copyOf(compatibility);
        // Deep copy shared labels
        var slCopy = new java.util.LinkedHashMap<ParticipantPair, Set<String>>();
        sharedLabels.forEach((k, v) -> slCopy.put(k, Set.copyOf(v)));
        sharedLabels = Map.copyOf(slCopy);
    }
}
