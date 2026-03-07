package com.revhire.employer.controller;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.employer.dto.ApplicantProfileDTO;
import com.revhire.employer.dto.ApplicantRowDTO;
import com.revhire.employer.dto.ApplicationNoteDTO;
import com.revhire.employer.service.ApplicantService;
import com.revhire.job.entity.Job;
import com.revhire.profile.dto.ProfileResponse;
import com.revhire.profile.service.ProfileService;
import com.revhire.employer.dto.BulkActionDTO;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    
//    @PostMapping("/jobs/{jobId}/applicants/bulk-shortlist")
//    @ResponseBody
//    public String bulkShortlist(
//            @PathVariable Long jobId,
//            @RequestBody BulkActionDTO dto) {
//
//        applicantService.bulkUpdateStatus(
//                dto.getApplicationIds(),
//                "SHORTLIST",
//                dto.getComment()
//        );
//
//        return "Applicants shortlisted successfully";
//    }
//
// // =====================================================
// // 4️⃣ Bulk Reject
// // =====================================================
// @PostMapping("/jobs/{jobId}/applicants/bulk-reject")
// @ResponseBody
// public String bulkReject(
//         @PathVariable Long jobId,
//         @RequestBody BulkActionDTO dto) {
//
//     applicantService.bulkUpdateStatus(
//             dto.getApplicationIds(),
//             "REJECT",
//             dto.getComment()
//     );
//
//     return "Applicants rejected successfully";
// }
    // =====================================================
    // 4️⃣ View Applicant Profile
    // URL: /employer/applicants/{appId}
    // =====================================================

	 @GetMapping("/applicants/{appId}")
	 public String viewApplicant(@PathVariable Long appId, Model model, Authentication auth) {
	     // 1. Get applicant - This is the primary requirement
	     ApplicantProfileDTO applicant = applicantService.getApplicantProfile(appId);
	     
	     // 2. Get profile - If profile is missing, your service now returns a default DTO, 
	     // so this shouldn't be throwing an exception anymore.
	     ProfileResponse profile = profileService.getProfileByUserId(applicant.getSeekerId());
	     
	     // 3. Get Note - This will return an empty DTO if no note exists
	     ApplicationNoteDTO existingNote = applicantService.getNoteForApplication(appId);
	     
	     model.addAttribute("applicant", applicant);
	     model.addAttribute("profile", profile);
	     model.addAttribute("existingNote", existingNote);
	     
	     return "employer/applicant-profile";
	 }
//	 @PostMapping("/applicants/{appId}/notes/add")
//	 public String addNote(@PathVariable Long appId, @RequestParam String note, Authentication authentication) {
//	     applicantService.addNote(appId, note, authentication.getName());
//	     return "redirect:/employer/applicants/" + appId;
//	 }
//	 
//	// =====================================================
//	// 5️⃣ Update Single Applicant Status (Shortlist/Reject)
//	// =====================================================
//	@PostMapping("/applicants/{appId}/status")
//	public String updateApplicantStatus(
//	        @PathVariable Long appId,
//	        @RequestParam String action,
//	        @RequestParam(required = false) String comment) {
//
//	    // Reusing your existing bulkUpdateStatus logic for a single ID
//	    applicantService.bulkUpdateStatus(
//	            List.of(appId), 
//	            action, 
//	            (comment != null ? comment : "Status updated from profile view")
//	    );
//
//	    // Redirect back to the profile page to refresh the view and button states
//	    return "redirect:/employer/applicants/" + appId;
//	}
	// =====================================================
	// 5️⃣ Shortlist Applicant
	// URL: /employer/applicants/{appId}/shortlist
	// =====================================================
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
 // In ApplicantController.java
//    @GetMapping("/jobs/review")
//    public String getPendingReviews(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "5") int size,
//            Model model,
//            Authentication authentication) {
//
//        Pageable pageable = PageRequest.of(page, size);
//        
//        // Add null-safe check if authentication is missing
//        String email = authentication != null ? authentication.getName() : "";
//        
//        Page<Job> jobsPage = applicantService.getJobsWithPendingApplications(email, pageable);
//
//        model.addAttribute("jobsPage", jobsPage);
//        model.addAttribute("keyword", ""); 
//        model.addAttribute("size", size);
//        model.addAttribute("isReviewMode", true);
//
//        return "employer/employer-jobs-list"; 
//    }
    
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
        
        return "employer/applicant-list"; 
    }
    
   
}