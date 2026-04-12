package com.bica.reborn.music;

import java.util.List;
import java.util.Objects;

/**
 * A composition with its session type and rendered score.
 *
 * @param sessionType     the session type string encoding the composition
 * @param musicResult     the rendered LilyPond score
 * @param sections        section names for documentation
 * @param stateCount      states in the session type's state space
 * @param transitionCount transitions in the session type's state space
 */
public record CompositionResult(
        String sessionType,
        MusicResult musicResult,
        List<String> sections,
        int stateCount,
        int transitionCount) {

    public CompositionResult {
        Objects.requireNonNull(sessionType, "sessionType must not be null");
        Objects.requireNonNull(musicResult, "musicResult must not be null");
        Objects.requireNonNull(sections, "sections must not be null");
        sections = List.copyOf(sections);
    }
}
