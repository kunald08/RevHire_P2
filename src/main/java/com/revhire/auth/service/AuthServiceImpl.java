package com.revhire.auth.service;

import com.revhire.auth.dto.RegisterRequest;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.Role;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	private static final Logger logger = LogManager.getLogger(AuthServiceImpl.class);
	
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
        	logger.debug("Preparing to send HTML email to: {} | Subject: {}", to, subject);
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
            logger.info("Email successfully sent to: {}", to);
        } catch (Exception e) {
        	logger.error("SMTP Error: Failed to send email to {}. Reason: {}", to, e.getMessage());
        
        }
    }

    @Override
    public void register(RegisterRequest request) {
        String email = request.getEmail();
        logger.info("Service: Processing registration for {}", email);
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);

        if (userRepository.existsByEmail(email)) {
        	logger.warn("Security: Registration attempt for existing email: {}. Triggering enumeration protection.", email);
            // Enumeration Protection: Set a dummy OTP so the timer works, but send the "Already Exists" email
            otpStore.put(email, new OtpData("000000", expiryTime, 0, true));
            sendAlreadyRegisteredEmail(email);
            return;
        }

        validatePasswordStrength(request.getPassword());
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        pendingRegistrations.put(email, request);
        otpStore.put(email, new OtpData(otp, expiryTime, 0, false)); 
        
        logger.info("Service: Generated OTP for new user {}. Dispatching welcome email.", email);
        sendHtmlEmail(email, "RevHire - Verify Your Email", "Welcome to RevHire!", 
            "Thank you for joining. Use the code below to verify your account.", otp, null, null);
    }

    private void sendAlreadyRegisteredEmail(String email) {
        sendHtmlEmail(email, "RevHire - Security Notification", "Account Already Exists", 
            "You tried to register, but you already have an account. Please login or reset your password if needed.", 
            null, "http://localhost:8080/auth/login", "Login Now");
    }

    public void confirmRegistration(String email, String userOtp) {
    	logger.info("Service: Confirming registration for {}", email);
        OtpData data = otpStore.get(email);
        
        if (data == null) {
        	logger.warn("Service: No OTP data found in memory for {}", email);
            throw new RuntimeException("No pending verification found.");
        }

        // If it's a dummy or wrong OTP, we just say "Invalid OTP" 
        // This prevents attackers from knowing the account exists via the error message
        if (data.isDummy || !data.otp.equals(userOtp)) {
        	logger.error("Security: Invalid OTP entered for {}. (IsDummy: {})", email, data.isDummy);
            throw new RuntimeException("Invalid OTP!");
        }

        RegisterRequest request = pendingRegistrations.get(email);
        if (request == null) {
            throw new RuntimeException("Session expired. Please register again.");
        }

        if (System.currentTimeMillis() > data.expiryTime) {
        	logger.warn("Service: OTP expired for {}", email);
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
        logger.info("Service: Successfully persisted new user {} to database.", email);
        
        otpStore.remove(email);
        pendingRegistrations.remove(email);
    }

    @Scheduled(fixedRate = 600000) 
    public void cleanExpiredEntries() {
    	logger.debug("Scheduled Task: Cleaning up expired OTPs and registrations.");
        long now = System.currentTimeMillis();
        otpStore.entrySet().removeIf(entry -> now > entry.getValue().expiryTime);
        pendingRegistrations.keySet().removeIf(email -> !otpStore.containsKey(email));
        logger.debug("Scheduled Task: Cleanup complete.");
    }

   @Override
	@Transactional
	public void resendRegistrationOtp(String email) {
	   logger.info("Service: Resend OTP requested for {}", email);
	   
	    long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);
	    
	    OtpData oldData = otpStore.get(email);
	
	    // 1. Check limit
	    if (oldData != null && oldData.resendCount >= 3) {
	    	logger.warn("Service: Resend limit reached for {}", email);
	        throw new RuntimeException("LIMIT_REACHED");
	    }
	
	    int nextCount = (oldData != null) ? oldData.resendCount + 1 : 1;
	    int remaining = 3 - nextCount;
	
	    // 2. Handle Existing User
	    if (userRepository.existsByEmail(email)) {
	    	logger.info("Service: Resending 'Already Registered' notice to {}", email);
	        otpStore.put(email, new OtpData("000000", expiryTime, nextCount, true));
	        sendAlreadyRegisteredEmail(email);
	        // Throwing a custom message allows the frontend to see the count even on "success"
	        throw new RuntimeException("SENT_REMAINING:" + remaining); 
	    }
	
	    // 3. Handle Real User
	    RegisterRequest request = pendingRegistrations.get(email);
	    if (request != null && oldData != null) {
	        String newOtp = String.format("%06d", new Random().nextInt(999999));
	        otpStore.put(email, new OtpData(newOtp, expiryTime, nextCount, false));
	        sendHtmlEmail(email, "RevHire - New Verification Code", "Your New Code", 
	            "As requested, here is your new verification code.", newOtp, null, null);
	        
	        logger.info("Service: Resent new OTP to {}. Attempts remaining: {}", email, remaining);
	        // Throwing this helps the AJAX handler update the "Attempts Left" label
	        throw new RuntimeException("SENT_REMAINING:" + remaining);
	    } else {
	    	logger.error("Service: Resend failed for {}. Session likely expired.", email);
	        throw new RuntimeException("SESSION_EXPIRED");
	    }
	}
   
   @Override
   public int getResendCount(String email) {
       OtpData data = otpStore.get(email);
       return (data != null) ? data.resendCount : 0;
   }
    @Override
    public long getRemainingSeconds(String email) {
        OtpData data = otpStore.get(email);
        if (data == null) return 0;
        
        long seconds = (data.expiryTime - System.currentTimeMillis()) / 1000;
        return seconds > 0 ? seconds : 0;
    }

    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
    	logger.info("Service: Attempting password change for user: {}", email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            logger.error("Service: Password change failed. User {} not found.", email);
            return new RuntimeException("User not found");
        });
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            logger.warn("Security: Password change rejected for {}. Current password mismatch.", email);
            throw new RuntimeException("Incorrect current password.");
        }
        validatePasswordStrength(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Service: Password successfully updated for {}.", email);
    }

    @Override
    @Transactional
    public void sendForgotPasswordLink(String email) {
    	logger.info("Service: Forgot password request received for: {}", email);
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(System.currentTimeMillis() + (15 * 60 * 1000));
            userRepository.save(user);
            String resetLink = "http://localhost:8080/auth/reset-password?token=" + token;
            logger.info("Service: Generated reset token for {}. Dispatching email link.", email);
            sendHtmlEmail(user.getEmail(), "RevHire - Reset Password", "Reset Request", "Click below to reset.", null, resetLink, "Reset Password");
        }, () -> {
            // Silently log to prevent account enumeration, but don't throw error to UI
            logger.warn("Security: Forgot password requested for non-existent email: {}", email);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
    	logger.info("Service: Attempting password reset via token.");
        User user = userRepository.findByResetToken(token).orElseThrow(() -> {
            logger.warn("Security: Password reset failed. Invalid or used token.");
            return new RuntimeException("Invalid link.");
        });
        if (user.getResetTokenExpiry() < System.currentTimeMillis()){
            logger.warn("Security: Password reset failed for {}. Token expired.", user.getEmail());
            throw new RuntimeException("Link expired.");
        }
        validatePasswordStrength(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        userRepository.save(user);
        logger.info("Service: Password successfully reset for user: {}", user.getEmail());
    }

    private void validatePasswordStrength(String password) {
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        if (password == null || !password.matches(pattern)) {
        	logger.warn("Security: Password strength validation failed.");
            throw new RuntimeException("Password too weak.");
        }
    }
    
    @Override
    public void sendLoginOtp(String email) {
    	logger.info("Service: Generating Login OTP for {}", email);
        // 1. Ensure user exists
        if (!userRepository.existsByEmail(email)) {
        	logger.warn("Security: Login OTP requested for non-existent user: {}", email);
            throw new RuntimeException("User not found");
        }

        // 2. Generate and store OTP (Reusable logic from your register method)
        String otp = String.format("%06d", new Random().nextInt(999999));
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);
        otpStore.put(email, new OtpData(otp, expiryTime, 0, false));

        logger.info("Service: Login OTP generated for {}. Sending email.", email);
        // 3. Send Email
        sendHtmlEmail(email, "RevHire - Login OTP", "Your Login Code", 
            "Use the code below to sign in to your account.", otp, null, null);
    }
    
    @Override
    @Transactional
    public void resendLoginOtp(String email) {
    	logger.info("Service: Resend Login OTP requested for {}", email);
        // 1. Check if user exists (Crucial for Login)
        if (!userRepository.existsByEmail(email)) {
        	logger.warn("Security: Resend Login OTP failed. User {} does not exist.", email);
            throw new RuntimeException("USER_NOT_FOUND");
        }

        OtpData oldData = otpStore.get(email);
        
        // 2. Check Resend Limit
        if (oldData != null && oldData.resendCount >= 3) {
        	logger.warn("Service: Resend Login OTP limit reached for {}", email);
            throw new RuntimeException("LIMIT_REACHED");
        }

        int nextCount = (oldData != null) ? oldData.resendCount + 1 : 1;
        String newOtp = String.format("%06d", new Random().nextInt(999999));
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);

        // 3. Store and Send (Login specific)
        otpStore.put(email, new OtpData(newOtp, expiryTime, nextCount, false));
        
        logger.info("Service: New Login OTP sent to {}. Attempt #{}", email, nextCount);
        sendHtmlEmail(email, "RevHire - Your Login Code", "New Login OTP", 
            "Here is your requested login verification code.", newOtp, null, null);
        
        int remaining = 3 - nextCount;
        throw new RuntimeException("SENT_REMAINING:" + remaining);
    }
    
    @Override
    public void verifyLoginOtp(String email, String userOtp) {
    	logger.info("Service: Verifying Login OTP for {}", email);
        OtpData data = otpStore.get(email);

        if (data == null) {
        	logger.warn("Service: Verification failed. No login session found for {}", email);
            throw new RuntimeException("No login session found.");
        }

        if (System.currentTimeMillis() > data.expiryTime) {
        	logger.warn("Service: Login OTP expired for {}", email);
            otpStore.remove(email);
            throw new RuntimeException("OTP has expired.");
        }

        if (!data.otp.equals(userOtp)) {
        	logger.error("Security: Invalid Login OTP entry for {}", email);
            throw new RuntimeException("Invalid OTP!");
        }
        
        logger.info("Service: Login OTP verified successfully for {}", email);
        otpStore.remove(email); // Success, clear the OTP
    }

    @Override
    public void authenticateUserManually(String email, HttpServletRequest request) {
    	logger.info("Service: Initiating manual authentication for {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Service: Manual auth failed. User {} not found.", email);
                    return new RuntimeException("User not found");
                });
        // Create UserDetails for Spring Security
        org.springframework.security.core.userdetails.UserDetails userDetails = 
            org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .build();

        // Create Authentication Token
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth = 
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // Link authentication to the current request session
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Persist the session so the user stays logged in while browsing
        request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", 
            org.springframework.security.core.context.SecurityContextHolder.getContext());
        
        logger.info("Service: Manual authentication complete. SecurityContext updated for {}", email);
    }
}