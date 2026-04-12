package com.bica.web.dto;

import java.time.LocalDate;

public record VenuePaperRequest(
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
        boolean doubleBlindActive) {
}
