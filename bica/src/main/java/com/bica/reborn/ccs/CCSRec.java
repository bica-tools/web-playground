package com.bica.reborn.ccs;

import java.util.Objects;

/** Recursive process {@code fix X . P}. */
public record CCSRec(String var, CCSProcess body) implements CCSProcess {

    public CCSRec {
        Objects.requireNonNull(var, "var must not be null");
        Objects.requireNonNull(body, "body must not be null");
    }
}
