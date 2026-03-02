package com.bica.web.dto;

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
        String dotSource
) {}
