package com.revhire.employer.controller;

import com.revhire.application.repository.ApplicationRepository;
import com.revhire.employer.dto.DashboardStats;
import com.revhire.employer.service.ApplicantService;
import com.revhire.employer.service.DashboardService;
import com.revhire.employer.service.EmployerService;
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
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/employer")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final ApplicantService applicantService;
    private final EmployerService employerService;
    private final ApplicationRepository applicationRepository;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String email = authentication.getName();

        // 1. Fetch Summary Statistics (also returns companyName — single employer lookup)
        DashboardStats stats = dashboardService.getEmployerDashboardStats(email);
        model.addAttribute("stats", stats);

        // 2. Company name from stats — no extra DB call
        model.addAttribute("companyName", stats.getCompanyName());

        // 3. Active menu for sidebar highlight
        model.addAttribute("activeMenu", "dashboard");

        // 4. Fetch Recent Job Postings (Top 5)
        Page<Job> recentJobsPage = applicantService.getEmployerJobsWithApplications(
                email, 
                "", 
                PageRequest.of(0, 5)
        );
        List<Job> jobs = recentJobsPage.getContent();

        // 5. Batch-fetch application counts in ONE query (avoids N+1)
        if (!jobs.isEmpty()) {
            List<Long> jobIds = jobs.stream().map(Job::getId).toList();
            Map<Long, Long> counts = applicationRepository.countApplicationsByJobIds(jobIds)
                    .stream()
                    .collect(Collectors.toMap(
                            row -> (Long) row[0],
                            row -> (Long) row[1]
                    ));
            jobs.forEach(job -> job.setApplicationCount(counts.getOrDefault(job.getId(), 0L)));
        }

        model.addAttribute("jobs", jobs);

        return "employer/dashboard";
    }
}