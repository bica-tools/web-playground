package com.bica.web.dto;

import java.util.List;

public record ProgrammeStatusDto(
        int totalSteps,
        int acceptedSteps,
        int totalModules,
        int totalTests,
        List<StepEvaluationDto> steps
) {}
