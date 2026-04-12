package com.bica.reborn.agent.transport;

import java.util.concurrent.CompletableFuture;

/**
 * Pluggable transport for inter-agent communication.
 *
 * <p>The same agent protocol works identically whether agents are in-process,
 * communicating via MCP (tool-calling), or via A2A (task delegation). The
 * transport handles serialization, delivery, and response correlation.
 *
 * <p>Session type: {@code &{send: +{delivered: end, failed: end}, receive: +{message: end, timeout: end}}}
 */
public interface Transport {

    /** Send a message to a named agent. Returns the response asynchronously. */
    CompletableFuture<TransportMessage> send(String targetAgent, TransportMessage message);

    /** Register a listener for incoming messages to a named agent. */
    void onMessage(String agentName, TransportListener listener);

    /** Transport identifier for logging/observability. */
    String id();
}
