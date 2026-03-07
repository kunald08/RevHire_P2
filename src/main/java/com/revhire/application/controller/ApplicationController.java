package com.revhire.application.controller;

import com.revhire.application.dto.ApplicationRequest;
import com.revhire.application.dto.ApplicationResponse;
import com.revhire.application.dto.WithdrawRequest;
import com.revhire.application.service.ApplicationService;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
@Log4j2
@PreAuthorize("hasRole('SEEKER')")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserRepository userRepository;

    /**
     * Handle login redirects to the correct login page
     */
    @GetMapping("/login")
    public String redirectToAuthLogin() {
        log.info("Redirecting to auth login page");
        return "redirect:/auth/login";
    }

    /**
     * Show apply form for a specific job
     */
    @GetMapping("/apply/{jobId}")
    public String showApplyForm(
            @PathVariable Long jobId,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.info("Showing apply form for job: {}, user: {}", jobId, 
            currentUser != null ? currentUser.getId() : "anonymous");
        
        try {
            // Get authenticated user ID
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            // Check if already applied
            if (applicationService.hasApplied(jobId, userId)) {
                redirectAttributes.addFlashAttribute("infoMessage", "You have already applied for this job");
                return "redirect:/jobs/search/results";
            }
            
            ApplicationRequest request = new ApplicationRequest();
            request.setJobId(jobId);
            
            model.addAttribute("applicationRequest", request);
            model.addAttribute("jobId", jobId);
            
            return "application/apply-job";
            
        } catch (Exception e) {
            log.error("Error showing apply form: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load application form");
            return "redirect:/jobs/search/results";
        }
    }

    /**
     * Submit a new application
     */
    @PostMapping("/apply/{jobId}")
    public String submitApplication(
            @PathVariable Long jobId,
            @Valid @ModelAttribute("applicationRequest") ApplicationRequest request,
            BindingResult result,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        
        log.info("Submitting application for job: {}, user: {}", jobId, 
            currentUser != null ? currentUser.getId() : "anonymous");
        
        try {
            // Get authenticated user ID
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            if (result.hasErrors()) {
                return "application/apply-job";
            }
            
            request.setJobId(jobId);
            ApplicationResponse response = applicationService.applyForJob(userId, request);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Application submitted successfully!");
            return "redirect:/applications";
            
        } catch (Exception e) {
            log.error("Error submitting application: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/applications/apply/" + jobId;
        }
    }

    /**
     * View all applications for the current user
     */
    @GetMapping
    public String myApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.info("Fetching applications for user: {}", 
            currentUser != null ? currentUser.getId() : "anonymous");
        
        try {
            // Get authenticated user ID
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
            Page<ApplicationResponse> applications = applicationService.getMyApplications(userId, pageable);
            
            model.addAttribute("applications", applications);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", applications.getTotalPages());
            model.addAttribute("totalItems", applications.getTotalElements());
            
            return "application/my-applications";
            
        } catch (Exception e) {
            log.error("Error fetching applications: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load your applications");
            return "redirect:/jobs/search";
        }
    }

    /**
     * View details of a specific application
     */
    @GetMapping("/{id}")
    public String viewApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.info("Viewing application: {} for user: {}", id, 
            currentUser != null ? currentUser.getId() : "anonymous");
        
        try {
            // Get authenticated user ID
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            ApplicationResponse application = applicationService.getApplicationDetails(id, userId);
            model.addAttribute("application", application);
            
            // Check if withdrawable
            boolean canWithdraw = application != null && 
                application.getStatus() != null && 
                !"WITHDRAWN".equals(application.getStatus().toString()) &&
                !"SHORTLISTED".equals(application.getStatus().toString()) &&
                !"REJECTED".equals(application.getStatus().toString());
            
            model.addAttribute("canWithdraw", canWithdraw);
            
            return "application/application-detail";
            
        } catch (Exception e) {
            log.error("Error viewing application: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Application not found");
            return "redirect:/applications";
        }
    }

    /**
     * Withdraw an application - FIXED VERSION
     */
    @PostMapping("/{id}/withdraw")
    public String withdrawApplication(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        
        log.info("Withdrawing application: {} for user: {}", id, 
            currentUser != null ? currentUser.getId() : "anonymous");
        
        try {
            // Get authenticated user ID
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            // Create withdraw request
            WithdrawRequest request = new WithdrawRequest();
            if (reason != null && !reason.isEmpty()) {
                request.setReason(reason);
            } else {
                request.setReason("Withdrawn by user");
            }
            
            // Call service to withdraw
            applicationService.withdrawApplication(id, userId, request);
            
            redirectAttributes.addFlashAttribute("successMessage", "Application withdrawn successfully");
            log.info("Application {} withdrawn successfully by user {}", id, userId);
            
        } catch (Exception e) {
            log.error("Error withdrawing application: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to withdraw application: " + e.getMessage());
        }
        
        return "redirect:/applications";
    }

    /**
     * Helper method to get current user ID from either AuthenticationPrincipal or SecurityContext
     */
    private Long getCurrentUserId(User currentUser) {
        if (currentUser != null) {
            return currentUser.getId();
        }
        
        // Try SecurityContextHolder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                return user.getId();
            }
        }
        
        return null;
    }

    /**
     * Handle exceptions for this controller
     */
    @ExceptionHandler(Exception.class)
    public String handleError(Exception ex, RedirectAttributes redirectAttributes) {
        log.error("Controller error: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute("errorMessage", "An error occurred: " + ex.getMessage());
        return "redirect:/applications";
    }
}