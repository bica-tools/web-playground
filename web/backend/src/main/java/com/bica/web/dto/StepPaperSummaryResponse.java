package com.bica.web.dto;

import java.util.List;

/** Lightweight DTO for step paper list pages (no abstract or full detail). */
public record StepPaperSummaryResponse(
        Long id,
        String stepNumber,
        String slug,
        String title,
        String phase,
        String status,
        String proofBacking,
        String grade,
        Integer wordCount,
        String tags,
        List<ReactionCountResponse> reactionCounts) {
}
