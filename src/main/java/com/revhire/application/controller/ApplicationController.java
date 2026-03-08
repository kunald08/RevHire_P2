package com.revhire.application.controller;

import com.revhire.application.dto.ApplicationRequest;
import com.revhire.application.dto.ApplicationResponse;
import com.revhire.application.dto.WithdrawRequest;
import com.revhire.application.service.ApplicationService;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.service.ResumeService;
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

import java.util.List;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
@Log4j2
@PreAuthorize("hasRole('SEEKER')")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserRepository userRepository;
    private final ResumeService resumeService;

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
            
            // Get user email for ResumeService
            String userEmail = getCurrentUserEmail(currentUser);
            if (userEmail == null) {
                return "redirect:/auth/login";
            }
            
            // Check for duplicate application
            boolean hasApplied = applicationService.hasApplied(jobId, userId);
            if (hasApplied) {
                log.info("User {} has already applied for job {}", userId, jobId);
                model.addAttribute("alreadyApplied", true);
            }
            
            // Get user's resumes for dropdown
            List<Resume> resumes = resumeService.getResumesByEmail(userEmail);
            log.info("Found {} resumes for user {}", resumes.size(), userEmail);
            
            ApplicationRequest request = new ApplicationRequest();
            request.setJobId(jobId);
            
            model.addAttribute("applicationRequest", request);
            model.addAttribute("jobId", jobId);
            model.addAttribute("resumes", resumes);
            
            return "application/apply-job";
            
        } catch (Exception e) {
            log.error("Error showing apply form: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load application form: " + e.getMessage());
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
        
        log.info("========== SUBMIT APPLICATION ==========");
        log.info("Submitting application for job: {}, user: {}", jobId, 
            currentUser != null ? currentUser.getId() : "anonymous");
        
        // Log the complete request details
        if (request != null) {
            log.info("Request details - jobId: {}, resumeId: {}, coverLetter length: {}", 
                request.getJobId(),
                request.getResumeId(),
                request.getCoverLetter() != null ? request.getCoverLetter().length() : 0);
            
            // Check if new resume is present
            if (request.getNewResume() != null && !request.getNewResume().isEmpty()) {
                log.info("New resume file present - Name: {}, Size: {}, ContentType: {}", 
                    request.getNewResume().getOriginalFilename(),
                    request.getNewResume().getSize(),
                    request.getNewResume().getContentType());
            } else {
                log.warn("No new resume file in request");
            }
        }
        
        try {
            // Get authenticated user ID
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            // Double-check for duplicate before submitting
            if (applicationService.hasApplied(jobId, userId)) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "You have already applied for this job.");
                return "redirect:/jobs/search/results";
            }
            
            if (result.hasErrors()) {
                log.error("Form validation errors: {}", result.getAllErrors());
                return "application/apply-job";
            }
            
            // Validate that either resumeId is provided OR newResume is provided
            if ((request.getResumeId() == null || request.getResumeId() == 0) && 
                (request.getNewResume() == null || request.getNewResume().isEmpty())) {
                
                log.error("No resume selected or uploaded");
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Please select an existing resume or upload a new one.");
                return "redirect:/applications/apply/" + jobId;
            }
            
            request.setJobId(jobId);
            ApplicationResponse response = applicationService.applyForJob(userId, request);
            
            log.info("Application submitted successfully - ID: {}, resumeId: {}, resumeFileName: {}", 
                response.getId(), response.getResumeId(), response.getResumeFileName());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Application submitted successfully!");
            return "redirect:/applications";
            
        } catch (Exception e) {
            log.error("Error submitting application: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to submit application: " + e.getMessage());
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
            
            log.info("Application data - ID: {}, JobID: {}, ResumeID: {}, ResumeFileName: {}", 
                application != null ? application.getId() : null,
                application != null ? application.getJobId() : null,
                application != null ? application.getResumeId() : null,
                application != null ? application.getResumeFileName() : null);
            
            model.addAttribute("application", application);
            
            // Explicitly add IDs to model for the template
            if (application != null) {
                model.addAttribute("applicationId", application.getId());
                model.addAttribute("jobId", application.getJobId());
            }
            
            // Check if withdrawable
            boolean canWithdraw = application != null && application.isWithdrawable();
            model.addAttribute("canWithdraw", canWithdraw);
            
            return "application/application-detail";
            
        } catch (Exception e) {
            log.error("Error viewing application: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Application not found");
            return "redirect:/applications";
        }
    }

    /**
     * Withdraw an application
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
     * Helper method to get current user email
     */
    private String getCurrentUserEmail(User currentUser) {
        if (currentUser != null && currentUser.getEmail() != null) {
            return currentUser.getEmail();
        }
        
        // Try SecurityContextHolder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
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