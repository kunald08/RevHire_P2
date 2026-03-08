package com.revhire.application.controller;

import com.revhire.application.dto.ApplicationRequest;
import com.revhire.application.dto.ApplicationResponse;
import com.revhire.application.dto.WithdrawRequest;
import com.revhire.application.service.ApplicationService;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import com.revhire.notification.service.NotificationService;
import com.revhire.common.enums.NotificationType;
import com.revhire.profile.entity.JobSeekerProfile;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.repository.ProfileRepository;
import com.revhire.profile.repository.ResumeRepository;
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
import java.util.Optional;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
@Log4j2
@PreAuthorize("hasRole('SEEKER')")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final NotificationService notificationService;

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
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            // Check if already applied
            if (applicationService.hasApplied(jobId, userId)) {
                redirectAttributes.addFlashAttribute("infoMessage", "You have already applied for this job");
                return "redirect:/jobs/search/results";
            }
            
            // Load job details
            Job job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Job not found");
                return "redirect:/jobs/search/results";
            }
            model.addAttribute("job", job);
            
            // Load user's resumes from profile
            List<Resume> resumes = List.of();
            Optional<JobSeekerProfile> profileOpt = profileRepository.findByUserId(userId);
            if (profileOpt.isPresent()) {
                resumes = resumeRepository.findByProfileId(profileOpt.get().getId());
            }
            model.addAttribute("resumes", resumes);
            
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
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.info("Submitting application for job: {}, user: {}", jobId, 
            currentUser != null ? currentUser.getId() : "anonymous");
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            if (result.hasErrors()) {
                // Reload form data
                Job job = jobRepository.findById(jobId).orElse(null);
                model.addAttribute("job", job);
                List<Resume> resumes = List.of();
                Optional<JobSeekerProfile> profileOpt = profileRepository.findByUserId(userId);
                if (profileOpt.isPresent()) {
                    resumes = resumeRepository.findByProfileId(profileOpt.get().getId());
                }
                model.addAttribute("resumes", resumes);
                model.addAttribute("jobId", jobId);
                return "application/apply-job";
            }
            
            request.setJobId(jobId);
            ApplicationResponse response = applicationService.applyForJob(userId, request);
            
            // Send notification to employer
            try {
                Job job = jobRepository.findById(jobId).orElse(null);
                if (job != null && job.getEmployer() != null && job.getEmployer().getUser() != null) {
                    User applicant = userRepository.findById(userId).orElse(null);
                    String applicantName = applicant != null ? applicant.getName() : "A candidate";
                    notificationService.createNotification(
                        job.getEmployer().getUser(),
                        applicantName + " applied for \"" + job.getTitle() + "\"",
                        NotificationType.APPLICATION_RECEIVED,
                        "/employer/applicants/" + response.getId()
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to send application notification: {}", e.getMessage());
            }
            
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
     * View all applications for the current user with status filtering
     */
    @GetMapping
    public String myApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.info("Fetching applications for user: {}, status filter: {}", 
            currentUser != null ? currentUser.getId() : "anonymous", status);
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
            
            // Parse status filter
            ApplicationStatus statusFilter = null;
            if (status != null && !status.isEmpty()) {
                try {
                    statusFilter = ApplicationStatus.valueOf(status);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status filter: {}", status);
                }
            }
            
            Page<ApplicationResponse> applications;
            if (statusFilter != null) {
                applications = applicationService.getMyApplicationsByStatus(userId, statusFilter, pageable);
            } else {
                applications = applicationService.getMyApplications(userId, pageable);
            }
            
            // Stat counts for dashboard cards
            long totalCount = applicationService.countByStatus(userId, null);
            long appliedCount = applicationService.countByStatus(userId, ApplicationStatus.APPLIED);
            long underReviewCount = applicationService.countByStatus(userId, ApplicationStatus.UNDER_REVIEW);
            long shortlistedCount = applicationService.countByStatus(userId, ApplicationStatus.SHORTLISTED);
            long rejectedCount = applicationService.countByStatus(userId, ApplicationStatus.REJECTED);
            long withdrawnCount = applicationService.countByStatus(userId, ApplicationStatus.WITHDRAWN);
            
            model.addAttribute("applications", applications);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", applications.getTotalPages());
            model.addAttribute("totalItems", applications.getTotalElements());
            model.addAttribute("activeStatus", status != null ? status : "ALL");
            
            // Stat counts
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("appliedCount", appliedCount);
            model.addAttribute("underReviewCount", underReviewCount);
            model.addAttribute("shortlistedCount", shortlistedCount);
            model.addAttribute("rejectedCount", rejectedCount);
            model.addAttribute("withdrawnCount", withdrawnCount);
            
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
            
            log.info("CONTROLLER DEBUG for app {}: jobTitle={}, status={}, coverLetter={}, appliedAt={}, jobId={}, jobDesc={}, resumeFile={}",
                application.getId(), application.getJobTitle(), application.getStatus(),
                application.getCoverLetter(), application.getAppliedAt(), application.getJobId(),
                application.getJobDescription() != null ? application.getJobDescription().substring(0, Math.min(30, application.getJobDescription().length())) : "null",
                application.getResumeFileName());
            
            model.addAttribute("appDetail", application);
            
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
            
            // Send notification to employer about withdrawal
            try {
                ApplicationResponse app = applicationService.getApplicationDetails(id, userId);
                if (app != null && app.getJobId() != null) {
                    Job job = jobRepository.findById(app.getJobId()).orElse(null);
                    if (job != null && job.getEmployer() != null && job.getEmployer().getUser() != null) {
                        User applicant = userRepository.findById(userId).orElse(null);
                        String applicantName = applicant != null ? applicant.getName() : "A candidate";
                        notificationService.createNotification(
                            job.getEmployer().getUser(),
                            applicantName + " withdrew their application for \"" + job.getTitle() + "\"",
                            NotificationType.APPLICATION_WITHDRAWN,
                            "/employer/jobs"
                        );
                    }
                }
            } catch (Exception ne) {
                log.warn("Failed to send withdraw notification: {}", ne.getMessage());
            }
            
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