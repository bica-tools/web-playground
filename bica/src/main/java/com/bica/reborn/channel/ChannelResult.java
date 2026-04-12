package com.bica.reborn.channel;

import com.bica.reborn.statespace.StateSpace;

import java.util.Objects;

/**
 * Result of channel duality analysis for a session type S.
 *
 * <p>Captures whether {@code L(S) × L(dual(S))} forms a lattice,
 * whether the two halves are isomorphic, whether branch/selection
 * annotations are complementary, and references to the constructed
 * state spaces.
 *
 * @param channelIsLattice       true iff {@code L(S) × L(dual(S))} is a lattice
 * @param dualityIsomorphism     true iff {@code L(S) ≅ L(dual(S))}
 * @param branchComplementarity  true iff branch ↔ selection under the isomorphism
 * @param channelProduct         the product state space {@code L(S) × L(dual(S))}
 * @param roleA                  state space for role A ({@code L(S)})
 * @param roleB                  state space for role B ({@code L(dual(S))})
 */
public record ChannelResult(
        boolean channelIsLattice,
        boolean dualityIsomorphism,
        boolean branchComplementarity,
        StateSpace channelProduct,
        StateSpace roleA,
        StateSpace roleB) {

    public ChannelResult {
        Objects.requireNonNull(channelProduct, "channelProduct must not be null");
        Objects.requireNonNull(roleA, "roleA must not be null");
        Objects.requireNonNull(roleB, "roleB must not be null");
    }
}
