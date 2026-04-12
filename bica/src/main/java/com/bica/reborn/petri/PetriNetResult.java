package com.bica.reborn.petri;

/**
 * Result of converting a state space to a Petri net.
 *
 * @param net                     The constructed Petri net.
 * @param numPlaces               Number of places.
 * @param numTransitions          Number of transitions.
 * @param isOccurrenceNet         True if the net is acyclic.
 * @param isFreeChoice            True if the net is free-choice.
 * @param reachabilityIsomorphic  True if the reachability graph is isomorphic to the original state space.
 * @param numReachableMarkings    Number of reachable markings.
 */
public record PetriNetResult(
        PetriNet net,
        int numPlaces,
        int numTransitions,
        boolean isOccurrenceNet,
        boolean isFreeChoice,
        boolean reachabilityIsomorphic,
        int numReachableMarkings) {

    public PetriNetResult {
        if (net == null) {
            throw new NullPointerException("net must not be null");
        }
    }
}
