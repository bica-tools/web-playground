package com.bica.reborn.globaltype;

import java.util.Objects;

/**
 * Global type variable reference.
 */
public record GVar(String name) implements GlobalType {
    public GVar {
        Objects.requireNonNull(name, "name must not be null");
    }
}
