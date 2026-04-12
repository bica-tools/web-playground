package com.bica.web.dto;

import java.util.List;

public record StepEvaluationDto(
        String stepNumber,
        String title,
        String grade,
        int score,
        boolean accepted,
        List<String> fixes
) {}
