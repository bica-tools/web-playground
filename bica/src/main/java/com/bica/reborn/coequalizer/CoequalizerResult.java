package com.bica.reborn.coequalizer;

import com.bica.reborn.statespace.StateSpace;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Result of coequalizer analysis for a pair of morphisms.
 *
 * @param hasCoequalizer              True iff the coequalizer exists.
 * @param quotientSpace               The quotient state space, or null.
 * @param quotientMap                  The quotient map q: R → Q, or null.
 * @param congruenceClasses            Congruence class representatives → members.
 * @param fMapping                     The mapping f.
 * @param gMapping                     The mapping g.
 * @param isLattice                    Whether the quotient is a lattice.
 * @param isRealizable                 Whether the quotient has reticular form.
 * @param universalPropertyHolds       True iff the universal property was verified.
 * @param counterexample               Description of failure, or null.
 */
public record CoequalizerResult(
        boolean hasCoequalizer,
        StateSpace quotientSpace,
        Map<Integer, Integer> quotientMap,
        Map<Integer, Set<Integer>> congruenceClasses,
        Map<Integer, Integer> fMapping,
        Map<Integer, Integer> gMapping,
        boolean isLattice,
        boolean isRealizable,
        boolean universalPropertyHolds,
        String counterexample) {

    public CoequalizerResult {
        Objects.requireNonNull(fMapping);
        Objects.requireNonNull(gMapping);
        fMapping = Map.copyOf(fMapping);
        gMapping = Map.copyOf(gMapping);
        if (quotientMap != null) quotientMap = Map.copyOf(quotientMap);
    }
}
