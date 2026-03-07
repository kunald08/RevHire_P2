package com.revhire.job.controller;

import com.revhire.exception.BadRequestException; 
import com.revhire.job.dto.JobRequest;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.JobStatsResponse;
import com.revhire.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;

@Controller
@RequiredArgsConstructor
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

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
            redirectAttributes.addFlashAttribute("success", "Job created successfully.");
            return "redirect:/jobs/my";
        } catch (BadRequestException ex) {
            bindingResult.reject("globalError", ex.getMessage());
            return "job/job-create";
        }
    }

    @GetMapping("/my")
    public String myJobs(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        Page<JobResponse> jobsPage =
                jobService.getEmployerJobs(authentication.getName(), page);

        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("jobs", jobsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", jobsPage.getTotalPages());

        return "job/my-jobs";
    }
<<<<<<< HEAD


=======
    
>>>>>>> develop
    @GetMapping("/{id:\\d+}")
    public String viewJob(@PathVariable Long id,
                          Authentication authentication,
                          Model model) {

        String email = authentication.getName();

        model.addAttribute("job", jobService.getJobById(id, email));
        return "job/job-detail";
    }

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

    @PostMapping("/{id:\\d+}/delete")
    public String deleteJob(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

        jobService.deleteJob(id, authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Job deleted successfully.");
        return "redirect:/jobs/my";
    }

    @PostMapping("/{id:\\d+}/close")
    public String closeJob(@PathVariable Long id,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {

        jobService.closeJob(id, authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Job closed successfully.");
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id:\\d+}/reopen")
    public String reopenJob(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

        jobService.reopenJob(id, authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Job reopened successfully.");
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id:\\d+}/fill")
    public String markAsFilled(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        jobService.markAsFilled(id, authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Job marked as filled.");
        return "redirect:/jobs/" + id;
    }

    @GetMapping("/{id:\\d+}/stats")
    public String getJobStats(@PathVariable Long id,
                              Authentication authentication,
                              Model model) {

        String email = authentication.getName();
        JobStatsResponse stats = jobService.getJobStatistics(id, email);

        model.addAttribute("stats", stats);
        return "job/job-stats";
    }
}