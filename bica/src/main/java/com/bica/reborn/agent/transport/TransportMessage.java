package com.bica.reborn.agent.transport;

import java.util.Map;
import java.util.Objects;

/**
 * Envelope for inter-agent messages.
 *
 * <p>Every message carries the session type label that identifies which
 * protocol transition this message represents. The payload contains
 * the actual data exchanged between agents.
 *
 * @param fromAgent   sender agent name
 * @param toAgent     recipient agent name
 * @param label       session type transition label
 * @param payload     message data (must be serializable for remote transports)
 * @param timestampMs creation timestamp in milliseconds
 */
public record TransportMessage(
        String fromAgent,
        String toAgent,
        String label,
        Map<String, Object> payload,
        long timestampMs) {

    public TransportMessage {
        Objects.requireNonNull(fromAgent, "fromAgent must not be null");
        Objects.requireNonNull(toAgent, "toAgent must not be null");
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        payload = Map.copyOf(payload);
    }

    /** Convenience factory with auto-timestamp. */
    public static TransportMessage of(String from, String to, String label, Map<String, Object> payload) {
        return new TransportMessage(from, to, label, payload, System.currentTimeMillis());
    }

    /** Create a response to this message. */
    public TransportMessage reply(String label, Map<String, Object> payload) {
        return TransportMessage.of(toAgent, fromAgent, label, payload);
    }
}
