package com.revhire.auth.controller;

import com.revhire.auth.dto.RegisterRequest;
import com.revhire.auth.service.AuthService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
            return "redirect:/auth/login?success";
        } catch (RuntimeException e) {
            // Log the error and send the message back to the specific form
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", request); // Keep the data in the form so they don't have to re-type
            
            // Determine which form to return to based on the role
            String role = request.getRole() != null ? request.getRole().toLowerCase() : "seeker";
            return "auth/register-" + role;
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
}