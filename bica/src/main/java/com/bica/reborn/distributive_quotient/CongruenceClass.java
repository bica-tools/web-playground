package com.bica.reborn.distributive_quotient;

import java.util.Objects;
import java.util.Set;

/**
 * An equivalence class in a lattice congruence.
 *
 * @param representative the minimum state ID in the class
 * @param members        all state IDs in the class
 * @param labels         transition labels within the class
 */
public record CongruenceClass(
        int representative,
        Set<Integer> members,
        Set<String> labels) {

    public CongruenceClass {
        Objects.requireNonNull(members, "members must not be null");
        Objects.requireNonNull(labels, "labels must not be null");
        members = Set.copyOf(members);
        labels = Set.copyOf(labels);
    }
}
