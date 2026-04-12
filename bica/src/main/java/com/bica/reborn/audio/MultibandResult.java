package com.bica.reborn.audio;

import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.List;
import java.util.Objects;

/**
 * Result of analyzing a multiband processor as a product lattice.
 *
 * @param bands            the frequency bands analyzed
 * @param sessionType      the parallel session type string
 * @param stateSpace       the product lattice state space
 * @param numStates        total number of states in the product lattice
 * @param numTransitions   total number of transitions
 * @param isLattice        whether the state space forms a lattice
 * @param isDistributive   whether the lattice is distributive
 * @param classification   Birkhoff classification
 * @param width            maximum number of concurrent bands (= num bands)
 * @param compressionRatio ratio of product states to sum of individual states
 * @param bandSizes        number of states per individual band
 * @param latticeResult    full lattice check result
 */
public record MultibandResult(
        List<FrequencyBand> bands,
        String sessionType,
        StateSpace stateSpace,
        int numStates,
        int numTransitions,
        boolean isLattice,
        boolean isDistributive,
        String classification,
        int width,
        double compressionRatio,
        List<Integer> bandSizes,
        LatticeResult latticeResult) {

    public MultibandResult {
        Objects.requireNonNull(bands);
        Objects.requireNonNull(sessionType);
        Objects.requireNonNull(stateSpace);
        Objects.requireNonNull(classification);
        Objects.requireNonNull(bandSizes);
        Objects.requireNonNull(latticeResult);
        bands = List.copyOf(bands);
        bandSizes = List.copyOf(bandSizes);
    }
}
