package com.revhire.auth.security;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.employer.repository.EmployerRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager; // Added
import org.apache.logging.log4j.Logger;     // Added
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Custom handler to redirect users to different landing pages based on their Roles
 * and Profile completion status after a successful login.
 */
@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LogManager.getLogger(CustomLoginSuccessHandler.class);

    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String email = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        logger.info("Authentication Successful for user: {} | Authorities: {}", email, authorities);

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();

            if (role.equals("EMPLOYER")) {
                logger.info("Processing Employer login flow for: {}", email);
                
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> {
                            logger.error("Database mismatch: Authentication succeeded for {}, but user not found in DB.", email);
                            return new RuntimeException("User not found");
                        });

                boolean profileExists = employerRepository.findByUser(user).isPresent();

                if (!profileExists) {
                    logger.warn("Employer profile missing for {}. Redirecting to profile creation.", email);
                    response.sendRedirect("/employers/profile/create");
                } else {
                    logger.info("Employer profile found for {}. Redirecting to dashboard.", email);
                    response.sendRedirect("/employer/dashboard");
                }
                return;
            }

            if (role.equals("SEEKER")) {
                logger.info("User {} logged in as SEEKER. Redirecting to job search.", email);
                response.sendRedirect("/jobs/search");
                return;
            }

            if (role.equals("ADMIN")) {
                logger.info("Admin user {} logged in. Redirecting to home.", email);
                response.sendRedirect("/");
                return;
            }
        }

        logger.warn("User {} logged in with no recognized authority. Redirecting to default home.", email);
        response.sendRedirect("/");
    }
}