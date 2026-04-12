package com.bica.reborn.fca;

import java.util.List;

/**
 * The concept lattice of a formal context.
 *
 * @param concepts list of all formal concepts
 * @param order    list of (i, j) pairs where concept i &le; concept j (extent_i &sube; extent_j)
 * @param top      index of the top concept (largest extent)
 * @param bottom   index of the bottom concept (smallest extent)
 */
public record ConceptLattice(
        List<FormalConcept> concepts,
        List<int[]> order,
        int top,
        int bottom) {

    public ConceptLattice {
        concepts = List.copyOf(concepts);
        order = List.copyOf(order);
    }
}
