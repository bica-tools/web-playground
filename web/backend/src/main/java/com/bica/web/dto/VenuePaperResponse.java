package com.bica.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VenuePaperResponse(
        Long id,
        String slug,
        String title,
        String abstractText,
        String venue,
        String status,
        String pdfPath,
        LocalDate submissionDate,
        LocalDate decisionDate,
        String stepsCovered,
        String doi,
        String arxivId,
        String bibtex,
        boolean doubleBlindActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
