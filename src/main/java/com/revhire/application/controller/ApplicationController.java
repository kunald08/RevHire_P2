package com.revhire.application.controller;

import com.revhire.application.dto.ApplicationRequest;
import com.revhire.application.dto.ApplicationResponse;
import com.revhire.application.dto.WithdrawRequest;
import com.revhire.application.service.ApplicationService;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
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
    private final ResumeRepository resumeRepository;
    private final ProfileRepository profileRepository;

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
        
        log.info("Showing apply form for job: {}, user: {}", jobId, 
            currentUser != null ? currentUser.getId() : "anonymous");
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) return "redirect:/auth/login";
            
            if (applicationService.hasApplied(jobId, userId)) {
                redirectAttributes.addFlashAttribute("infoMessage", "You have already applied for this job");
                return "redirect:/jobs/search/results";
            }
            
            List<Resume> userResumes = List.of();
            
            try {
                Optional<JobSeekerProfile> profileOpt = profileRepository.findByUserId(userId);
                
                if (profileOpt.isPresent()) {
                    JobSeekerProfile profile = profileOpt.get();
                    log.info("Found profile with ID: {} for user ID: {}", profile.getId(), userId);
                    
                    userResumes = resumeRepository.findByProfileId(profile.getId());
                    log.info("Found {} resumes for profile", userResumes.size());
                    
                    for (Resume resume : userResumes) {
                        log.info("Resume ID: {}, FileName: {}, Created: {}", 
                            resume.getId(), resume.getFileName(), resume.getCreatedAt());
                    }
                } else {
                    log.warn("No profile found for user ID: {}", userId);
                }
            } catch (Exception e) {
                log.error("Error fetching resumes: {}", e.getMessage(), e);
            }
            
            ApplicationRequest request = new ApplicationRequest();
            request.setJobId(jobId);
            
            model.addAttribute("applicationRequest", request);
            model.addAttribute("jobId", jobId);
            model.addAttribute("userResumes", userResumes);
            
            return "application/apply-job";
            
        } catch (Exception e) {
            log.error("Error showing apply form: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load application form");
            return "redirect:/jobs/search/results";
        }
    }

    @PostMapping("/apply/{jobId}")
    public String submitApplication(
            @PathVariable Long jobId,
            @Valid @ModelAttribute("applicationRequest") ApplicationRequest request,
            BindingResult result,
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) MultipartFile newResume,
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
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) return "redirect:/auth/login";
            
            // Double-check for duplicate before submitting
            if (applicationService.hasApplied(jobId, userId)) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "You have already applied for this job.");
                return "redirect:/jobs/search/results";
            }
            
            if (result.hasErrors()) {
                log.warn("Form has errors: {}", result.getAllErrors());
                return "application/apply-job";
            }
            
            // Handle resume
            if (resumeId != null && resumeId > 0) {
                // Using existing resume
                request.setResumeId(resumeId);
                log.info("Using existing resume with ID: {}", resumeId);
            } 
            else if (newResume != null && !newResume.isEmpty()) {
                // Upload new resume
                log.info("Uploading new resume: {}", newResume.getOriginalFilename());
                log.info("File size: {} bytes", newResume.getSize());
                log.info("Content type: {}", newResume.getContentType());
                
                // Validate file size
                if (newResume.getSize() > 2 * 1024 * 1024) {
                    log.warn("File size exceeds limit: {} bytes", newResume.getSize());
                    redirectAttributes.addFlashAttribute("errorMessage", "File size exceeds 2MB limit");
                    return "redirect:/applications/apply/" + jobId;
                }
                
                // Validate file type
                String filename = newResume.getOriginalFilename();
                if (filename == null || filename.isEmpty()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Invalid file name");
                    return "redirect:/applications/apply/" + jobId;
                }
                
                String ext = filename.toLowerCase();
                boolean isValidType = ext.endsWith(".pdf") || 
                                     ext.endsWith(".doc") || 
                                     ext.endsWith(".docx");
                
                if (!isValidType) {
                    log.warn("Invalid file extension: {}", ext);
                    redirectAttributes.addFlashAttribute("errorMessage", "Only PDF and DOC/DOCX files are allowed");
                    return "redirect:/applications/apply/" + jobId;
                }
                
                // Get user's profile
                Optional<JobSeekerProfile> profileOpt = profileRepository.findByUserId(userId);
                if (profileOpt.isEmpty()) {
                    log.error("Profile not found for user: {}", userId);
                    redirectAttributes.addFlashAttribute("errorMessage", "User profile not found");
                    return "redirect:/applications/apply/" + jobId;
                }
                
                JobSeekerProfile profile = profileOpt.get();
                
                // Save new resume with VERY SHORT file type (max 5 chars)
                try {
                    Resume savedResume = saveResume(newResume, profile);
                    request.setResumeId(savedResume.getId());
                    log.info("New resume saved with ID: {}, File: {}", savedResume.getId(), savedResume.getFileName());
                } catch (Exception e) {
                    log.error("Error saving resume: {}", e.getMessage(), e);
                    redirectAttributes.addFlashAttribute("errorMessage", "Failed to save resume. Please try again.");
                    return "redirect:/applications/apply/" + jobId;
                }
            } 
            else {
                log.warn("No resume selected");
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a resume");
                return "redirect:/applications/apply/" + jobId;
            }
            
            request.setJobId(jobId);
            log.info("Calling application service with jobId: {}, resumeId: {}", jobId, request.getResumeId());
            
            ApplicationResponse response = applicationService.applyForJob(userId, request);
            
            log.info("Application submitted successfully with ID: {}", response.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Application submitted successfully!");
            return "redirect:/applications";
            
        } catch (Exception e) {
            log.error("Error submitting application: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/applications/apply/" + jobId;
        } finally {
            log.info("========== SUBMIT APPLICATION END ==========");
        }
    }

    /**
     * Helper method to save a new resume with VERY SHORT file type
     */
    private Resume saveResume(MultipartFile file, JobSeekerProfile profile) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) filename = "resume";
        
        // Determine file type based ONLY on extension (very short)
        String fileType = "unk";
        String ext = filename.toLowerCase();
        
        if (ext.endsWith(".pdf")) {
            fileType = "pdf";
        } else if (ext.endsWith(".doc")) {
            fileType = "doc";
        } else if (ext.endsWith(".docx")) {
            fileType = "docx";
        }
        
        log.info("Saving resume - Filename: {}, FileType: {}", filename, fileType);
        
        Resume resume = Resume.builder()
                .profile(profile)
                .fileName(filename)
                .fileType(fileType)  // Using VERY short values: "pdf", "doc", "docx", "unk"
                .fileData(file.getBytes())
                .fileSize(file.getSize())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        return resumeRepository.save(resume);
    }

    @GetMapping
    public String myApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) return "redirect:/auth/login";
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
            Page<ApplicationResponse> applications = applicationService.getMyApplications(userId, pageable);
            
            model.addAttribute("applications", applications);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", applications.getTotalPages());
            
            return "application/my-applications";
            
        } catch (Exception e) {
            log.error("Error fetching applications: {}", e.getMessage(), e);
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
            if (userId == null) return "redirect:/auth/login";
            
            ApplicationResponse application = applicationService.getApplicationDetails(id, userId);
            
            log.info("Application data - ID: {}, JobID: {}, ResumeID: {}, ResumeFileName: {}", 
                application != null ? application.getId() : null,
                application != null ? application.getJobId() : null,
                application != null ? application.getResumeId() : null,
                application != null ? application.getResumeFileName() : null);
            
            model.addAttribute("application", application);
            
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
            if (userId == null) return "redirect:/auth/login";
            
            WithdrawRequest request = new WithdrawRequest();
            request.setReason(reason != null ? reason : "Withdrawn by user");
            
            applicationService.withdrawApplication(id, userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Application withdrawn successfully");
            
        } catch (Exception e) {
            log.error("Error withdrawing application: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/applications";
    }

    private Long getCurrentUserId(User currentUser) {
        if (currentUser != null) return currentUser.getId();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            try {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) return user.getId();
            } catch (Exception e) {
                log.error("Error finding user by email: {}", e.getMessage());
            }
        }
        return null;
    }
}