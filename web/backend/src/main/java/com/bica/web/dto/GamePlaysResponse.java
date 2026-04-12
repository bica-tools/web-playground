package com.bica.web.dto;

import java.util.List;

/**
 * Response for {@code POST /api/game-plays}: test paths as game plays.
 *
 * <p>Each game play is a sequence of steps through the state space,
 * categorized as valid (winning), violation (illegal move), or
 * incomplete (abandoned).
 */
public record GamePlaysResponse(
    List<GamePlay> plays,
    int totalTransitions,
    int totalStates,
    GameDataResponse board
) {
    public record GamePlay(
        String name,
        String kind,
        List<GameStep> steps,
        /** For violations: the illegal method attempted. */
        String violationMethod,
        /** For violations: methods that were actually enabled. */
        List<String> enabledMethods,
        /** For incomplete: methods still available at the stopped state. */
        List<String> remainingMethods,
        double transitionCoverage,
        double stateCoverage
    ) {}

    public record GameStep(
        String label,
        int from,
        int to,
        boolean isSelection
    ) {}
}
