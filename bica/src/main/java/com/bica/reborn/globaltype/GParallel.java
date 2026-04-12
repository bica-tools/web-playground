package com.bica.reborn.globaltype;

import java.util.Objects;

/**
 * Parallel composition of global types: G₁ || G₂.
 */
public record GParallel(GlobalType left, GlobalType right) implements GlobalType {
    public GParallel {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");
    }
}
