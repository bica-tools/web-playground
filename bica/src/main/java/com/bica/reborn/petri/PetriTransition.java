package com.bica.reborn.petri;

/**
 * A transition in a Petri net.
 *
 * @param id          Unique transition identifier.
 * @param label       Method/event label (e.g., "open", "read").
 * @param isSelection True if this corresponds to an internal-choice transition.
 * @param sourceState Source state in the original state space.
 * @param targetState Target state in the original state space.
 */
public record PetriTransition(int id, String label, boolean isSelection,
                               Integer sourceState, Integer targetState) {

    public PetriTransition {
        if (label == null) {
            throw new NullPointerException("label must not be null");
        }
    }
}
