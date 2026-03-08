package com.revhire.employer.controller;

import com.revhire.employer.dto.EmployerRequest;
import com.revhire.employer.dto.EmployerResponse;
import com.revhire.employer.service.EmployerService;
import com.revhire.job.service.JobService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for employer / company profile pages (Module 3 — Chaitanya).
 * Maps to /employers/* endpoints. Returns Thymeleaf views.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/employers")
public class EmployerController {

    private static final Logger logger = LogManager.getLogger(EmployerController.class);

    private final EmployerService employerService;
    private final JobService jobService;

    /**
     * Populates companyName for the employer sidebar on all views served by this controller.
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

    // ────────────────────────────────────────────
    // VIEW COMPANY PROFILE
    // ────────────────────────────────────────────

    @GetMapping("/profile")
    public String viewProfile(Authentication authentication, Model model) {

        String email = authentication.getName();
        logger.info("Employer viewing own company profile: {}", email);

        try {
            model.addAttribute("employer", employerService.getEmployerByEmail(email));
            model.addAttribute("activeMenu", "profile");
            return "employer/company-profile";
        } catch (Exception e) {
            logger.info("No employer profile exists yet — redirecting to create form");
            return "redirect:/employers/profile/create";
        }
    }

    // ────────────────────────────────────────────
    // EDIT PROFILE FORM
    // ────────────────────────────────────────────

    @GetMapping("/profile/edit")
    public String editProfile(Authentication authentication, Model model) {

        String email = authentication.getName();

        try {
            model.addAttribute("employerRequest", employerService.getEmployerByEmail(email));
            model.addAttribute("isEdit", true);
        } catch (Exception e) {
            model.addAttribute("employerRequest", new EmployerRequest());
            model.addAttribute("isEdit", false);
        }

        model.addAttribute("activeMenu", "editprofile");
        return "employer/company-edit";
    }

    // ────────────────────────────────────────────
    // CREATE PROFILE FORM
    // ────────────────────────────────────────────

    @GetMapping("/profile/create")
    public String createProfileForm(Model model) {

        model.addAttribute("employerRequest", new EmployerRequest());
        model.addAttribute("isEdit", false);
        model.addAttribute("activeMenu", "editprofile");
        return "employer/company-edit";
    }

    // ────────────────────────────────────────────
    // SAVE / UPDATE PROFILE
    // ────────────────────────────────────────────

    @PostMapping("/profile")
    public String saveProfile(@Valid @ModelAttribute("employerRequest") EmployerRequest request,
                              BindingResult bindingResult,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes,
                              Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", employerService.hasProfile(authentication.getName()));
            model.addAttribute("activeMenu", "editprofile");
            return "employer/company-edit";
        }

        String email = authentication.getName();
        employerService.createOrUpdateEmployer(request, email);

        redirectAttributes.addFlashAttribute("success", "Company profile saved successfully!");
        return "redirect:/employers/profile";
    }

    // ────────────────────────────────────────────
    // PUBLIC COMPANY PAGE
    // ────────────────────────────────────────────

    @GetMapping("/{id}")
    public String publicProfile(@PathVariable Long id, Model model) {

        logger.info("Public view of employer profile — ID: {}", id);
        model.addAttribute("employer", employerService.getEmployerById(id));
        model.addAttribute("isPublic", true);
        model.addAttribute("activeMenu", "profile");
        return "employer/company-profile-public";
    }
    
}