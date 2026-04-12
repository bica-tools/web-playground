package com.bica.reborn.coproduct;

import com.bica.reborn.statespace.StateSpace;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of coproduct analysis for a pair of session type lattices.
 *
 * @param hasCoproduct    True iff a valid coproduct was found.
 * @param bestCandidate   The best candidate construction, or null.
 * @param allCandidates   All candidate constructions tried.
 * @param counterexample  Description of why no coproduct exists, or null.
 */
public record CoproductResult(
        boolean hasCoproduct,
        CoproductCandidate bestCandidate,
        List<CoproductCandidate> allCandidates,
        String counterexample) {

    public CoproductResult {
        Objects.requireNonNull(allCandidates, "allCandidates must not be null");
        allCandidates = List.copyOf(allCandidates);
    }

    /**
     * An injection ι: factor → candidate.
     */
    public record InjectionMap(
            int factorIndex,
            Map<Integer, Integer> mapping,
            boolean isInjective,
            boolean isOrderPreserving) {

        public InjectionMap {
            Objects.requireNonNull(mapping);
            mapping = Map.copyOf(mapping);
        }
    }

    /**
     * A candidate coproduct construction with analysis results.
     */
    public record CoproductCandidate(
            StateSpace statespace,
            List<InjectionMap> injections,
            String construction,
            boolean isLattice,
            boolean isRealizable) {

        public CoproductCandidate {
            Objects.requireNonNull(statespace);
            Objects.requireNonNull(injections);
            Objects.requireNonNull(construction);
            injections = List.copyOf(injections);
        }
    }
}
