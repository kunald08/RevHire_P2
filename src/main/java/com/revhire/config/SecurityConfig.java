package com.revhire.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.revhire.auth.security.CustomLoginSuccessHandler;
import com.revhire.auth.security.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

/**
 * **********************************************************************************
 * SECURITY CONFIGURATION - REVHIRE PROJECT
 * **********************************************************************************
 * Author:    Aswathy J Lal
 * Date:      Tuesday, March 03, 2026
 * Status:    Auth Implementation - Corrected & Finalized
 * * USAGE & ARCHITECTURE:
 * 1. RBAC (Role-Based Access Control): Uses .hasAuthority() instead of .hasRole()
 * to map directly to our User Entity's 'Role' Enum strings (SEEKER/EMPLOYER).
 * 2. GRANULAR PROTECTION: Implements strict path-based security. 
 * 3. GLOBAL ERROR HANDLING: Permitted /error to allow custom 404/500 views.
 * 4. UX ENHANCEMENT: Redirects unauthorized users with context-aware parameters.
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
                    // 1. PUBLIC PAGES & GLOBAL ERROR HANDLING
                    .requestMatchers("/").permitAll()                                  
                    .requestMatchers("/error", "/auth/denied").permitAll()             // CRITICAL: Allows 404/403 pages to be seen
                    .requestMatchers("/jobs/search/**", "/jobs/search/results").permitAll() 
                    

                    /* * NOTE: The Regex {id:[0-9]+} is used here to enforce strict input validation 
                     * at the security layer (preventing non-numeric IDs from reaching the service).
                     *
                     * SIDE EFFECT: This strict matching changes the timing of Entity loading. 
                     * If you experience 'ClassCastException' in Thymeleaf when comparing Enums, 
                     * ensure the template uses String-based comparison (#strings.toString()) 
                     * to handle Hibernate Proxies correctly.
                     */
                    .requestMatchers("/jobs/{id:\\d+}/").permitAll()   
                    .requestMatchers("/employers/{id:\\d+}/").permitAll()  
                    .requestMatchers("/auth/forgot-password", "/auth/reset-password","/auth/resend-otp", "/auth/verify-login/**", "/auth/login/otp-request/**").permitAll()
                    
                    // 2. AUTHENTICATION & STATIC ASSETS
                    .requestMatchers("/auth/login", "/auth/register/**","/auth/verify",  "/login").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                    
                    // 3. COMMON AUTHENTICATED AREA
                    .requestMatchers("/profile/view/{id}", "/resume/view/{id}").authenticated()
                    .requestMatchers("/auth/me").authenticated()


                    // 4. SEEKER ONLY (Standardized on .hasAuthority to match User Entity Enum)
                    .requestMatchers("/profile/edit/**", "/profile/save/**", "/profile/delete/**").hasAuthority("SEEKER")
                    .requestMatchers("/resume/upload/**", "/resume/builder/**").hasAuthority("SEEKER")
                    .requestMatchers("/profile/**", "/resume/**").hasAuthority("SEEKER")
                    .requestMatchers("/applications/**", "/favorites/**").hasAuthority("SEEKER")

                    // 5. EMPLOYER ONLY 
                    .requestMatchers("/employer/**", "/employers/profile/**").hasAuthority("EMPLOYER")
                    
                    
                    // 6. CATCH-ALL PROTECTION
                    .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/login") 
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
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/auth/denied") 
                .authenticationEntryPoint((request, response, authException) -> {
                    // Redirects unauthorized guest access to login
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