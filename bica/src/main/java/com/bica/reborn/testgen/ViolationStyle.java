package com.bica.reborn.testgen;

/**
 * How violation tests are generated.
 *
 * <ul>
 *   <li>{@code DISABLED_ANNOTATION} &mdash; violation tests are annotated with
 *       {@code @Disabled} and call the method directly (skeleton for manual completion)</li>
 *   <li>{@code CALL_ANYWAY} &mdash; violation tests wrap the call in
 *       {@code assertThrows(ProtocolViolationException.class, ...)} so they are runnable</li>
 * </ul>
 */
public enum ViolationStyle {
    DISABLED_ANNOTATION,
    CALL_ANYWAY
}
