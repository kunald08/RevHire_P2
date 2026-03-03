package com.revhire.config;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.employer.repository.EmployerRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        Collection<? extends GrantedAuthority> authorities =
                authentication.getAuthorities();

        String email = authentication.getName();

        for (GrantedAuthority authority : authorities) {

            if (authority.getAuthority().equals("EMPLOYER")) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow();

                boolean profileExists =
                        employerRepository.findByUser(user).isPresent();

                if (!profileExists) {
                    response.sendRedirect("/employers/profile/create");
                } else {
                    response.sendRedirect("/jobs/my");
                }

                return;
            }

            if (authority.getAuthority().equals("SEEKER")) {
                response.sendRedirect("/jobs/search");
                return;
            }

            if (authority.getAuthority().equals("ADMIN")) {
                response.sendRedirect("/");
                return;
            }
        }

        response.sendRedirect("/");
    }
}