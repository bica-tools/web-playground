package com.bica.reborn.polarity;

import java.util.Objects;
import java.util.Set;

/**
 * A formal concept: a maximal rectangle (extent, intent) in the incidence table.
 *
 * <p>The extent is the set of states that share all capabilities in the intent.
 * The intent is the set of labels enabled at all states in the extent.
 *
 * @param extent set of state IDs
 * @param intent set of transition labels
 */
public record Concept(Set<Integer> extent, Set<String> intent) {

    public Concept {
        Objects.requireNonNull(extent, "extent must not be null");
        Objects.requireNonNull(intent, "intent must not be null");
        extent = Set.copyOf(extent);
        intent = Set.copyOf(intent);
    }
}
