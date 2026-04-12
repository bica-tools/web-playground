package com.bica.web.dto;

import java.util.List;

public record CoverageStoryboardResponse(
        int totalTransitions,
        int totalStates,
        List<CoverageFrameDto> frames
) {}
