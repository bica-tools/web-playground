package com.bica.reborn.fca;

import java.util.*;

/**
 * A formal context (G, M, I) for FCA.
 *
 * @param objects    sorted list of object identifiers (state IDs)
 * @param attributes sorted list of attribute names (transition labels)
 * @param incidence  mapping: object -> set of attributes
 */
public record FormalContext(
        List<Integer> objects,
        List<String> attributes,
        Map<Integer, Set<String>> incidence) {

    public FormalContext {
        objects = List.copyOf(objects);
        attributes = List.copyOf(attributes);
        // Deep-copy incidence to unmodifiable
        var copy = new LinkedHashMap<Integer, Set<String>>();
        for (var e : incidence.entrySet()) {
            copy.put(e.getKey(), Set.copyOf(e.getValue()));
        }
        incidence = Map.copyOf(copy);
    }
}
