package com.bica.web.dto;

import java.util.List;

public record CompareResponse(
        String pretty1,
        String pretty2,
        // Subtyping
        boolean type1SubtypeOfType2,
        boolean type2SubtypeOfType1,
        String subtypingRelation,
        // Duality
        String dual1,
        String dual2,
        boolean areDuals,
        // State spaces
        int states1,
        int transitions1,
        boolean isLattice1,
        String svgHtml1,
        int states2,
        int transitions2,
        boolean isLattice2,
        String svgHtml2,
        // Chomsky
        String chomsky1,
        String chomsky2,
        // Recursion
        boolean isRecursive1,
        boolean isGuarded1,
        boolean isContractive1,
        boolean isTailRecursive1,
        boolean isRecursive2,
        boolean isGuarded2,
        boolean isContractive2,
        boolean isTailRecursive2,
        // Method diff
        List<String> sharedMethods,
        List<String> uniqueMethods1,
        List<String> uniqueMethods2
) {}
