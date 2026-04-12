package com.bica.web.dto;

public record BlogPostRequest(
        String slug,
        String title,
        String summary,
        String content,
        Integer arc,
        Integer sequence,
        String tags,
        String author,
        boolean published) {
}
