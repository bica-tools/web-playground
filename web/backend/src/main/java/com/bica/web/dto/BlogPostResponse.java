package com.bica.web.dto;

import java.time.LocalDateTime;

public record BlogPostResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String content,
        Integer arc,
        Integer sequence,
        String tags,
        String author,
        boolean published,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt) {
}
