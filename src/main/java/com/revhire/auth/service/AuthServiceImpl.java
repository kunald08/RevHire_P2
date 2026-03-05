package com.revhire.auth.service;

import com.revhire.auth.dto.RegisterRequest;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.Role;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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

    private final Map<String, RegisterRequest> pendingRegistrations = new ConcurrentHashMap<>();
    
    // 1. Ensure you only have ONE declaration of otpStore using OtpData
    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();

    @Value("${mail.from.address}")
    private String fromEmail;

    // 2. Helper class for expiration logic
    private static class OtpData {
        String otp;
        long expiryTime;

        OtpData(String otp, long expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered!");
        }
        validatePasswordStrength(request.getPassword());

        String otp = String.format("%06d", new Random().nextInt(999999));

        // 3. Set expiry to 5 minutes from now
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);

        pendingRegistrations.put(request.getEmail(), request);
        otpStore.put(request.getEmail(), new OtpData(otp, expiryTime));

        sendOtpEmail(request.getEmail(), otp);
    }
    
    
    public void confirmRegistration(String email, String userOtp) {
        OtpData data = otpStore.get(email);
        RegisterRequest request = pendingRegistrations.get(email);

        // 4. Check if OTP exists
        if (data == null || request == null) {
            throw new RuntimeException("No pending registration found or OTP expired.");
        }

        // 5. Check if OTP has expired
        if (System.currentTimeMillis() > data.expiryTime) {
            otpStore.remove(email);
            pendingRegistrations.remove(email);
            throw new RuntimeException("OTP has expired! Please register again.");
        }

        // 6. Check if OTP matches
        if (!data.otp.equals(userOtp)) {
            throw new RuntimeException("Invalid OTP!");
        }

        // Final Step: Save to Database
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .location(request.getLocation())
                .role(Role.valueOf(request.getRole().toUpperCase()))
                .build();

        userRepository.save(user);

        // Cleanup
        otpStore.remove(email);
        pendingRegistrations.remove(email);
    }

    private void sendOtpEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("RevHire - Verify Your Email");
        message.setText("Your verification code is: " + otp + "\nThis code will expire shortly.");
        mailSender.send(message);
    }

    private void validatePasswordStrength(String password) {
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        if (password == null || !password.matches(pattern)) {
            throw new RuntimeException("Password must be at least 8 characters long and " +
                    "include uppercase, lowercase, a number, and a special character.");
        }
    }
    
//    Automatic Background Cleanup
    
    @Scheduled(fixedRate = 600000) // Runs every 10 minutes
    public void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        // Remove if expired
        otpStore.entrySet().removeIf(entry -> now > entry.getValue().expiryTime);
        
        // Also cleanup pending registrations that have no matching active OTP
        pendingRegistrations.keySet().removeIf(email -> !otpStore.containsKey(email));
    }
    
    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        // 1. Fetch the user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Verify Old Password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("The current password you entered is incorrect.");
        }

        // 3. Check that New Password is not the same as Old Password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password cannot be the same as your current password!");
        }

        // 4. Validate Strength (Reuse your existing method)
        validatePasswordStrength(newPassword);

        // 5. Update and Save
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        sendPasswordChangeNotification(email);
    }
    
    private void sendPasswordChangeNotification(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("RevHire Security Alert: Password Changed");
        message.setText("Hello,\n\nThis is a confirmation that the password for your RevHire account was recently changed. " +
                "If you did not perform this action, please contact our support team immediately.\n\n" +
                "Securely,\nThe RevHire Team");
        mailSender.send(message);
    }

}