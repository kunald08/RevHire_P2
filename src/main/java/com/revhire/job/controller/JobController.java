package com.revhire.job.controller;

import com.revhire.employer.repository.EmployerRepository;
import com.revhire.exception.BadRequestException;
import com.revhire.job.dto.JobRequest;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.JobStatsResponse;
import com.revhire.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for job posting operations (Module 3 — Chaitanya).
 * Maps to /jobs/* endpoints. Returns Thymeleaf views.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/jobs")
public class JobController {

    private static final Logger logger = LogManager.getLogger(JobController.class);

    private final JobService jobService;
    private final EmployerRepository employerRepository;

    // ────────────────────────────────────────────
    // CREATE JOB
    // ────────────────────────────────────────────

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("jobRequest", new JobRequest());
        return "job/job-create";
    }

    @PostMapping
    public String createJob(@Valid @ModelAttribute("jobRequest") JobRequest request,
                            BindingResult bindingResult,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "job/job-create";
        }

        try {
            jobService.createJob(request, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Job posted successfully! It's now live.");
            return "redirect:/jobs/my";
        } catch (BadRequestException ex) {
            bindingResult.reject("globalError", ex.getMessage());
            return "job/job-create";
        }
    }

    // ────────────────────────────────────────────
    // MY JOBS (paginated + filterable)
    // ────────────────────────────────────────────

    @GetMapping("/my")
    public String myJobs(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) String status,
                         Authentication authentication,
                         Model model) {

        Page<JobResponse> jobsPage = jobService.getEmployerJobs(
                authentication.getName(), page, keyword, status);

        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("jobs", jobsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", jobsPage.getTotalPages());
        model.addAttribute("totalJobs", jobsPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusFilter", status);

        return "job/my-jobs";
    }

    // ────────────────────────────────────────────
    // VIEW JOB DETAIL
    // ────────────────────────────────────────────

    @GetMapping("/{id:\\d+}")
    public String viewJob(@PathVariable Long id,
                          Authentication authentication,
                          Model model) {

        // Handle both authenticated and anonymous (public) access
        JobResponse job;
        boolean isOwner = false;

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String email = authentication.getName();
            job = jobService.getJobById(id, email);
            isOwner = employerRepository.findByUserEmail(email)
                    .map(emp -> emp.getId().equals(job.getEmployerId()))
                    .orElse(false);
        } else {
            job = jobService.getJobById(id);
        }

        model.addAttribute("job", job);
        model.addAttribute("isOwner", isOwner);

        return "job/job-detail";
    }

    // ────────────────────────────────────────────
    // EDIT JOB
    // ────────────────────────────────────────────

    @GetMapping("/{id:\\d+}/edit")
    public String editJob(@PathVariable Long id, Model model) {

        JobResponse job = jobService.getJobById(id);

        JobRequest request = new JobRequest();
        request.setTitle(job.getTitle());
        request.setDescription(job.getDescription());
        request.setLocation(job.getLocation());
        request.setSalaryMin(job.getSalaryMin());
        request.setSalaryMax(job.getSalaryMax());
        request.setJobType(job.getJobType());
        request.setDeadline(job.getDeadline());
        request.setNumOpenings(job.getNumOpenings());
        request.setExperienceMin(job.getExperienceMin());
        request.setExperienceMax(job.getExperienceMax());
        request.setRequiredSkills(job.getRequiredSkills());
        request.setEducationReq(job.getEducationReq());

        model.addAttribute("jobRequest", request);
        model.addAttribute("jobId", id);

        return "job/job-edit";
    }

    @PostMapping("/{id:\\d+}/update")
    public String updateJob(@PathVariable Long id,
                            @Valid @ModelAttribute("jobRequest") JobRequest request,
                            BindingResult bindingResult,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("jobId", id);
            return "job/job-edit";
        }

        try {
            jobService.updateJob(id, request, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Job updated successfully.");
            return "redirect:/jobs/" + id;
        } catch (BadRequestException ex) {
            bindingResult.reject("globalError", ex.getMessage());
            model.addAttribute("jobId", id);
            return "job/job-edit";
        }
    }

    // ────────────────────────────────────────────
    // DELETE
    // ────────────────────────────────────────────

    @PostMapping("/{id:\\d+}/delete")
    public String deleteJob(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

    	try {
    	    jobService.deleteJob(id, authentication.getName());
    	    redirectAttributes.addFlashAttribute("success", "Job deleted.");
    	} catch (BadRequestException e) {
    	    redirectAttributes.addFlashAttribute("error", e.getMessage());
    	}
        return "redirect:/jobs/my";
    }

    // ────────────────────────────────────────────
    // LIFECYCLE ACTIONS
    // ────────────────────────────────────────────

    @PostMapping("/{id:\\d+}/close")
    public String closeJob(@PathVariable Long id,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {

        jobService.closeJob(id, authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Job closed — no new applications will be accepted.");
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id:\\d+}/reopen")
    public String reopenJob(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

        jobService.reopenJob(id, authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Job is now active and accepting applications.");
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id:\\d+}/fill")
    public String markAsFilled(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        jobService.markAsFilled(id, authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Job marked as filled. Congratulations!");
        return "redirect:/jobs/" + id;
    }

    // ────────────────────────────────────────────
    // STATISTICS
    // ────────────────────────────────────────────

    @GetMapping("/{id:\\d+}/stats")
    public String getJobStats(@PathVariable Long id,
                              Authentication authentication,
                              Model model) {

        JobStatsResponse stats = jobService.getJobStatistics(id, authentication.getName());
        model.addAttribute("stats", stats);
        model.addAttribute("jobId", id);
        model.addAttribute("jobTitle", stats.getJobTitle());
        return "job/job-stats";
    }

    // ────────────────────────────────────────────
    // ACTIVE JOBS
    // ────────────────────────────────────────────

    @GetMapping("/active")
    public String activeJobs(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String keyword,
                             Authentication authentication,
                             Model model) {

        Page<JobResponse> jobsPage = jobService.getEmployerJobs(
                authentication.getName(), page, keyword, "ACTIVE");

        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("jobs", jobsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", jobsPage.getTotalPages());
        model.addAttribute("totalJobs", jobsPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusFilter", "ACTIVE");

        return "job/my-jobs";
    }

    // ────────────────────────────────────────────
    // SAVE AS DRAFT
    // ────────────────────────────────────────────

    @PostMapping("/draft")
    public String saveDraft(@ModelAttribute("jobRequest") JobRequest request,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

        try {
            jobService.createDraftJob(request, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Job saved as draft. You can publish it later.");
            return "redirect:/jobs/my";
        } catch (BadRequestException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/jobs/create";
        }
    }

    // ────────────────────────────────────────────
    // PUBLISH DRAFT
    // ────────────────────────────────────────────

    @PostMapping("/{id:\\d+}/publish")
    public String publishDraft(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        try {
            jobService.publishDraft(id, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Job is now live and accepting applications!");
            return "redirect:/jobs/" + id;
        } catch (BadRequestException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/jobs/" + id + "/edit";
        }
    }

    // ────────────────────────────────────────────
    // DUPLICATE JOB
    // ────────────────────────────────────────────

    @PostMapping("/{id:\\d+}/duplicate")
    public String duplicateJob(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        JobResponse copy = jobService.duplicateJob(id, authentication.getName());
        redirectAttributes.addFlashAttribute("success",
                "Job duplicated as draft — \"" + copy.getTitle() + "\". Edit and publish when ready.");
        return "redirect:/jobs/" + copy.getId() + "/edit";
    }
}