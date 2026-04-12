package com.bica.reborn.statespace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Labeled transition system for a session type.
 *
 * <ul>
 *   <li>{@code states} &mdash; set of integer state IDs</li>
 *   <li>{@code transitions} &mdash; list of {@code (source, label, target)} triples</li>
 *   <li>{@code top} &mdash; initial protocol state (top)</li>
 *   <li>{@code bottom} &mdash; terminal state (bottom = end)</li>
 *   <li>{@code labels} &mdash; human-readable description for each state</li>
 * </ul>
 */
public record StateSpace(
        Set<Integer> states,
        List<Transition> transitions,
        int top,
        int bottom,
        Map<Integer, String> labels) {

    /** A single labeled transition in the state space. */
    public record Transition(int source, String label, int target, TransitionKind kind) {

        /** Backward-compatible 3-arg constructor; defaults to {@link TransitionKind#METHOD}. */
        public Transition(int source, String label, int target) {
            this(source, label, target, TransitionKind.METHOD);
        }

        public Transition {
            Objects.requireNonNull(label, "label must not be null");
            Objects.requireNonNull(kind, "kind must not be null");
        }
    }

    public StateSpace {
        Objects.requireNonNull(states, "states must not be null");
        Objects.requireNonNull(transitions, "transitions must not be null");
        Objects.requireNonNull(labels, "labels must not be null");
        states = Set.copyOf(states);
        transitions = List.copyOf(transitions);
        labels = Map.copyOf(labels);
    }

    /** Return {@code (label, target)} pairs for transitions from {@code state}. */
    public List<Map.Entry<String, Integer>> enabled(int state) {
        var result = new ArrayList<Map.Entry<String, Integer>>();
        for (var t : transitions) {
            if (t.source() == state) {
                result.add(Map.entry(t.label(), t.target()));
            }
        }
        return result;
    }

    /** Direct successor states of {@code state}. */
    public Set<Integer> successors(int state) {
        var result = new HashSet<Integer>();
        for (var t : transitions) {
            if (t.source() == state) {
                result.add(t.target());
            }
        }
        return result;
    }

    /**
     * Returns the target state after taking transition {@code label} from {@code state},
     * or empty if no such transition exists.
     */
    public OptionalInt step(int state, String label) {
        for (var t : transitions) {
            if (t.source() == state && t.label().equals(label)) {
                return OptionalInt.of(t.target());
            }
        }
        return OptionalInt.empty();
    }

    /** Returns the set of method/label names enabled at {@code state}. */
    public Set<String> enabledLabels(int state) {
        return transitions.stream()
                .filter(t -> t.source() == state)
                .map(Transition::label)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Returns full {@link Transition} objects for transitions from {@code state}. */
    public List<Transition> transitionsFrom(int state) {
        var result = new ArrayList<Transition>();
        for (var t : transitions) {
            if (t.source() == state) {
                result.add(t);
            }
        }
        return result;
    }

    /** Returns method names enabled at {@code state} (only {@link TransitionKind#METHOD}). */
    public Set<String> enabledMethods(int state) {
        return transitions.stream()
                .filter(t -> t.source() == state && t.kind() == TransitionKind.METHOD)
                .map(Transition::label)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Returns selection labels at {@code state} (only {@link TransitionKind#SELECTION}). */
    public Set<String> enabledSelections(int state) {
        return transitions.stream()
                .filter(t -> t.source() == state && t.kind() == TransitionKind.SELECTION)
                .map(Transition::label)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** All states reachable from {@code state} (inclusive). */
    public Set<Integer> reachableFrom(int state) {
        var visited = new HashSet<Integer>();
        var stack = new ArrayDeque<Integer>();
        stack.push(state);
        while (!stack.isEmpty()) {
            int s = stack.pop();
            if (visited.add(s)) {
                for (int t : successors(s)) {
                    stack.push(t);
                }
            }
        }
        return visited;
    }
}
