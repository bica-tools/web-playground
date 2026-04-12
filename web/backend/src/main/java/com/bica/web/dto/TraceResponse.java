package com.bica.web.dto;

import java.util.List;
import java.util.Map;

public record TraceResponse(
        boolean valid,
        boolean complete,
        int currentState,
        List<Map<String, Object>> path,
        String violationAt,
        boolean atEnd,
        List<String> enabledAtEnd,
        String prettyType
) {}
