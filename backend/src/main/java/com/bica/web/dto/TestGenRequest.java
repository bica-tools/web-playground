package com.bica.web.dto;

public record TestGenRequest(
        String typeString,
        String className,
        String packageName,
        Integer maxRevisits
) {}
