package com.bica.web.dto;

import java.util.List;

public record TutorialDto(
        String id,
        int number,
        String title,
        String subtitle,
        List<TutorialStepDto> steps
) {}
