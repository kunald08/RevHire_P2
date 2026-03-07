package com.revhire.profile.controller;

import com.revhire.profile.dto.*;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.service.ProfileService;
import com.revhire.profile.service.ResumeService;
import jakarta.validation.Valid;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Job Seeker profile management.
 * Handles profile CRUD, education, experience, skills, and certifications.
 */
@Controller
@RequiredArgsConstructor
public class ProfileController {

    private static final Logger logger = LogManager.getLogger(ProfileController.class);

    private final ProfileService profileService;
    private final ResumeService resumeService;

    // ==================== Profile ====================

    /**
     * View own profile page.
     */
    @GetMapping("/profile")
    public String viewProfile(Authentication authentication, Model model) {
        String email = authentication.getName();
        logger.info("Viewing profile for: {}", email);

        ProfileResponse profile = profileService.getProfileByEmail(email);
        model.addAttribute("profile", profile);

        // Fetch uploaded resumes (file-based only) for Quick Actions section
        List<Resume> uploadedResumes = resumeService.getResumesByEmail(email).stream()
                .filter(r -> r.getFileData() != null)
                .toList();
        model.addAttribute("uploadedResumes", uploadedResumes);

        return "profile/profile";
    }

    /**
     * Show edit profile form.
     */
    @GetMapping("/profile/edit")
    public String editProfile(Authentication authentication, Model model) {
        String email = authentication.getName();
        logger.info("Editing profile for: {}", email);

        ProfileResponse profile = profileService.getProfileByEmail(email);
        model.addAttribute("profile", profile);

        // Pre-fill the form with existing data
        ProfileRequest profileRequest = ProfileRequest.builder()
                .headline(profile.getHeadline())
                .summary(profile.getSummary())
                .currentEmploymentStatus(profile.getCurrentEmploymentStatus())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .build();
        model.addAttribute("profileRequest", profileRequest);

        // Empty DTOs for add-forms
        model.addAttribute("educationDto", new EducationDto());
        model.addAttribute("experienceDto", new ExperienceDto());
        model.addAttribute("skillDto", new SkillDto());
        model.addAttribute("certificationDto", new CertificationDto());

        return "profile/profile-edit";
    }

    /**
     * Save/update profile basic info.
     */
    @PostMapping("/profile")
    public String saveProfile(Authentication authentication,
                              @ModelAttribute("profileRequest") ProfileRequest request,
                              RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        logger.info("Saving profile for: {}", email);

        profileService.saveProfile(email, request);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/profile";
    }

    /**
     * View a seeker's public profile (for employers or other authenticated users).
     */
    @GetMapping("/profile/{userId}")
    public String viewPublicProfile(@PathVariable Long userId, Model model) {
        logger.info("Viewing public profile for user ID: {}", userId);

        ProfileResponse profile = profileService.getProfileByUserId(userId);
        model.addAttribute("profile", profile);
        return "profile/view-profile";
    }

    // ==================== Education ====================

    @PostMapping("/profile/education")
    public String addEducation(Authentication authentication,
                               @Valid @ModelAttribute("educationDto") EducationDto dto,
                               RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        profileService.addEducation(email, dto);
        redirectAttributes.addFlashAttribute("success", "Education added successfully!");
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/education/{id}/update")
    public String updateEducation(Authentication authentication,
                                  @PathVariable Long id,
                                  @Valid @ModelAttribute("educationDto") EducationDto dto,
                                  RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        profileService.updateEducation(email, id, dto);
        redirectAttributes.addFlashAttribute("success", "Education updated successfully!");
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/education/{id}/delete")
    public String deleteEducation(Authentication authentication,
                                  @PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        profileService.deleteEducation(email, id);
        redirectAttributes.addFlashAttribute("success", "Education deleted successfully!");
        return "redirect:/profile/edit";
    }

    // ==================== Experience ====================

    @PostMapping("/profile/experience")
    public String addExperience(Authentication authentication,
                                @Valid @ModelAttribute("experienceDto") ExperienceDto dto,
                                RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        profileService.addExperience(email, dto);
        redirectAttributes.addFlashAttribute("success", "Experience added successfully!");
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/experience/{id}/update")
    public String updateExperience(Authentication authentication,
                                   @PathVariable Long id,
                                   @Valid @ModelAttribute("experienceDto") ExperienceDto dto,
                                   RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        profileService.updateExperience(email, id, dto);
        redirectAttributes.addFlashAttribute("success", "Experience updated successfully!");
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/experience/{id}/delete")
    public String deleteExperience(Authentication authentication,
                                   @PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        profileService.deleteExperience(email, id);
        redirectAttributes.addFlashAttribute("success", "Experience deleted successfully!");
        return "redirect:/profile/edit";
    }

    // ==================== Skills ====================

    @PostMapping("/profile/skills")
    public String addSkill(Authentication authentication,
                           @Valid @ModelAttribute("skillDto") SkillDto dto,
                           RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        profileService.addSkill(email, dto);
        redirectAttributes.addFlashAttribute("success", "Skill added successfully!");
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/skills/{id}/delete")
    public String deleteSkill(Authentication authentication,
                              @PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        profileService.deleteSkill(email, id);
        redirectAttributes.addFlashAttribute("success", "Skill deleted successfully!");
        return "redirect:/profile/edit";
    }

    // ==================== Certifications ====================

    @PostMapping("/profile/certifications")
    public String addCertification(Authentication authentication,
                                   @Valid @ModelAttribute("certificationDto") CertificationDto dto,
                                   RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        profileService.addCertification(email, dto);
        redirectAttributes.addFlashAttribute("success", "Certification added successfully!");
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/certifications/{id}/delete")
    public String deleteCertification(Authentication authentication,
                                      @PathVariable Long id,
                                      RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        profileService.deleteCertification(email, id);
        redirectAttributes.addFlashAttribute("success", "Certification deleted successfully!");
        return "redirect:/profile/edit";
    }

    // ==================== Profile Photo ====================

    /**
     * Upload a profile photo.
     */
    @PostMapping("/profile/photo")
    public String uploadPhoto(Authentication authentication,
                              @RequestParam("photo") MultipartFile photo,
                              RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        logger.info("Uploading profile photo for: {}", email);

        try {
            profileService.uploadProfilePhoto(email, photo);
            redirectAttributes.addFlashAttribute("success", "Profile photo updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload photo. Please try again.");
        }
        return "redirect:/profile/edit";
    }

    /**
     * Remove the current profile photo.
     */
    @PostMapping("/profile/photo/remove")
    public String removePhoto(Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        logger.info("Removing profile photo for: {}", email);

        profileService.removeProfilePhoto(email);
        redirectAttributes.addFlashAttribute("success", "Profile photo removed.");
        return "redirect:/profile/edit";
    }
}
