package com.revhire.auth.controller;

import com.revhire.auth.dto.RegisterRequest;
import com.revhire.auth.service.AuthService;
import com.revhire.auth.service.AuthServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") RegisterRequest request, Model model) {
        try {
            authService.register(request);
            // Redirect to verification page with email as a parameter
            return "redirect:/auth/verify?email=" + request.getEmail();
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", request);
            String role = request.getRole() != null ? request.getRole().toLowerCase() : "seeker";
            return "auth/register-" + role;
        }
    }

    @GetMapping("/verify")
    public String showVerifyPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        // 300 seconds = 5 minutes
        model.addAttribute("secondsRemaining", 300); 
        return "auth/verify-otp";
    }
    
    @PostMapping("/verify")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp, Model model) {
        try {
            ((AuthServiceImpl) authService).confirmRegistration(email, otp);
            return "redirect:/auth/login?success=Account verified! Please login.";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "auth/verify-otp";
        }
    }
    
    /**
     * Handles 403 Access Denied errors (e.g., Seeker trying to access Employer pages)
     * Matches the .accessDeniedPage("/auth/denied") in SecurityConfig
     */
    @GetMapping("/denied")
    public String accessDenied(Model model) {
        model.addAttribute("errorCode", "403");
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", "Oops! You don't have permission to access this area. " +
                "Employers and Seekers have different dashboards.");
        return "auth/error"; 
    }
    
//    handles the 404 page not found error.
//    @GetMapping("/not-found")
//    public String notFound(Model model) {
//        model.addAttribute("errorCode", "404");
//        model.addAttribute("errorTitle", "Page Not Found");
//        model.addAttribute("errorMessage", "The page you are looking for doesn't exist or has been moved.");
//        return "auth/error"; 
//    }
    
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
    
//  ===============Change password=======================

    
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
        // 1. Check if new passwords match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New passwords do not match!");
            return "auth/change-password";
        }

        try {
            // 2. Call service logic
            authService.changePassword(userDetails.getUsername(), oldPassword, newPassword);

            // 3. Auto-logout logic
            request.getSession().invalidate(); // Clear session
            
         // Change 'success' to 'pwUpdated'
            return "redirect:/auth/login?pwUpdated=Password updated! Please login with your new password.";
        } catch (RuntimeException e) {
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

    // 2. Process the Request and send Email
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        try {
            authService.sendForgotPasswordLink(email);
            return "redirect:/auth/login?info=Check your email for the password reset link!";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/forgot-password-request";
        }
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
            return "redirect:/auth/login?pwUpdated=Password reset successful! Please login.";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
    }
}