package com.bica.reborn.typestate;

import java.util.Objects;

/**
 * A method call on a tracked variable at a specific source position.
 *
 * @param variable the variable name (e.g., "f")
 * @param method   the method called (e.g., "read")
 * @param position source position for error reporting (0 if unknown)
 */
public record MethodCall(String variable, String method, long position) {

    public MethodCall {
        Objects.requireNonNull(variable, "variable must not be null");
        Objects.requireNonNull(method, "method must not be null");
    }
}
