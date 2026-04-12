package com.bica.reborn.audio;

import java.util.Objects;

/**
 * A node in an audio routing graph.
 *
 * @param name     unique identifier for this node
 * @param nodeType one of "source", "effect", "bus", "output"
 * @param channels channel configuration ("mono", "stereo", "surround")
 */
public record AudioNode(String name, String nodeType, String channels) {

    public AudioNode {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(nodeType, "nodeType must not be null");
        Objects.requireNonNull(channels, "channels must not be null");
        if (!nodeType.equals("source") && !nodeType.equals("effect")
                && !nodeType.equals("bus") && !nodeType.equals("output")) {
            throw new IllegalArgumentException(
                    "Invalid nodeType '" + nodeType + "'; must be 'source', 'effect', 'bus', or 'output'");
        }
        if (!channels.equals("mono") && !channels.equals("stereo")
                && !channels.equals("surround")) {
            throw new IllegalArgumentException(
                    "Invalid channels '" + channels + "'; must be 'mono', 'stereo', or 'surround'");
        }
    }

    /** Convenience constructor with default stereo channels. */
    public AudioNode(String name, String nodeType) {
        this(name, nodeType, "stereo");
    }
}
