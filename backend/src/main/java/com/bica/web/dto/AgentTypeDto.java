package com.bica.web.dto;

public record AgentTypeDto(
        String name,
        String protocol,
        String sessionType,
        String description,
        String transport
) {}
