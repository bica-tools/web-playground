package com.bica.web.dto;

import java.util.List;

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
        int numTests,
        boolean isRecursive,
        int recDepth,
        int numMethods,
        List<String> methods,
        boolean threadSafe,
        int numValidPaths,
        int numViolations,
        int numIncomplete,
        List<String> tags
) {}
