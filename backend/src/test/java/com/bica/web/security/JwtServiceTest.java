package com.bica.web.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "bica-reticulate-session-types-2026-secret-key-minimum-256-bits-long";
    private static final long EXPIRY_HOURS = 24;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRY_HOURS);

    @Test
    void generateAndValidateToken() {
        String token = jwtService.generateToken("admin");
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void extractUsername() {
        String token = jwtService.generateToken("admin");
        assertEquals("admin", jwtService.extractUsername(token));
    }

    @Test
    void extractExpiration() {
        String token = jwtService.generateToken("admin");
        var expiration = jwtService.extractExpiration(token);
        assertNotNull(expiration);
        assertTrue(expiration.getTime() > System.currentTimeMillis());
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertFalse(jwtService.validateToken("not.a.valid.token"));
    }

    @Test
    void tamperedTokenReturnsFalse() {
        String token = jwtService.generateToken("admin");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtService.validateToken(tampered));
    }

    @Test
    void emptyTokenReturnsFalse() {
        assertFalse(jwtService.validateToken(""));
    }

    @Test
    void differentUsersGetDifferentTokens() {
        String token1 = jwtService.generateToken("admin");
        String token2 = jwtService.generateToken("other");
        assertNotEquals(token1, token2);
        assertEquals("admin", jwtService.extractUsername(token1));
        assertEquals("other", jwtService.extractUsername(token2));
    }

    @Test
    void secretTooShortThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtService("short", EXPIRY_HOURS));
    }
}
