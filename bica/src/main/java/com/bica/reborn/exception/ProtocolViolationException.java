package com.bica.reborn.exception;

import java.util.Set;

/**
 * Thrown when a method is called on an object that is not in a state
 * where that method is enabled according to its session type protocol.
 *
 * <p>Extends {@link IllegalStateException} for backward compatibility:
 * existing {@code catch(IllegalStateException)} code still works, but
 * generated tests can assert the more specific type to avoid masking
 * unrelated state errors.
 *
 * <p>Example:
 * <pre>{@code
 * // In a session-typed class:
 * public void read() {
 *     if (state != State.OPENED) {
 *         throw new ProtocolViolationException("read", state.name(),
 *                 Set.of("read", "write"));
 *     }
 *     // ...
 * }
 * }</pre>
 */
public class ProtocolViolationException extends IllegalStateException {

    private final String attemptedMethod;
    private final String currentState;
    private final Set<String> enabledMethods;

    /**
     * @param attemptedMethod the method that was called
     * @param currentState    description of the current object state
     * @param enabledMethods  methods that are enabled in the current state
     */
    public ProtocolViolationException(String attemptedMethod, String currentState,
                                      Set<String> enabledMethods) {
        super("Protocol violation: '" + attemptedMethod + "' is not enabled in state "
                + currentState + "; enabled: " + enabledMethods);
        this.attemptedMethod = attemptedMethod;
        this.currentState = currentState;
        this.enabledMethods = Set.copyOf(enabledMethods);
    }

    /**
     * Convenience constructor when enabled methods are not known.
     *
     * @param attemptedMethod the method that was called
     * @param currentState    description of the current object state
     */
    public ProtocolViolationException(String attemptedMethod, String currentState) {
        super("Protocol violation: '" + attemptedMethod + "' is not enabled in state "
                + currentState);
        this.attemptedMethod = attemptedMethod;
        this.currentState = currentState;
        this.enabledMethods = Set.of();
    }

    public String attemptedMethod() { return attemptedMethod; }
    public String currentState() { return currentState; }
    public Set<String> enabledMethods() { return enabledMethods; }
}
