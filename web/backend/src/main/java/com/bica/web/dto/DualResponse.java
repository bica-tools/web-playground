package com.bica.web.dto;

public record DualResponse(
        String prettyOriginal,
        String prettyDual,
        boolean isInvolution,
        boolean isIsomorphic,
        boolean selectionFlipped,
        int originalStates,
        int dualStates,
        String svgOriginal,
        String svgDual
) {}
