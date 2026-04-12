package com.bica.web.dto;

public record SubtypeResponse(
        String prettySubtype,
        String prettySupertype,
        boolean isSubtype,
        boolean isEquivalent,
        String relation,
        int subtypeStates,
        int supertypeStates,
        String svgSubtype,
        String svgSupertype
) {}
