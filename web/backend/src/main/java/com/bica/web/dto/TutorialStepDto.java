package com.bica.web.dto;

public record TutorialStepDto(
        String title,
        String prose,
        String code,
        String codeLabel,
        String interactiveType
) {}
