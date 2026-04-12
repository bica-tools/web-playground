package com.bica.reborn.audio;

import java.util.Objects;

/**
 * A directed edge in an audio routing graph.
 *
 * @param src   source node name
 * @param dst   destination node name
 * @param label edge label (e.g. "send", "insert", "feedback")
 */
public record AudioRoute(String src, String dst, String label) {

    public AudioRoute {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(dst, "dst must not be null");
        Objects.requireNonNull(label, "label must not be null");
    }

    /** Convenience constructor with default label "send". */
    public AudioRoute(String src, String dst) {
        this(src, dst, "send");
    }
}
