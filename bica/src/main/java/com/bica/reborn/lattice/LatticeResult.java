package com.bica.reborn.lattice;

import java.util.Map;
import java.util.Objects;

/**
 * Result of a lattice check on a state space.
 *
 * @param isLattice       True iff the quotient poset is a lattice.
 * @param hasTop          True iff the top (initial state's SCC) reaches all SCCs.
 * @param hasBottom       True iff all SCCs reach the bottom (end state's SCC).
 * @param allMeetsExist   True iff every pair of quotient nodes has a meet.
 * @param allJoinsExist   True iff every pair of quotient nodes has a join.
 * @param numScc          Number of SCCs in the quotient.
 * @param counterexample  First failing pair, or {@code null} if the poset is a lattice.
 * @param sccMap          Mapping from original state ID to its SCC representative.
 */
public record LatticeResult(
        boolean isLattice,
        boolean hasTop,
        boolean hasBottom,
        boolean allMeetsExist,
        boolean allJoinsExist,
        int numScc,
        Counterexample counterexample,
        Map<Integer, Integer> sccMap) {

    /** A counterexample pair where meet or join does not exist. */
    public record Counterexample(int a, int b, String kind) {

        public Counterexample {
            Objects.requireNonNull(kind, "kind must not be null");
        }
    }

    public LatticeResult {
        Objects.requireNonNull(sccMap, "sccMap must not be null");
        sccMap = Map.copyOf(sccMap);
    }
}
