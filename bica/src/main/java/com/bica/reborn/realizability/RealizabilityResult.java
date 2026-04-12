package com.bica.reborn.realizability;

import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.reticular.ReticularFormResult;

import java.util.List;
import java.util.Objects;

/**
 * Result of realizability analysis.
 *
 * @param isRealizable      True iff the state space arises as L(S) for some S.
 * @param obstructions      All detected obstructions (empty if realizable).
 * @param conditions        Necessary conditions summary.
 * @param reconstructedType Pretty-printed session type (if realizable).
 * @param latticeResult     Underlying lattice check result.
 * @param reticularResult   Underlying reticular form check result.
 */
public record RealizabilityResult(
        boolean isRealizable,
        List<RealizabilityChecker.Obstruction> obstructions,
        RealizabilityChecker.Conditions conditions,
        String reconstructedType,
        LatticeResult latticeResult,
        ReticularFormResult reticularResult) {

    public RealizabilityResult {
        Objects.requireNonNull(obstructions);
        Objects.requireNonNull(conditions);
        obstructions = List.copyOf(obstructions);
    }
}
