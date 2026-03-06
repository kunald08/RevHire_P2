package com.revhire.application.controller;

import com.revhire.job.dto.JobSearchFilter;
import com.revhire.application.service.ApplicationService;
import com.revhire.application.service.FavoriteService;
import com.revhire.application.service.JobSearchService;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;  // ADD THIS IMPORT
import com.revhire.common.enums.JobType;
import com.revhire.job.dto.JobResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Log4j2
public class JobSearchController {

    private final JobSearchService jobSearchService;
    private final ApplicationService applicationService;
    private final FavoriteService favoriteService;
    private final UserRepository userRepository;  // ADD THIS FIELD

    /**
     * Handle login redirects to the correct login page
     */
    @GetMapping("/login")
    public String redirectToAuthLogin() {
        log.info("Redirecting to auth login page");
        return "redirect:/auth/login";
    }

    @GetMapping("/search")
    public String showSearchPage(Model model) {
        log.info("Showing job search page");
        model.addAttribute("filter", new JobSearchFilter());
        model.addAttribute("jobTypes", JobType.values());
        return "job/job-search";
    }

    @GetMapping("/search/results")
    public String searchJobs(
            @Valid @ModelAttribute("filter") JobSearchFilter filter,
            BindingResult result,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser,  // This might be null
            Model model) {
        
        log.info("Processing job search with filter: {}", filter);
        
        if (result.hasErrors()) {
            model.addAttribute("jobTypes", JobType.values());
            return "job/job-search";
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<JobResponse> jobs = jobSearchService.searchJobs(filter, pageable);
        
        model.addAttribute("jobs", jobs);
        model.addAttribute("filter", filter);
        model.addAttribute("jobTypes", JobType.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", jobs.getTotalPages());
        
        // IMPROVED: Check authentication from SecurityContextHolder
        boolean isSeeker = false;
        Map<Long, Boolean> favoriteStatuses = new HashMap<>();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            // User is authenticated
            log.info("User is authenticated: {}", auth.getName());
            
            // Check if user has SEEKER role
            isSeeker = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SEEKER") || a.getAuthority().equals("ROLE_SEEKER"));
            
            if (isSeeker) {
                // Get user by email to get the ID
                String email = auth.getName();
                
                // Try to find user by email
                User user = userRepository.findByEmail(email).orElse(null);
                
                if (user != null) {
                    Long userId = user.getId();
                    for (JobResponse job : jobs.getContent()) {
                        boolean isFavorited = favoriteService.isJobFavorited(userId, job.getId());
                        favoriteStatuses.put(job.getId(), isFavorited);
                    }
                    log.info("Added favorite statuses for {} jobs", favoriteStatuses.size());
                } else {
                    log.warn("User not found in database for email: {}", email);
                }
            }
        } else {
            log.info("No authenticated user");
        }
        
        model.addAttribute("isSeeker", isSeeker);
        model.addAttribute("favoriteStatuses", favoriteStatuses);
        
        return "job/job-search-results";
    }
}