package com.bica.web.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private String encodedPassword;

    public AdminUserDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${bica.security.admin-username}") String adminUsername,
            @Value("${bica.security.admin-password}") String adminPassword) {
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @PostConstruct
    void init() {
        this.encodedPassword = passwordEncoder.encode(adminPassword);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!adminUsername.equals(username)) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return User.withUsername(adminUsername)
                .password(encodedPassword)
                .roles("ADMIN")
                .build();
    }
}
