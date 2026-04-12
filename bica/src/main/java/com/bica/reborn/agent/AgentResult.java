package com.bica.reborn.agent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Outcome of an agent invocation.
 *
 * @param success           true if the agent completed without errors
 * @param selectionOutcome  for selection handlers: the label chosen (null for branch handlers)
 * @param output            handler's output data
 * @param transitionsTaken  session type labels traversed during this invocation
 * @param errorMessage      error description if not successful
 */
public record AgentResult(
        boolean success,
        String selectionOutcome,
        Map<String, Object> output,
        List<String> transitionsTaken,
        String errorMessage) {

    public AgentResult {
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(transitionsTaken, "transitionsTaken must not be null");
        output = Map.copyOf(output);
        transitionsTaken = List.copyOf(transitionsTaken);
    }

    /** Successful result with output data. */
    public static AgentResult success(Map<String, Object> output) {
        return new AgentResult(true, null, output, List.of(), null);
    }

    /** Successful result with a selection outcome. */
    public static AgentResult success(String selection, Map<String, Object> output) {
        return new AgentResult(true, selection, output, List.of(), null);
    }

    /** Successful result with transitions recorded. */
    public static AgentResult success(Map<String, Object> output, List<String> transitions) {
        return new AgentResult(true, null, output, transitions, null);
    }

    /** Successful result with selection and transitions. */
    public static AgentResult success(String selection, Map<String, Object> output, List<String> transitions) {
        return new AgentResult(true, selection, output, transitions, null);
    }

    /** Failed result. */
    public static AgentResult failure(String error) {
        return new AgentResult(false, null, Map.of(), List.of(), error);
    }

    /** Failed result with partial output. */
    public static AgentResult failure(String error, Map<String, Object> partialOutput) {
        return new AgentResult(false, null, partialOutput, List.of(), error);
    }
}
