package com.bica.reborn.petri;

import java.util.*;

/**
 * A Petri net constructed from a session type state space.
 *
 * <p>The net is structured so that each state in the original state space
 * corresponds to a unique marking, and each state-space transition
 * corresponds to a Petri net transition.
 *
 * @param places          Mapping from place ID to Place.
 * @param transitions     Mapping from transition ID to PetriTransition.
 * @param pre             Pre-set: maps transition ID to set of (placeId, weight) arcs.
 * @param post            Post-set: maps transition ID to set of (placeId, weight) arcs.
 * @param initialMarking  The initial marking (tokens at protocol start).
 * @param stateToMarking  Maps state-space state to its corresponding marking.
 * @param markingToState  Maps frozen marking key to state-space state.
 * @param isOccurrenceNet True if the net is acyclic (no recursion).
 * @param isFreeChoice    True if all transitions sharing an input place have the same pre-set.
 */
public record PetriNet(
        Map<Integer, Place> places,
        Map<Integer, PetriTransition> transitions,
        Map<Integer, Set<Arc>> pre,
        Map<Integer, Set<Arc>> post,
        Map<Integer, Integer> initialMarking,
        Map<Integer, Map<Integer, Integer>> stateToMarking,
        Map<String, Integer> markingToState,
        boolean isOccurrenceNet,
        boolean isFreeChoice) {

    /**
     * An arc connecting a place and a transition with a weight.
     */
    public record Arc(int placeId, int weight) implements Comparable<Arc> {
        @Override
        public int compareTo(Arc o) {
            int c = Integer.compare(placeId, o.placeId);
            return c != 0 ? c : Integer.compare(weight, o.weight);
        }
    }

    public PetriNet {
        places = Map.copyOf(places);
        transitions = Map.copyOf(transitions);
        // Deep-copy pre/post
        var preCopy = new HashMap<Integer, Set<Arc>>();
        for (var e : pre.entrySet()) {
            preCopy.put(e.getKey(), Set.copyOf(e.getValue()));
        }
        pre = Map.copyOf(preCopy);
        var postCopy = new HashMap<Integer, Set<Arc>>();
        for (var e : post.entrySet()) {
            postCopy.put(e.getKey(), Set.copyOf(e.getValue()));
        }
        post = Map.copyOf(postCopy);
        initialMarking = Map.copyOf(initialMarking);
        var stmCopy = new HashMap<Integer, Map<Integer, Integer>>();
        for (var e : stateToMarking.entrySet()) {
            stmCopy.put(e.getKey(), Map.copyOf(e.getValue()));
        }
        stateToMarking = Map.copyOf(stmCopy);
        markingToState = Map.copyOf(markingToState);
    }
}
