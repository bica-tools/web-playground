package com.bica.reborn.petri;

/**
 * A place in a Petri net.
 *
 * @param id    Unique place identifier.
 * @param label Human-readable label (e.g., "s0", "s0->s1:open").
 * @param state The state-space state this place represents, or null for intermediate places.
 */
public record Place(int id, String label, Integer state) {

    public Place {
        if (label == null) {
            throw new NullPointerException("label must not be null");
        }
    }
}
