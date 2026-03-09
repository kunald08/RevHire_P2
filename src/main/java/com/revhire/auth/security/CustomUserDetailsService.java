package com.revhire.auth.security;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager; // Added
import org.apache.logging.log4j.Logger;     // Added
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LogManager.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        logger.debug("Attempting to load user details for security context: {}", email);

        // 1. Fetch user from your database using the email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Security Alert: Login attempt with non-existent email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        logger.info("User identity verified in DB for email: {} | Role: {}", user.getEmail(), user.getRole());

        // 2. Convert your User entity into a Spring Security "UserDetails" object
        // Note: We log the role to verify the authorities are being mapped correctly
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}