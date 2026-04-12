package com.bica.reborn.music;

import java.util.Objects;

/**
 * A single musical note or rest.
 *
 * @param pitch      "c".."b" or "r" for rest
 * @param accidental "" | "is" (sharp) | "es" (flat)
 * @param octave     0-8 (-1 for rests)
 * @param duration   "1"=whole "2"=half "4"=quarter "8"=eighth "16"=sixteenth
 * @param dotted     whether the note is dotted
 */
public record MusicalNote(
        String pitch,
        String accidental,
        int octave,
        String duration,
        boolean dotted) {

    public MusicalNote {
        Objects.requireNonNull(pitch, "pitch must not be null");
        Objects.requireNonNull(accidental, "accidental must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
    }

    /** True if this note is a rest. */
    public boolean isRest() {
        return "r".equals(pitch);
    }
}
