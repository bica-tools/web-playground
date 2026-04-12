package com.bica.reborn.audio;

import java.util.Objects;

/**
 * A single frequency band in a multiband processor.
 *
 * @param name      human-readable band name (e.g. "low", "mid", "high")
 * @param lowHz     lower frequency bound in Hz
 * @param highHz    upper frequency bound in Hz
 * @param processor session type string for the band's effect chain
 */
public record FrequencyBand(String name, double lowHz, double highHz, String processor) {

    public FrequencyBand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(processor, "processor must not be null");
        if (lowHz < 0) throw new IllegalArgumentException("lowHz must be non-negative, got " + lowHz);
        if (highHz <= lowHz) throw new IllegalArgumentException(
                "highHz (" + highHz + ") must be greater than lowHz (" + lowHz + ")");
    }

    /** Bandwidth in Hz. */
    public double bandwidth() {
        return highHz - lowHz;
    }

    /** Geometric center frequency in Hz. */
    public double centerHz() {
        return Math.sqrt(lowHz * highHz);
    }
}
