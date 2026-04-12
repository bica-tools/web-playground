package com.bica.reborn.ccs;

import java.util.Objects;

/** Parallel composition {@code P | Q}. */
public record CCSPar(CCSProcess left, CCSProcess right) implements CCSProcess {

    public CCSPar {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");
    }
}
