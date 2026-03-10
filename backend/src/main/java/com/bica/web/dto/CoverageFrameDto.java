package com.bica.web.dto;

public record CoverageFrameDto(
        String testName,
        String testKind,
        double transitionCoverage,
        double stateCoverage,
        String svgHtml
) {}
