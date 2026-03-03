package com.revhire.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.revhire.auth.security.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

/**
 * **********************************************************************************
 * SECURITY CONFIGURATION - REVHIRE PROJECT
 * **********************************************************************************
 * Author:    Aswathy J Lal
 * Date:      Saturday, Feb 28, 2026
 * Status:    Auth Implementation
 * * USAGE & ARCHITECTURE:
 * 1. RBAC (Role-Based Access Control): Uses .hasAuthority() instead of .hasRole()
 * to map directly to our User Entity's 'Role' Enum strings (SEEKER/EMPLOYER).
 * This ensures a 1:1 match with database values without hidden prefixes.
 * * 2. GRANULAR PROTECTION: Implements strict path-based security. While public 
 * searching is allowed, all sensitive actions (Apply, Edit, Post) are 
 * locked behind specific authority checks to prevent cross-account access.
 * * 3. SESSION MANAGEMENT: Handles full authentication lifecycle including 
 * custom success handling, session invalidation, and secure logout.
 * * 4. UX ENHANCEMENT: Includes a custom AuthenticationEntryPoint to redirect 
 * unauthorized users with context-aware parameters (?loginRequired=true).
 * **********************************************************************************
 */


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomLoginSuccessHandler successHandler;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Keep disabled for now to allow POST requests
            .authorizeHttpRequests(auth -> auth
            		// 1. THE ONLY PUBLIC PAGES
                    .requestMatchers("/").permitAll()                                  // Landing/Home
                    .requestMatchers("/jobs/search/**", "/jobs/search/results").permitAll() // Member D Search
                    .requestMatchers("/jobs/{id}").permitAll()                         // Member C Job View
                    .requestMatchers("/employers/{id}").permitAll()                    // Member C Company View
                    
                    // 2. AUTHENTICATION PATHS (Must be public so users can actually log in!)
                    .requestMatchers("/auth/login", "/auth/register/**", "/login").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                    
                    .requestMatchers("/profile/view/{id}", "/resume/view/{id}").authenticated()

                    // 3. SEEKER ONLY 
                    /* * Updated: 2026-03-03 11:40 by Aswathy J Lal
                     * Standardized on .hasAuthority() to match the User Entity 'Role' Enum strings.
                     * This avoids the hidden "ROLE_" prefix requirement of .hasRole().
                     */
                    .requestMatchers("/profile/edit/**", "/profile/save/**", "/profile/delete/**").hasAuthority("SEEKER")
                    .requestMatchers("/resume/upload/**", "/resume/builder/**").hasAuthority("SEEKER")
                    .requestMatchers("/profile/**", "/resume/**").hasAuthority("SEEKER")
                    .requestMatchers("/applications/**", "/favorites/**").hasAuthority("SEEKER")

                    // 4. EMPLOYER ONLY 
                    .requestMatchers("/employer/**", "/employers/profile/**").hasAuthority("EMPLOYER")
                    .requestMatchers("/jobs/create", "/jobs/{id}/edit", "/jobs/my").hasAuthority("EMPLOYER")
                    .requestMatchers("/auth/me").authenticated()
                    
                    // 5. REQUIRE LOGIN FOR EVERYTHING ELSE (Notifications, /auth/me, etc.)
                    .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/login") // Matches <form th:action="@{/login}">
                //.defaultSuccessUrl("/", true)
                .successHandler(successHandler)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // 5. CUSTOM ACCESS DENIED HANDLING
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/auth/denied") // Redirect here if a Seeker visits an Employer page
                .authenticationEntryPoint((request, response, authException) -> {
                    // Check if the user is trying to access a protected area
                    // and redirect with the 'loginRequired' parameter
                    response.sendRedirect("/auth/login?loginRequired=true");
                })
            );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}