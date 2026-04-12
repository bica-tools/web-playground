package com.bica.reborn.agent.transport;

/**
 * Callback for receiving messages from other agents.
 */
@FunctionalInterface
public interface TransportListener {

    /** Called when a message arrives for this agent. */
    void onMessage(TransportMessage message);
}
