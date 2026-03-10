package com.bica.web.dto;

import java.util.List;
import java.util.Map;

public record GlobalAnalyzeResponse(
        String pretty,
        int numStates,
        int numTransitions,
        int numSccs,
        boolean isLattice,
        String counterexample,
        List<String> roles,
        int numRoles,
        boolean usesParallel,
        boolean isRecursive,
        String svgHtml,
        String dotSource,
        Map<String, ProjectionDto> projections
) {
    public record ProjectionDto(
            String role,
            String localType,
            int localStates,
            int localTransitions,
            boolean localIsLattice,
            String localSvgHtml
    ) {}
}
