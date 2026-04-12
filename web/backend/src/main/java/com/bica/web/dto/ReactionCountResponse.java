package com.bica.web.dto;

public record ReactionCountResponse(
        String reactionType,
        long count) {
}
