package com.revhire.employer.controller;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.employer.dto.ApplicantProfileDTO;
import com.revhire.employer.dto.ApplicantRowDTO;
import com.revhire.employer.dto.ApplicationNoteDTO;
import com.revhire.employer.service.ApplicantService;
import com.revhire.employer.service.EmployerService;
import com.revhire.job.entity.Job;
import com.revhire.profile.dto.ProfileResponse;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.service.ProfileService;
import com.revhire.profile.service.ResumeService;
import com.revhire.employer.dto.BulkActionDTO;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/employer")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantService applicantService;
    private final ProfileService profileService;
    private final ResumeService resumeService;
    private final EmployerService employerService;

    /**
     * Populates companyName for the employer sidebar on all views.
     * Uses a fast single-column query instead of loading the full employer profile.
     */
    @ModelAttribute
    public void addCommonAttributes(Authentication authentication, Model model) {
        if (authentication != null) {
            String name = employerService.getCompanyName(authentication.getName());
            if (name != null) {
                model.addAttribute("companyName", name);
            }
        }
    }

    // =====================================================
    // 1️⃣ Employer Jobs List (Already Working)
    // =====================================================

    @GetMapping("/jobs")
    public String getEmployerJobs(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model,
            Authentication authentication) {
    	 System.out.println("DEBUG: Searching with keyword: " + keyword+"'");
        Pageable pageable = PageRequest.of(page, size);

        Page<Job> jobsPage =
                applicantService.getEmployerJobsWithApplications(
                        authentication.getName(),
                        keyword,
                        pageable);

        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", size);
        model.addAttribute("isReviewMode", false);
        model.addAttribute("activeMenu", "applicants");
       
        return "employer/employer-jobs-list";
    }

    // =====================================================
    // 2️⃣ View Applicants of Specific Job
    // URL: /employer/jobs/{jobId}/applicants
    // =====================================================

    @GetMapping("/jobs/{jobId}/applicants")
    public String viewApplicantsByJob(
            @PathVariable Long jobId,
            Model model) {

        List<ApplicantRowDTO> applicants =
                applicantService.getApplicantsByJob(jobId);

        model.addAttribute("applicants", applicants);
        model.addAttribute("jobTitle",
                applicantService.getJobTitle(jobId));
        model.addAttribute("totalApplicants",
                applicantService.getApplicantCount(jobId));
        model.addAttribute("jobId", jobId);
        model.addAttribute("isReviewMode", false);
        model.addAttribute("activeMenu", "applicants");

        return "employer/applicant-list";
    }
    
 // This single method now handles both standard jobs and review mode jobs
    @PostMapping({"/jobs/{jobId}/applicants/bulk-{action}", "/jobs/review/{jobId}/applicants/bulk-{action}"})
    @ResponseBody
    public String bulkUpdateStatus(
            @PathVariable Long jobId, 
            @PathVariable String action, 
            @RequestBody BulkActionDTO dto) {
        
        // Normalize action string if necessary (e.g., lowercase to uppercase)
        String formattedAction = action.toUpperCase(); 

        applicantService.bulkUpdateStatus(
                dto.getApplicationIds(), 
                formattedAction, 
                dto.getComment()
        );

        return "Applicants " + action.toLowerCase() + "ed successfully";
    }

    

    @GetMapping("/applicants/{appId}")
    public String viewApplicant(@PathVariable Long appId, Model model, Authentication auth) {
        ApplicantProfileDTO applicant = applicantService.getApplicantProfile(appId);
        ProfileResponse profile = profileService.getProfileByUserId(applicant.getSeekerId());
        
        // Fetch the latest resume using the profile ID
        Resume latestResume = applicantService.getLatestResumeByProfileId(profile.getId());
        
        model.addAttribute("applicant", applicant);
        model.addAttribute("profile", profile);
        model.addAttribute("existingNote", applicantService.getNoteForApplication(appId));
        model.addAttribute("resume", latestResume); // Now accessible in Thymeleaf as ${resume}
        model.addAttribute("activeMenu", "applicants");
        
        return "employer/applicant-profile";
    }

	@PostMapping("/applicants/process-action")
	public String processAction(
	        @RequestParam Long appId, 
	        @RequestParam String action, 
	        @RequestParam(required = false) String comment) {
	    
	    // Ensure 'action' matches what your Service expects (e.g., "SHORTLIST" or "REJECT")
	    applicantService.bulkUpdateStatus(
	            List.of(appId), 
	            action, 
	            (comment != null && !comment.isEmpty()) ? comment : (action + " via profile")
	    );
	    
	    return "redirect:/employer/applicants/" + appId;
	}
	// =====================================================
    // Reviewer Notes Endpoints
    // =====================================================

    // Endpoint: /employer/notes/{appId}/addnote
    @PostMapping("/notes/{appId}/addnote")
    public String addNote(@PathVariable Long appId, 
                          @RequestParam String note, Authentication authentication) {
        applicantService.addNote(appId, note, authentication.getName());
        return "redirect:/employer/applicants/" + appId;
    }

    // Endpoint: /employer/notes/{noteId}/clear
    @PostMapping("/notes/{noteId}/clear")
    public String clearNote(@PathVariable Long noteId, 
                            @RequestParam Long appId) {
        
        applicantService.deleteNote(noteId);
        // Redirect back to the specific applicant profile
        return "redirect:/employer/applicants/" + appId;
    }
 
    
    @GetMapping("/jobs/review")
    public String getPendingReviews(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        String email = authentication != null ? authentication.getName() : "";
        
        // 1. Get filtered jobs
        Page<Job> jobsPage = applicantService.getJobsWithPendingApplications(email, keyword, pageable);
        
        // 2. Pre-calculate pending counts for this specific page
        Map<Long, Long> pendingCounts = new HashMap<>();
        for (Job job : jobsPage.getContent()) {
            pendingCounts.put(job.getId(), applicantService.getPendingApplicantCount(job.getId()));
        }
        
        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("pendingCounts", pendingCounts);
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", size);
        model.addAttribute("isReviewMode", true);
        model.addAttribute("activeMenu", "review");

        return "employer/employer-jobs-list"; 
    }
    @GetMapping("/jobs/review/{id}/applicants")
    public String getReviewApplicants(@PathVariable Long id, Model model) {
        List<ApplicationStatus> pendingStatuses = List.of(
                ApplicationStatus.APPLIED, 
                ApplicationStatus.UNDER_REVIEW
        );
        
        List<ApplicantRowDTO> applicants = applicantService.getFilteredApplicantsByJob(id, pendingStatuses);
        
        model.addAttribute("applicants", applicants);
        model.addAttribute("jobTitle", applicantService.getJobTitle(id));
        model.addAttribute("jobId", id);
        model.addAttribute("isReviewMode", true); // Used in HTML to show/hide certain UI elements
        model.addAttribute("activeMenu", "review");
        
        return "employer/applicant-list"; 
    }
    
    @GetMapping("/resume/download/{id}")
    public ResponseEntity<?> downloadResume(@PathVariable Long id) {
        Resume resume = resumeService.downloadResume(id);

        if (resume == null || resume.getFileData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body("Resume file not found.");
        }

        String contentType = "PDF".equals(resume.getFileType())
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + 
                        (resume.getFileName() != null ? resume.getFileName() : "resume.pdf") + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resume.getFileData());
    }

    @GetMapping("/resume/view/{id}")
    public ResponseEntity<?> viewResume(@PathVariable Long id) {
        Resume resume = resumeService.downloadResume(id);

        if (resume == null || resume.getFileData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body("Resume file not found.");
        }

        String contentType = "PDF".equals(resume.getFileType())
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + 
                        (resume.getFileName() != null ? resume.getFileName() : "resume.pdf") + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(resume.getFileData().length)
                .body(resume.getFileData());
    }
    @GetMapping("/jobs/{jobId}/applicants/filter")
    public String filterApplicants(
            @PathVariable Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer experience,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String certification,
            @RequestParam(required = false) String skills,
            Model model) {

        // Normalize inputs: convert empty strings to null so they don't break logic
        String cleanStatus = (status != null && status.isBlank()) ? null : status;
        String cleanName = (name != null && name.isBlank()) ? null : name;
        String cleanEdu = (education != null && education.isBlank()) ? null : education;
        String cleanCert = (certification != null && certification.isBlank()) ? null : certification;
        String cleanSkills = (skills != null && skills.isBlank()) ? null : skills;

        // Call the service with cleaned parameters
        List<ApplicantRowDTO> filteredApplicants = applicantService.getFilteredApplicants(
                jobId, cleanStatus, cleanName, experience, cleanEdu, cleanCert, cleanSkills);

        // Populate the model
        model.addAttribute("applicants", filteredApplicants);
        model.addAttribute("jobTitle", applicantService.getJobTitle(jobId));
        model.addAttribute("jobId", jobId);
        model.addAttribute("isReviewMode", false);
        model.addAttribute("activeMenu", "applicants");

        return "employer/applicant-list"; 
    }
}