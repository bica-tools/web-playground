package com.bica.reborn.ccs;

import java.util.Objects;

/** Process variable reference (e.g. {@code X} inside {@code fix X . ...}). */
public record CCSVar(String name) implements CCSProcess {

    public CCSVar {
        Objects.requireNonNull(name, "name must not be null");
    }
}
