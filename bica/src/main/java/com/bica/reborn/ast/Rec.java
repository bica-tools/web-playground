package com.bica.reborn.ast;

import java.util.Objects;

/** Recursive type {@code rec X . S}. */
public record Rec(String var, SessionType body) implements SessionType {

    public Rec {
        Objects.requireNonNull(var, "var must not be null");
        Objects.requireNonNull(body, "body must not be null");
    }
}
