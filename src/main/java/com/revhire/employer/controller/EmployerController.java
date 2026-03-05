package com.revhire.employer.controller;

import com.revhire.employer.dto.EmployerRequest;  
import com.revhire.employer.service.EmployerService;
import com.revhire.job.service.JobService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;

@Controller
@RequiredArgsConstructor
@RequestMapping("/employers")
public class EmployerController {

    private final EmployerService employerService;
    private final JobService jobService;
    
    @GetMapping("/profile")
    public String viewProfile(Authentication authentication, Model model) {

        String email = authentication.getName();

        try {
            model.addAttribute("employer", employerService.getEmployerByEmail(email));
            return "employer/company-profile";
        } catch (Exception e) {
            return "redirect:/employers/profile/edit";
        }
    }

    @GetMapping("/profile/edit")
    public String editProfile(Authentication authentication, Model model) {

        String email = authentication.getName();
        model.addAttribute("employerRequest",
                employerService.getEmployerByEmail(email));

        return "employer/company-edit";
    }
    
    @GetMapping("/profile/create")
    public String createProfileForm(Model model) {

        model.addAttribute("employerRequest", new EmployerRequest());
        return "employer/company-edit";
    }

    @PostMapping("/profile")
    public String saveProfile(@Valid @ModelAttribute("employerRequest") EmployerRequest request,
                              BindingResult bindingResult,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "employer/company-edit";
        }

        String email = authentication.getName();
        employerService.createOrUpdateEmployer(request, email);

        redirectAttributes.addFlashAttribute("success", "Company profile updated successfully.");
        return "redirect:/employers/profile";
    }

    @GetMapping("/{id}")
    public String publicProfile(@PathVariable Long id, Model model) {

        model.addAttribute("employer", employerService.getEmployerById(id));
        return "employer/company-profile";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {

        String email = authentication.getName();

        model.addAttribute("jobs",
                jobService.getEmployerJobs(email));

        return "employer/dashboard";
    }
    
}