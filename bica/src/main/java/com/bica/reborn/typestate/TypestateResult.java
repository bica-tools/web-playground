package com.bica.reborn.typestate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of typestate checking for a method body.
 *
 * @param isValid     true if no protocol violations were found
 * @param errors      list of protocol violations (empty if valid)
 * @param finalStates variable → final state ID at method return
 */
public record TypestateResult(
        boolean isValid,
        List<TypestateError> errors,
        Map<String, Integer> finalStates) {

    public TypestateResult {
        Objects.requireNonNull(errors, "errors must not be null");
        Objects.requireNonNull(finalStates, "finalStates must not be null");
        errors = List.copyOf(errors);
        finalStates = Map.copyOf(finalStates);
    }

    /** Convenience factory for a valid result. */
    public static TypestateResult valid(Map<String, Integer> finalStates) {
        return new TypestateResult(true, List.of(), finalStates);
    }
}
