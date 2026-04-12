package com.bica.reborn.petri;

import com.bica.reborn.lattice.LatticeResult;

import java.util.Objects;

/**
 * Result of marking lattice analysis.
 *
 * @param latticeResult              Lattice check on the marking poset.
 * @param width                      Maximum antichain size (Dilworth width).
 * @param height                     Longest chain length from top to bottom.
 * @param numMarkings                Number of reachable markings.
 * @param numCoveringPairs           Number of pairs in the covering relation.
 * @param isIsomorphicToStatespace   True if M(N(S)) is isomorphic to L(S).
 * @param chainCount                 Number of maximal chains.
 */
public record MarkingLatticeResult(
        LatticeResult latticeResult,
        int width,
        int height,
        int numMarkings,
        int numCoveringPairs,
        boolean isIsomorphicToStatespace,
        int chainCount) {

    public MarkingLatticeResult {
        Objects.requireNonNull(latticeResult);
    }
}
