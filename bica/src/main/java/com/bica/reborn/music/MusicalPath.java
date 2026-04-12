package com.bica.reborn.music;

import java.util.List;
import java.util.Objects;

/**
 * A sequence of notes forming one voice.
 *
 * @param notes     the notes in this voice
 * @param voiceName human-readable name for this voice
 */
public record MusicalPath(
        List<MusicalNote> notes,
        String voiceName) {

    public MusicalPath {
        Objects.requireNonNull(notes, "notes must not be null");
        Objects.requireNonNull(voiceName, "voiceName must not be null");
        notes = List.copyOf(notes);
    }

    public MusicalPath(List<MusicalNote> notes) {
        this(notes, "Voice");
    }
}
