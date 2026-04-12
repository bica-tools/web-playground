package com.bica.reborn.domain.l3;

import java.util.Objects;

/**
 * Result of analyzing a protocol at a given fidelity level (L1, L2, or L3).
 *
 * <p>Ported from {@code reticulate/reticulate/l3_protocols.py}.
 *
 * @param name              Protocol name (e.g., "java_iterator").
 * @param level             Fidelity level: "L1", "L2", or "L3".
 * @param sessionType       Session type string used for analysis.
 * @param numStates         Number of states in the state space.
 * @param numTransitions    Number of transitions in the state space.
 * @param isLattice         True iff the state space forms a lattice.
 * @param isDistributive    True iff the lattice is distributive.
 * @param isModular         True iff the lattice is modular.
 * @param hasM3             True iff the lattice contains an M₃ sublattice.
 * @param hasN5             True iff the lattice contains an N₅ sublattice.
 * @param classification    One of "boolean", "distributive", "modular", "lattice", "not_lattice".
 * @param isGraded          True iff the lattice is graded.
 * @param rankIsValuation   True iff the rank function is a valuation.
 */
public record L3Result(
        String name,
        String level,
        String sessionType,
        int numStates,
        int numTransitions,
        boolean isLattice,
        boolean isDistributive,
        boolean isModular,
        boolean hasM3,
        boolean hasN5,
        String classification,
        boolean isGraded,
        boolean rankIsValuation) {

    public L3Result {
        Objects.requireNonNull(name);
        Objects.requireNonNull(level);
        Objects.requireNonNull(sessionType);
        Objects.requireNonNull(classification);
    }
}
