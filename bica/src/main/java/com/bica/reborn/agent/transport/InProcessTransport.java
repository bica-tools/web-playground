package com.bica.reborn.agent.transport;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process transport: direct method dispatch within the same JVM.
 *
 * <p>This is the default transport for single-JVM multi-agent systems.
 * Messages are delivered synchronously (no serialization, no network).
 * Ideal for testing, local orchestration, and embedded agents.
 */
public final class InProcessTransport implements Transport {

    private final Map<String, TransportListener> listeners = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<TransportMessage> send(String targetAgent, TransportMessage message) {
        TransportListener listener = listeners.get(targetAgent);
        if (listener == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No listener registered for agent: " + targetAgent));
        }
        return CompletableFuture.supplyAsync(() -> {
            listener.onMessage(message);
            return message;
        });
    }

    @Override
    public void onMessage(String agentName, TransportListener listener) {
        listeners.put(agentName, listener);
    }

    @Override
    public String id() {
        return "in-process";
    }

    /** Number of registered listeners. */
    public int listenerCount() {
        return listeners.size();
    }
}
