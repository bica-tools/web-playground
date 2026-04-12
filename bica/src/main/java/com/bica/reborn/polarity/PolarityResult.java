package com.bica.reborn.polarity;

import java.util.*;

/**
 * Result of polarity (FCA) analysis on a state space.
 *
 * @param relation            binary relation R: state → set of enabled labels
 * @param concepts            formal concepts as (extent, intent) pairs
 * @param conceptLatticeEdges covering relation on concepts (by index)
 * @param isConceptLattice    true iff the closure operators form a Galois connection
 *                            (always true by construction; sanity check)
 * @param numConcepts         number of formal concepts
 */
public record PolarityResult(
        Map<Integer, Set<String>> relation,
        List<Concept> concepts,
        List<int[]> conceptLatticeEdges,
        boolean isConceptLattice,
        int numConcepts) {

    public PolarityResult {
        Objects.requireNonNull(relation, "relation must not be null");
        Objects.requireNonNull(concepts, "concepts must not be null");
        Objects.requireNonNull(conceptLatticeEdges, "conceptLatticeEdges must not be null");
        relation = Map.copyOf(relation);
        concepts = List.copyOf(concepts);
        conceptLatticeEdges = List.copyOf(conceptLatticeEdges);
    }
}
