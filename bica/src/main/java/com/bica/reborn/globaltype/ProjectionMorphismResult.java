package com.bica.reborn.globaltype;

/**
 * Result of verifying projection as a state-space morphism.
 */
public record ProjectionMorphismResult(
        String role,
        int globalStates,
        int localStates,
        boolean isSurjective,
        boolean isOrderPreserving,
        boolean globalIsLattice,
        boolean localIsLattice) {
}
