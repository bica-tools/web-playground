package com.bica.reborn.fca;

import java.util.Set;

/**
 * A formal concept (A, B) where A is a set of objects and B is a set of attributes.
 *
 * @param extent the set of objects (states)
 * @param intent the set of attributes (labels)
 */
public record FormalConcept(Set<Integer> extent, Set<String> intent) {

    public FormalConcept {
        extent = Set.copyOf(extent);
        intent = Set.copyOf(intent);
    }
}
