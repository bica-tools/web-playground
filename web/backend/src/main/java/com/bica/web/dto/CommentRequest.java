package com.bica.web.dto;

public record CommentRequest(
        String authorName,
        String authorEmail,
        String content,
        Long parentCommentId) {
}
