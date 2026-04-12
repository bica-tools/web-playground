package com.bica.reborn.tensor;

import com.bica.reborn.statespace.StateSpace;

import java.util.Map;
import java.util.Objects;

/**
 * Result of external (tensor) product construction.
 *
 * <p>The tensor product models the state space of a program holding
 * multiple objects with independent session types. Each transition is
 * object-qualified: "name.method".
 *
 * @param stateSpaces     Mapping from object name to state space.
 * @param tensor          The program-level product state space.
 * @param isLattice       Whether the tensor product is a lattice (always true).
 * @param projections     Mapping from object name to projection dict.
 * @param stateCounts     Per-object state count.
 */
public record TensorResult(
        Map<String, StateSpace> stateSpaces,
        StateSpace tensor,
        boolean isLattice,
        Map<String, Map<Integer, Integer>> projections,
        Map<String, Integer> stateCounts) {

    public TensorResult {
        Objects.requireNonNull(stateSpaces);
        Objects.requireNonNull(tensor);
        Objects.requireNonNull(projections);
        Objects.requireNonNull(stateCounts);
    }
}
