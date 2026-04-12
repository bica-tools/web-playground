package com.bica.reborn.ast;

import java.util.Objects;

/** Type variable reference (e.g. {@code X} inside {@code rec X . …}). */
public record Var(String name) implements SessionType {

    public Var {
        Objects.requireNonNull(name, "name must not be null");
    }
}
