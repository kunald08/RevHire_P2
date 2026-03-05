package com.revhire.employer.controller;
import com.revhire.application.entity.Application;
import com.revhire.employer.dto.ApplicantRowDTO;
import com.revhire.employer.service.ApplicantService;
import com.revhire.job.entity.Job;
import com.revhire.employer.dto.BulkActionDTO;
import lombok.RequiredArgsConstructor;

import java.util.List;

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

        Pageable pageable = PageRequest.of(page, size);

        Page<Job> jobsPage =
                applicantService.getEmployerJobsWithApplications(
                        authentication.getName(),
                        keyword,
                        pageable);

        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", size);

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

        return "employer/applicant-list";
    }

    // =====================================================
    // 3️⃣ Bulk Shortlist / Reject
    // =====================================================

//    @PostMapping("/jobs/{jobId}/applicants/bulk")
//    public String bulkAction(
//            @PathVariable Long jobId,
//            @RequestParam List<Long> applicationIds,
//            @RequestParam String action) {
//
//        applicantService.bulkUpdateStatus(applicationIds, action);
//
//        return "redirect:/employer/jobs/" + jobId + "/applicants";
//    }
 // =====================================================
 // 3️⃣ Bulk Shortlist
 // =====================================================
    @PostMapping("/jobs/{jobId}/applicants/bulk-shortlist")
    @ResponseBody
    public String bulkShortlist(
            @PathVariable Long jobId,
            @RequestBody BulkActionDTO dto) {

        applicantService.bulkUpdateStatus(
                dto.getApplicationIds(),
                "SHORTLIST",
                dto.getComment()
        );

        return "Applicants shortlisted successfully";
    }

 // =====================================================
 // 4️⃣ Bulk Reject
 // =====================================================
 @PostMapping("/jobs/{jobId}/applicants/bulk-reject")
 @ResponseBody
 public String bulkReject(
         @PathVariable Long jobId,
         @RequestBody BulkActionDTO dto) {

     applicantService.bulkUpdateStatus(
             dto.getApplicationIds(),
             "REJECT",
             dto.getComment()
     );

     return "Applicants rejected successfully";
 }
    // =====================================================
    // 4️⃣ View Applicant Profile
    // URL: /employer/applicants/{appId}
    // =====================================================

    @GetMapping("/applicants/{appId}")
    public String viewApplicantProfile(
            @PathVariable Long appId,
            Model model) {

        Application application =
                applicantService.getApplicationEntity(appId);

        model.addAttribute("application", application);

        return "employer/applicant-profile";
    }
}