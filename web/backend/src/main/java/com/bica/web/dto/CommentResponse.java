package com.bica.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String authorName,
        String content,
        LocalDateTime createdAt,
        List<CommentResponse> replies) {
}
