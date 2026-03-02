package com.bica.web.dto;

public record BenchmarkDto(
        String name,
        String description,
        String typeString,
        String pretty,
        int numStates,
        int numTransitions,
        int numSccs,
        boolean isLattice,
        boolean usesParallel,
        String svgHtml,
        String toolUrl,
        int numTests
) {}
