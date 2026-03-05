package com.revhire.employer.controller;

import com.revhire.employer.dto.DashboardStats;
import com.revhire.employer.service.ApplicantService;
import com.revhire.employer.service.DashboardService;
import com.revhire.job.entity.Job;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/employer")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final ApplicantService applicantService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String email = authentication.getName();

        // 1. Fetch Summary Statistics
        DashboardStats stats = dashboardService.getEmployerDashboardStats(email);
        model.addAttribute("stats", stats);

        // 2. Fetch Recent Job Postings (Top 5)
        // We reuse the ApplicantService to get the first page of jobs
        Page<Job> recentJobsPage = applicantService.getEmployerJobsWithApplications(
                email, 
                "", 
                PageRequest.of(0, 5)
        );
        
        // Pass the list of jobs to the 'jobs' attribute expected by dashboard.html
        model.addAttribute("jobs", recentJobsPage.getContent());

        return "employer/dashboard";
    }
}