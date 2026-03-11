package com.bica.web.dto;

import java.util.List;
import java.util.Map;

public record CompositionResponse(
        int participantCount,
        List<ParticipantDto> participants,
        // Free product
        int freeStates,
        int freeTransitions,
        boolean freeIsLattice,
        String freeSvgHtml,
        // Synchronized product
        int syncedStates,
        int syncedTransitions,
        boolean syncedIsLattice,
        String syncedSvgHtml,
        // Reduction
        double reductionRatio,
        // Compatibility
        List<CompatibilityEntry> compatibility,
        // Shared labels
        List<SharedLabelsEntry> sharedLabels,
        // Optional global comparison
        GlobalComparisonDto globalComparison
) {
    public record ParticipantDto(
            String name,
            String pretty,
            int states,
            int transitions,
            boolean isLattice,
            String svgHtml
    ) {}

    public record CompatibilityEntry(
            String first,
            String second,
            boolean compatible
    ) {}

    public record SharedLabelsEntry(
            String first,
            String second,
            List<String> labels
    ) {}

    public record GlobalComparisonDto(
            int globalStates,
            boolean globalIsLattice,
            boolean embeddingExists,
            double overApproximationRatio,
            Map<String, Boolean> roleTypeMatches
    ) {}
}
