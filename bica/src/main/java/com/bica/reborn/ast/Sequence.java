package com.bica.reborn.ast;

import java.util.Objects;

/**
 * General sequential composition {@code S₁ . S₂}.
 *
 * <p>Only used when the left-hand side is <em>not</em> a bare identifier.
 * Bare identifiers desugar to single-method {@link Branch}.
 */
public record Sequence(SessionType left, SessionType right) implements SessionType {

    public Sequence {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");
    }
}
