package com.bica.reborn.ccs;

import java.util.Objects;
import java.util.Set;

/** Restriction {@code P \ {a, b, ...}} — hide actions from the environment. */
public record Restrict(CCSProcess proc, Set<String> actions) implements CCSProcess {

    public Restrict {
        Objects.requireNonNull(proc, "proc must not be null");
        Objects.requireNonNull(actions, "actions must not be null");
        actions = Set.copyOf(actions);
    }
}
