package com.revhire.auth.service;

import com.revhire.auth.dto.RegisterRequest;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.Role;

import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    private final Map<String, RegisterRequest> pendingRegistrations = new ConcurrentHashMap<>();
    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();

    @Value("${mail.from.address}")
    private String fromEmail;

    private static class OtpData {
        String otp;
        long expiryTime;
        int resendCount;
        boolean isDummy; // New flag to distinguish real vs fake

        OtpData(String otp, long expiryTime, int resendCount, boolean isDummy) {
            this.otp = otp;
            this.expiryTime = expiryTime;
            this.resendCount = resendCount;
            this.isDummy = isDummy;
        }
    }

    private void sendHtmlEmail(String to, String subject, String title, String message, String otp, String actionUrl, String actionText) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            Context context = new Context();
            context.setVariable("title", title);
            context.setVariable("message", message);
            context.setVariable("otp", otp);
            context.setVariable("actionUrl", actionUrl);
            context.setVariable("actionText", actionText);
            context.setVariable("email", to);

            String htmlContent = templateEngine.process("auth/auth-action-mailer", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    @Override
    public void register(RegisterRequest request) {
        String email = request.getEmail();
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);

        if (userRepository.existsByEmail(email)) {
            // Enumeration Protection: Set a dummy OTP so the timer works, but send the "Already Exists" email
            otpStore.put(email, new OtpData("000000", expiryTime, 0, true));
            sendAlreadyRegisteredEmail(email);
            return;
        }

        validatePasswordStrength(request.getPassword());
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        pendingRegistrations.put(email, request);
        otpStore.put(email, new OtpData(otp, expiryTime, 0, false)); 

        sendHtmlEmail(email, "RevHire - Verify Your Email", "Welcome to RevHire!", 
            "Thank you for joining. Use the code below to verify your account.", otp, null, null);
    }

    private void sendAlreadyRegisteredEmail(String email) {
        sendHtmlEmail(email, "RevHire - Security Notification", "Account Already Exists", 
            "You tried to register, but you already have an account. Please login or reset your password if needed.", 
            null, "http://localhost:8080/auth/login", "Login Now");
    }

    public void confirmRegistration(String email, String userOtp) {
        OtpData data = otpStore.get(email);
        
        if (data == null) {
            throw new RuntimeException("No pending verification found.");
        }

        // If it's a dummy or wrong OTP, we just say "Invalid OTP" 
        // This prevents attackers from knowing the account exists via the error message
        if (data.isDummy || !data.otp.equals(userOtp)) {
            throw new RuntimeException("Invalid OTP!");
        }

        RegisterRequest request = pendingRegistrations.get(email);
        if (request == null) {
            throw new RuntimeException("Session expired. Please register again.");
        }

        if (System.currentTimeMillis() > data.expiryTime) {
            otpStore.remove(email);
            pendingRegistrations.remove(email);
            throw new RuntimeException("OTP has expired!");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .location(request.getLocation())
                .role(Role.valueOf(request.getRole().toUpperCase()))
                .build();

        userRepository.save(user);
        otpStore.remove(email);
        pendingRegistrations.remove(email);
    }

    @Scheduled(fixedRate = 600000) 
    public void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        otpStore.entrySet().removeIf(entry -> now > entry.getValue().expiryTime);
        pendingRegistrations.keySet().removeIf(email -> !otpStore.containsKey(email));
    }

    @Override
    @Transactional // Ensures the otpStore update and email logic are bundled
    public void resendRegistrationOtp(String email) {
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);
        System.out.println("DEBUG: Resend requested for: " + email);

        if (userRepository.existsByEmail(email)) {
            System.out.println("DEBUG: User exists. Sending 'Already Registered' mail.");
            
            // 1. Create dummy data to keep the timer running
            otpStore.put(email, new OtpData("000000", expiryTime, 0, true));
            
            // 2. Trigger the email
            sendAlreadyRegisteredEmail(email);
            return; 
        }

        // Normal flow for new users
        RegisterRequest request = pendingRegistrations.get(email);
        OtpData oldData = otpStore.get(email);

        if (request != null && oldData != null) {
            if (oldData.resendCount >= 3) {
                throw new RuntimeException("Maximum resend attempts reached.");
            }

            String newOtp = String.format("%06d", new Random().nextInt(999999));
            otpStore.put(email, new OtpData(newOtp, expiryTime, oldData.resendCount + 1, false));

            sendHtmlEmail(email, "RevHire - New Verification Code", "Your New Code", 
                "As requested, here is your new verification code.", newOtp, null, null);
        } else {
            // Log this to see if the session is actually null
            System.out.println("DEBUG: Resend failed - No pending registration for " + email);
            throw new RuntimeException("Registration session expired. Please register again.");
        }
    }

    @Override
    public long getRemainingSeconds(String email) {
        OtpData data = otpStore.get(email);
        if (data == null) return 0;
        
        long seconds = (data.expiryTime - System.currentTimeMillis()) / 1000;
        return seconds > 0 ? seconds : 0;
    }

    // --- OTHER METHODS (changePassword, resetPassword, etc.) REMAIN SAME ---
    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) throw new RuntimeException("Incorrect current password.");
        validatePasswordStrength(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void sendForgotPasswordLink(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(System.currentTimeMillis() + (15 * 60 * 1000));
            userRepository.save(user);
            String resetLink = "http://localhost:8080/auth/reset-password?token=" + token;
            sendHtmlEmail(user.getEmail(), "RevHire - Reset Password", "Reset Request", "Click below to reset.", null, resetLink, "Reset Password");
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token).orElseThrow(() -> new RuntimeException("Invalid link."));
        if (user.getResetTokenExpiry() < System.currentTimeMillis()) throw new RuntimeException("Link expired.");
        validatePasswordStrength(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        userRepository.save(user);
    }

    private void validatePasswordStrength(String password) {
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        if (password == null || !password.matches(pattern)) {
            throw new RuntimeException("Password too weak.");
        }
    }
}