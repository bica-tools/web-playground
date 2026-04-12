package com.bica.web.dto;

import java.util.List;

public record ProgrammeStatsResponse(
        long totalPapers,
        long totalWords,
        long provedCount,
        List<PhaseStatsResponse> phases) {
}
