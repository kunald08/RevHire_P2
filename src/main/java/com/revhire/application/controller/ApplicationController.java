package com.revhire.application.controller;

import com.revhire.application.dto.ApplicationRequest;
import com.revhire.application.dto.ApplicationResponse;
import com.revhire.application.dto.WithdrawRequest;
import com.revhire.application.service.ApplicationService;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.common.enums.NotificationType;
import com.revhire.employer.service.EmployerService;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import com.revhire.notification.service.NotificationService;
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
    private final EmployerService employerService;

    @GetMapping("/login")
    public String redirectToAuthLogin() {
        return "redirect:/auth/login";
    }

    @GetMapping("/apply/{jobId}")
    public String showApplyForm(
            @PathVariable Long jobId,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            if (applicationService.hasApplied(jobId, userId)) {
                redirectAttributes.addFlashAttribute("infoMessage", "You have already applied for this job");
                return "redirect:/jobs/search/results";
            }
            
            Job job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Job not found");
                return "redirect:/jobs/search/results";
            }
            model.addAttribute("job", job);
            
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

    @PostMapping("/apply/{jobId}")
    public String submitApplication(
            @PathVariable Long jobId,
            @Valid @ModelAttribute("applicationRequest") ApplicationRequest request,
            BindingResult result,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            if (result.hasErrors()) {
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
                    User employerUser = job.getEmployer().getUser();
                    User applicant = userRepository.findById(userId).orElse(null);
                    String applicantName = applicant != null ? applicant.getName() : "A candidate";
                    
                    notificationService.createNotification(
                        employerUser,
                        applicantName + " applied for \"" + job.getTitle() + "\"",
                        NotificationType.APPLICATION_RECEIVED,
                        "/employer/applicants/" + response.getId()
                    );
                }
            } catch (Exception e) {
                log.error("Failed to send application notification: {}", e.getMessage());
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Application submitted successfully!");
            return "redirect:/applications";
            
        } catch (Exception e) {
            log.error("Error submitting application: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/applications/apply/" + jobId;
        }
    }

    @GetMapping
    public String myApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
            
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

    @GetMapping("/{id}")
    public String viewApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            ApplicationResponse application = applicationService.getApplicationDetails(id, userId);
            
            model.addAttribute("appDetail", application);
            
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

    @PostMapping("/{id}/withdraw")
    public String withdrawApplication(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            WithdrawRequest request = new WithdrawRequest();
            request.setReason(reason != null ? reason : "Withdrawn by user");
            
            applicationService.withdrawApplication(id, userId, request);
            
            // Send withdrawal notification to employer
            try {
                ApplicationResponse app = applicationService.getApplicationDetails(id, userId);
                if (app != null && app.getJobId() != null) {
                    Job job = jobRepository.findById(app.getJobId()).orElse(null);
                    if (job != null && job.getEmployer() != null && job.getEmployer().getUser() != null) {
                        User employerUser = job.getEmployer().getUser();
                        User applicant = userRepository.findById(userId).orElse(null);
                        String applicantName = applicant != null ? applicant.getName() : "A candidate";
                        
                        notificationService.createNotification(
                            employerUser,
                            applicantName + " withdrew their application for \"" + job.getTitle() + "\"",
                            NotificationType.APPLICATION_WITHDRAWN,
                            "/employer/jobs"
                        );
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send withdrawal notification: {}", e.getMessage());
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Application withdrawn successfully");
            
        } catch (Exception e) {
            log.error("Error withdrawing application: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/applications";
    }

    private Long getCurrentUserId(User currentUser) {
        if (currentUser != null) {
            return currentUser.getId();
        }
        
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

    @ExceptionHandler(Exception.class)
    public String handleError(Exception ex, RedirectAttributes redirectAttributes) {
        log.error("Controller error: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute("errorMessage", "An error occurred: " + ex.getMessage());
        return "redirect:/applications";
    }
    
}