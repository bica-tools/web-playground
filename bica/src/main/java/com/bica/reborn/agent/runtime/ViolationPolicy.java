package com.bica.reborn.agent.runtime;

/**
 * Policy for handling protocol violations detected at runtime.
 */
public enum ViolationPolicy {
    /** Throw {@link com.bica.reborn.agent.AgentException} immediately. Safest default. */
    THROW,
    /** Log the violation and continue execution. Useful for monitoring. */
    LOG_AND_CONTINUE,
    /** Mark the agent as quarantined, preventing further invocations. */
    QUARANTINE
}
