package com.bica.reborn.typestate;

import java.util.Objects;
import java.util.Set;

/**
 * A protocol violation detected during typestate checking.
 *
 * @param variable the variable name
 * @param method   the method that was called
 * @param state    the state the object was in when the method was called
 * @param enabled  the methods that were actually enabled in that state
 * @param message  human-readable error description
 */
public record TypestateError(
        String variable,
        String method,
        int state,
        Set<String> enabled,
        String message) {

    public TypestateError {
        Objects.requireNonNull(variable, "variable must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(enabled, "enabled must not be null");
        Objects.requireNonNull(message, "message must not be null");
        enabled = Set.copyOf(enabled);
    }
}
