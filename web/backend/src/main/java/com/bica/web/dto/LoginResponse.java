package com.bica.web.dto;

public record LoginResponse(String token, long expiresAt) {
}
