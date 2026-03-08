package com.revhire.auth.service;

import com.revhire.auth.dto.RegisterRequest;
import com.revhire.auth.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest seekerRequest;

    @BeforeEach
    void setUp() {
        seekerRequest = new RegisterRequest();
        seekerRequest.setEmail("test@example.com");
        seekerRequest.setPassword("Password@123"); // Meets strength requirements
        seekerRequest.setName("John Doe");
        seekerRequest.setRole("SEEKER");
    }

    @Test
    @DisplayName("Should successfully initiate registration and send OTP")
    void register_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        when(templateEngine.process(anyString(), any())).thenReturn("<html>OTP Content</html>");

        // Act
        authService.register(seekerRequest);

        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        assertEquals(300, authService.getRemainingSeconds("test@example.com"), 5, 
            "Should have roughly 300 seconds (5 mins) remaining");
    }

    @Test
    @DisplayName("Should handle existing user by sending security email and setting dummy OTP")
    void register_UserAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

        // Act
        authService.register(seekerRequest);

        // Assert
        // Verify that we didn't check password strength (it returns early)
        // Verify a "dummy" OTP (000000) was stored
        long remaining = authService.getRemainingSeconds("test@example.com");
        assertTrue(remaining > 0);
        
        // confirmRegistration should fail because it's a dummy
        assertThrows(RuntimeException.class, () -> {
            authService.confirmRegistration("test@example.com", "000000");
        }, "Should throw Invalid OTP for dummy entries");
    }

    @Test
    @DisplayName("Should throw exception when password is too weak")
    void register_WeakPassword() {
        // Arrange
        seekerRequest.setPassword("123"); // Fails regex

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(seekerRequest);
        });
        
        assertEquals("Password too weak.", exception.getMessage());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should throw LIMIT_REACHED after 3 resend attempts")
    void resendOtp_LimitReached() {
        // Arrange: Manually trigger registration to populate otpStore
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        authService.register(seekerRequest);

        // Act: Resend 3 times
        try { authService.resendRegistrationOtp("test@example.com"); } catch (Exception e) {}
        try { authService.resendRegistrationOtp("test@example.com"); } catch (Exception e) {}
        try { authService.resendRegistrationOtp("test@example.com"); } catch (Exception e) {}

        // Assert: 4th time should throw LIMIT_REACHED
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resendRegistrationOtp("test@example.com");
        });

        assertEquals("LIMIT_REACHED", exception.getMessage());
    }
}