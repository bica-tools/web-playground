package com.bica.reborn.ccs;

import java.util.*;

/**
 * Labelled Transition System with integer state IDs.
 *
 * @param states      set of state IDs
 * @param transitions list of labelled transitions
 * @param initial     the initial state
 * @param labels      human-readable description per state
 */
public record LTS(
        Set<Integer> states,
        List<LTSTransition> transitions,
        int initial,
        Map<Integer, String> labels) {

    /** A single labelled transition {@code source --label--> target}. */
    public record LTSTransition(int source, String label, int target) {

        public LTSTransition {
            Objects.requireNonNull(label, "label must not be null");
        }
    }

    public LTS {
        Objects.requireNonNull(states, "states must not be null");
        Objects.requireNonNull(transitions, "transitions must not be null");
        Objects.requireNonNull(labels, "labels must not be null");
        states = Set.copyOf(states);
        transitions = List.copyOf(transitions);
        labels = Map.copyOf(labels);
    }

    /** Return {@code (label, target)} pairs for transitions from {@code state}. */
    public List<Map.Entry<String, Integer>> successors(int state) {
        var result = new ArrayList<Map.Entry<String, Integer>>();
        for (var t : transitions) {
            if (t.source() == state) {
                result.add(Map.entry(t.label(), t.target()));
            }
        }
        return result;
    }
}
