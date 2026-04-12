package com.bica.reborn.exception;

import java.util.Set;

/**
 * Thrown when a session-typed object is finalized or garbage-collected
 * while still in a non-terminal state (i.e., the protocol was not
 * completed).
 *
 * <p>Extends {@link IllegalStateException} for backward compatibility.
 *
 * <p>Example:
 * <pre>{@code
 * // In a session-typed class:
 * public void close() {
 *     if (state != State.CLOSED) {
 *         throw new IncompleteSessionException(state.name(),
 *                 Set.of("read", "write"));
 *     }
 * }
 * }</pre>
 */
public class IncompleteSessionException extends IllegalStateException {

    private final String currentState;
    private final Set<String> remainingMethods;

    /**
     * @param currentState     description of the current object state
     * @param remainingMethods methods still enabled (protocol not complete)
     */
    public IncompleteSessionException(String currentState, Set<String> remainingMethods) {
        super("Incomplete session: object abandoned in state " + currentState
                + "; remaining methods: " + remainingMethods);
        this.currentState = currentState;
        this.remainingMethods = Set.copyOf(remainingMethods);
    }

    public String currentState() { return currentState; }
    public Set<String> remainingMethods() { return remainingMethods; }
}
