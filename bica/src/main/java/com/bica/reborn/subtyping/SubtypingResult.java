package com.bica.reborn.subtyping;

import java.util.Objects;

/**
 * Result of a Gay–Hole subtyping check between two session types.
 *
 * @param lhs        pretty-printed left-hand side type
 * @param rhs        pretty-printed right-hand side type
 * @param isSubtype  true iff lhs ≤_GH rhs
 * @param reason     human-readable explanation if not a subtype; null otherwise
 */
public record SubtypingResult(String lhs, String rhs, boolean isSubtype, String reason) {

    public SubtypingResult {
        Objects.requireNonNull(lhs, "lhs must not be null");
        Objects.requireNonNull(rhs, "rhs must not be null");
    }

    /** Convenience constructor for positive results. */
    public SubtypingResult(String lhs, String rhs, boolean isSubtype) {
        this(lhs, rhs, isSubtype, null);
    }
}
