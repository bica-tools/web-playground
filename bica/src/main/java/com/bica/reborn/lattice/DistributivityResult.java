package com.bica.reborn.lattice;

import java.util.List;
import java.util.Objects;

/**
 * Result of a distributivity check on a session type lattice.
 *
 * <p>Uses Birkhoff's characterization theorem: a lattice is distributive iff it
 * contains neither an N₅ (pentagon) nor an M₃ (diamond) sublattice. The lattice
 * is modular iff it contains no N₅. The lattice is Boolean iff it is distributive
 * and every element has a complement.
 *
 * <p>Hierarchy: Boolean ⊂ Distributive ⊂ Modular ⊂ Semi-modular ⊂ Lattice
 *
 * @param isLattice       True iff the state space forms a lattice.
 * @param isDistributive  True iff the lattice is distributive (no N₅, no M₃).
 * @param isModular       True iff the lattice is modular (no N₅).
 * @param hasM3           True iff the lattice contains an M₃ (diamond) sublattice.
 * @param hasN5           True iff the lattice contains an N₅ (pentagon) sublattice.
 * @param m3Witness       5-element witness (top, a, b, c, bot) for M₃, or null.
 * @param n5Witness       5-element witness (top, a, b, c, bot) for N₅, or null.
 * @param classification  One of "boolean", "distributive", "modular", "lattice", "not_lattice".
 * @param latticeResult   The underlying lattice check result.
 */
public record DistributivityResult(
        boolean isLattice,
        boolean isDistributive,
        boolean isModular,
        boolean hasM3,
        boolean hasN5,
        List<Integer> m3Witness,
        List<Integer> n5Witness,
        String classification,
        LatticeResult latticeResult) {

    public DistributivityResult {
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(latticeResult, "latticeResult must not be null");
        if (m3Witness != null) m3Witness = List.copyOf(m3Witness);
        if (n5Witness != null) n5Witness = List.copyOf(n5Witness);
    }
}
