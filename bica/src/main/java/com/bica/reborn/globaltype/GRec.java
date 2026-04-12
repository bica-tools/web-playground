package com.bica.reborn.globaltype;

import java.util.Objects;

/**
 * Recursive global type: rec X . G.
 */
public record GRec(String var, GlobalType body) implements GlobalType {
    public GRec {
        Objects.requireNonNull(var, "var must not be null");
        Objects.requireNonNull(body, "body must not be null");
    }
}
