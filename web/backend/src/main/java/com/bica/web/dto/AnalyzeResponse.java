package com.bica.web.dto;

import java.util.List;

public record AnalyzeResponse(
        String pretty,
        int numStates,
        int numTransitions,
        int numSccs,
        boolean isLattice,
        String counterexample,
        boolean terminates,
        boolean wfParallel,
        String svgHtml,
        String dotSource,
        boolean usesParallel,
        boolean isRecursive,
        int recDepth,
        List<String> methods,
        int numMethods,
        boolean threadSafe,
        int numTests,
        int numValidPaths,
        int numViolations,
        int numIncomplete
) {}
