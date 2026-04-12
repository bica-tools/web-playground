package com.bica.reborn.ast;

import java.util.Objects;

/** Parallel composition {@code ( S₁ || S₂ )}. */
public record Parallel(SessionType left, SessionType right) implements SessionType {

    public Parallel {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");
    }
}
