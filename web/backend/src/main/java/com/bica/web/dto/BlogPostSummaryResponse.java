package com.bica.web.dto;

import java.time.LocalDateTime;

/** Lightweight DTO for blog list pages (no full content). */
public record BlogPostSummaryResponse(
        Long id,
        String slug,
        String title,
        String summary,
        Integer arc,
        Integer sequence,
        String tags,
        String author,
        LocalDateTime publishedAt) {
}
