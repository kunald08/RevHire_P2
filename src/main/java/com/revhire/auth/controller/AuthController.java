package com.revhire.auth.controller;

import com.revhire.auth.dto.RegisterRequest;
import com.revhire.auth.service.AuthService;
import com.revhire.auth.service.AuthServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Authentication and Identity Management.
 * Handles Registration, OTP verification, Login flows, and Password Management.
 */

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final Logger logger = LogManager.getLogger(AuthController.class);
    private final AuthService authService;

    // ==================== Basic Navigation ====================

    @GetMapping("/login")
    public String login() {
        return "auth/login"; 
    }

    @GetMapping("/register")
    public String registerChoice() {
        return "auth/register-choice";
    }

    @GetMapping("/register/seeker")
    public String seekerForm(Model model) {
        model.addAttribute("user", new RegisterRequest());
        return "auth/register-seeker";
    }

    @GetMapping("/register/employer")
    public String employerForm(Model model) {
        model.addAttribute("user", new RegisterRequest());
        return "auth/register-employer";
    }

 // ==================== Registration Flow ====================
    
@PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") RegisterRequest request, Model model) {
        logger.info("New registration attempt for email: {} [Role: {}]", request.getEmail(), request.getRole());
        try {
            authService.register(request);
            logger.info("Registration request saved. Redirecting {} to OTP verification.", request.getEmail());
            return "redirect:/auth/verify?email=" + request.getEmail();
        } catch (RuntimeException e) {
            logger.warn("Registration failed for {}: {}", request.getEmail(), e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", request);
            String role = request.getRole() != null ? request.getRole().toLowerCase() : "seeker";
            return "auth/register-" + role;
        }
    }

    @GetMapping("/verify")
    public String showVerifyPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("isLogin", false); // ADDED: Required for dynamic HTML
        
        model.addAttribute("secondsRemaining", authService.getRemainingSeconds(email)); 
        model.addAttribute("resendCount", authService.getResendCount(email));
        
        return "auth/verify-otp";
    }
    
   
    
    @PostMapping("/verify")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp, Model model) {
        logger.info("Verifying registration OTP for: {}", email);
        try {
            ((AuthServiceImpl) authService).confirmRegistration(email, otp);
            logger.info("Account successfully verified and activated for: {}", email);
            return "redirect:/auth/login?success=Account verified! Please login.";
        } catch (RuntimeException e) {
            logger.error("Registration OTP mismatch for {}: {}", email, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "auth/verify-otp";
        }
    }
    
    // ==================== OTP Login Flow ====================
    @GetMapping("/login/otp-request")
    public String requestLoginOtp(@RequestParam String email, RedirectAttributes ra) {
    	logger.info("OTP Login requested for: {}", email);
        try {
            authService.sendLoginOtp(email); 
            System.out.println("DEBUG: OTP sent successfully, redirecting...");
            return "redirect:/auth/verify-login?email=" + email;
        } catch (Exception e) {
        	logger.error("Failed to generate login OTP for {}: {}", email, e.getMessage());
            ra.addFlashAttribute("error", "Could not send OTP. Please try again.");
            return "redirect:/auth/login";
        }
    }
    
    @GetMapping("/verify-login")
    public String showLoginVerifyPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("isLogin", true); // ADDED: Required for dynamic HTML
        
        model.addAttribute("secondsRemaining", authService.getRemainingSeconds(email)); 
        model.addAttribute("resendCount", authService.getResendCount(email));
        
        return "auth/verify-otp"; 
    }
    
    @PostMapping("/verify-login")
    public String verifyLogin(@RequestParam String email, @RequestParam String otp, HttpServletRequest request, Model model) {
    	logger.info("Attempting OTP Login verification for: {}", email);
        try {
            authService.verifyLoginOtp(email, otp);
            authService.authenticateUserManually(email, request);
            logger.info("User {} authenticated successfully via OTP. Session established.", email);
            return "redirect:/"; // Success!
        } catch (RuntimeException e) {
        	logger.warn("Login OTP verification failed for {}: {}", email, e.getMessage());
        	model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("isLogin", true);
            model.addAttribute("resendCount", authService.getResendCount(email));
            model.addAttribute("secondsRemaining", authService.getRemainingSeconds(email));
            return "auth/verify-otp";
        }
    }

    

    
 // ==================== Password Management ====================

    
    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String processChangePassword(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestParam String oldPassword,
                                        @RequestParam String newPassword,
                                        @RequestParam String confirmPassword,
                                        HttpServletRequest  request,
                                        Model model) {
    	String email = userDetails.getUsername();
    	logger.info("Change password request initiated by: {}", email);
        // 1. Check if new passwords match
        if (!newPassword.equals(confirmPassword)) {
        	logger.warn("Password change failed for {}: Passwords do not match.", email);
            model.addAttribute("error", "New passwords do not match!");
            return "auth/change-password";
        }

        try {
            // 2. Call service logic
            authService.changePassword(userDetails.getUsername(), oldPassword, newPassword);
            logger.info("Password updated successfully for {}. Invalidating session for re-login.", email);
            // 3. Auto-logout logic
            request.getSession().invalidate(); // Clear session
            
         // Change 'success' to 'pwUpdated'
            return "redirect:/auth/login?pwUpdated=Password updated! Please login with your new password.";
        } catch (RuntimeException e) {
        	logger.error("Password change error for {}: {}", email, e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "auth/change-password";
        }
    }
    
//    ===============Forgot password=======================
// 1. Show Forgot Password Request Page
    @GetMapping("/forgot-password")
    public String showForgotPasswordRequest() {
        return "auth/forgot-password-request";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email) {
        logger.info("Forgot password link request for: {}", email);
        try {
            authService.sendForgotPasswordLink(email);
        } catch (Exception e) {
            logger.error("Forgot password service error for {}: {}", email, e.getMessage());
        }
        return "redirect:/auth/login?info=If an account exists for " + email + ", a reset link has been sent.";
    }
    
    // 3. Show the actual New Password Form (from the Email link)
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    // 4. Process the New Password
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token, 
                                       @RequestParam String newPassword, 
                                       @RequestParam String confirmPassword, 
                                       Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
        try {
            authService.resetPassword(token, newPassword);
            logger.info("Password reset successful via token.");
            return "redirect:/auth/login?pwUpdated=Password reset successful! Please login.";
        } catch (RuntimeException e) {
            logger.error("Reset password failed: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
    }
    
 // ==================== Utilities & Security ====================
    
    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam String email, 
                            @RequestParam(defaultValue = "register") String type, 
                            RedirectAttributes ra) {
        logger.info("Resend OTP ({}) requested for: {}", type, email);
        try {
            if ("login".equals(type)) {
                authService.resendLoginOtp(email);
            } else {
                authService.resendRegistrationOtp(email);
            }
            ra.addFlashAttribute("resendSuccess", "true");
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("SENT_REMAINING")) {
                ra.addFlashAttribute("resendSuccess", "true");
            } else {
                logger.warn("OTP Resend rejected for {}: {}", email, msg);
                ra.addFlashAttribute("error", msg);
            }
        }
        ra.addAttribute("email", email);
        return "login".equals(type) ? "redirect:/auth/verify-login" : "redirect:/auth/verify";
    }
    
    /**
     * Handles 403 Access Denied errors (e.g., Seeker trying to access Employer pages)
     * Matches the .accessDeniedPage("/auth/denied") in SecurityConfig
     */
    @GetMapping("/denied")
    public String accessDenied(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = (userDetails != null) ? userDetails.getUsername() : "Anonymous";
        logger.warn("Unauthorized Access Attempt: User {} denied from restricted resource.", username);
        
        model.addAttribute("errorCode", "403");
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", "Oops! You don't have permission to access this area.");
        return "error"; 
    }
    
    @GetMapping("/me")
    public String showAccountSettings(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login?loginRequired=true";
        }

        // Add the user object to the model so we can show "Welcome, [Email]"
        model.addAttribute("user", userDetails);
        
        // Check role just to show/hide specific links on the settings page
        boolean isEmployer = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("EMPLOYER"));
        model.addAttribute("isEmployer", isEmployer);

        return "auth/account-settings"; // The HTML page for common features
    }

}