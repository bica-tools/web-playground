package com.bica.reborn.reticular;

import java.util.List;
import java.util.Objects;

/**
 * Classification of a single state in a state space.
 *
 * @param state  the state ID
 * @param kind   one of "branch", "select", "end", "product"
 * @param labels the transition labels from this state
 */
public record StateClassification(int state, String kind, List<String> labels) {

    public StateClassification {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(labels, "labels must not be null");
        labels = List.copyOf(labels);
    }
}
