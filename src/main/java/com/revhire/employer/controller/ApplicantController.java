package com.revhire.employer.controller;

import com.revhire.employer.service.ApplicantService;
import com.revhire.job.entity.Job;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/employer")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantService applicantService;

    @GetMapping("/jobs")
    public String getEmployerJobs(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size, // Configurable page size
            Model model,
            Authentication authentication) {

        // Boundary checks for safety
        int pageIndex = Math.max(page, 0);
        int pageSize = Math.max(size, 1); 

        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Job> jobsPage = applicantService.getEmployerJobsWithApplications(
                        authentication.getName(),
                        keyword,
                        pageable);

        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", pageSize); // Pass current size back to UI

        return "employer/employer-jobs-list";
    }
}