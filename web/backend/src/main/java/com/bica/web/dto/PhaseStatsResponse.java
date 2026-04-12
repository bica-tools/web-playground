package com.bica.web.dto;

public record PhaseStatsResponse(
        String phase,
        long totalPapers,
        long completedPapers,
        long provedPapers) {
}
