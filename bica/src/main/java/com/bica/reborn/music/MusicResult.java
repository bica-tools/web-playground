package com.bica.reborn.music;

import java.util.List;
import java.util.Objects;

/**
 * Result of session-type-to-music translation.
 *
 * @param voices         the voice paths extracted from the session type
 * @param lilypondSource complete LilyPond source string
 * @param isPolyphonic   true if the score has multiple voices
 * @param form           detected musical form
 * @param warnings       any warnings generated during translation
 */
public record MusicResult(
        List<MusicalPath> voices,
        String lilypondSource,
        boolean isPolyphonic,
        MusicalForm form,
        List<String> warnings) {

    public MusicResult {
        Objects.requireNonNull(voices, "voices must not be null");
        Objects.requireNonNull(lilypondSource, "lilypondSource must not be null");
        Objects.requireNonNull(form, "form must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        voices = List.copyOf(voices);
        warnings = List.copyOf(warnings);
    }
}
