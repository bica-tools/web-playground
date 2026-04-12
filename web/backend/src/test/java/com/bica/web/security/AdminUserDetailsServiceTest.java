package com.bica.web.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class AdminUserDetailsServiceTest {

    private AdminUserDetailsService service;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        service = new AdminUserDetailsService(passwordEncoder, "admin", "reticulate");
        service.init();
    }

    @Test
    void loadAdminUser() {
        var user = service.loadUserByUsername("admin");
        assertEquals("admin", user.getUsername());
        assertTrue(passwordEncoder.matches("reticulate", user.getPassword()));
        assertTrue(user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void unknownUserThrows() {
        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown"));
    }

    @Test
    void wrongPasswordDoesNotMatch() {
        var user = service.loadUserByUsername("admin");
        assertFalse(passwordEncoder.matches("wrong-password", user.getPassword()));
    }
}
