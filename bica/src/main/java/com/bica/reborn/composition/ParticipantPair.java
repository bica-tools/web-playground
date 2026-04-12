package com.bica.reborn.composition;

import java.util.Objects;

/**
 * Ordered pair of participant names, used as map key for pairwise results.
 */
public record ParticipantPair(String first, String second) {

    public ParticipantPair {
        Objects.requireNonNull(first, "first must not be null");
        Objects.requireNonNull(second, "second must not be null");
    }
}
