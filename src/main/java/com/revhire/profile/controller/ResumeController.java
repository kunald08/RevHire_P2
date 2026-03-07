package com.revhire.profile.controller;

import com.revhire.profile.entity.JobSeekerProfile;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.service.ProfileService;
import com.revhire.profile.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for resume operations — textual resume builder and file upload/download.
 */
@Controller
@RequiredArgsConstructor
public class ResumeController {

    private static final Logger logger = LogManager.getLogger(ResumeController.class);

    private final ResumeService resumeService;
    private final ProfileService profileService;

    /**
     * Show resume page (textual resume builder) with full profile data.
     */
    @GetMapping("/resume")
    public String showResumePage(Authentication authentication, Model model) {
        String email = authentication.getName();
        logger.info("Showing resume page for: {}", email);

        // Fetch the profile with all relations for the resume preview
        JobSeekerProfile profile = profileService.getOrCreateProfile(email);
        model.addAttribute("profile", profile);

        List<Resume> resumes = resumeService.getResumesByEmail(email);
        model.addAttribute("resumes", resumes);

        // Find existing textual resume for pre-filling the form
        Resume textualResume = resumes.stream()
                .filter(r -> r.getFileData() == null)
                .findFirst()
                .orElse(new Resume());
        model.addAttribute("resume", textualResume);

        return "profile/resume-builder";
    }

    /**
     * Save textual resume (objective & projects).
     */
    @PostMapping("/resume")
    public String saveTextualResume(Authentication authentication,
                                    @RequestParam("objective") String objective,
                                    @RequestParam("projects") String projects,
                                    RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        logger.info("Saving textual resume for: {}", email);

        resumeService.saveTextualResume(email, objective, projects);
        redirectAttributes.addFlashAttribute("success", "Resume saved successfully!");
        return "redirect:/resume";
    }

    /**
     * Show resume upload form.
     */
    @GetMapping("/resume/upload")
    public String showUploadForm(Authentication authentication, Model model) {
        String email = authentication.getName();
        logger.info("Showing upload form for: {}", email);

        List<Resume> resumes = resumeService.getResumesByEmail(email);
        // Filter to only file-based resumes
        List<Resume> fileResumes = resumes.stream()
                .filter(r -> r.getFileData() != null)
                .toList();
        model.addAttribute("uploadedResumes", fileResumes);

        return "profile/resume-upload";
    }

    /**
     * Upload resume file (PDF/DOCX, max 2MB).
     */
    @PostMapping("/resume/upload")
    public String uploadResumeFile(Authentication authentication,
                                   @RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        logger.info("Uploading resume file for: {}", email);

        try {
            resumeService.uploadResumeFile(email, file);
            redirectAttributes.addFlashAttribute("success", "Resume uploaded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/resume/upload";
    }

    /**
     * Download a resume file by ID.
     */
    @GetMapping("/resume/download/{id}")
    public ResponseEntity<byte[]> downloadResume(@PathVariable Long id) {
        logger.info("Downloading resume file ID: {}", id);
        
        Resume resume = resumeService.downloadResume(id);

        String contentType = "PDF".equals(resume.getFileType())
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resume.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(resume.getFileSize())
                .body(resume.getFileData());
    }

    /**
     * Delete a resume file by ID.
     */
    @PostMapping("/resume/delete/{id}")
    public String deleteResume(Authentication authentication,
                               @PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        logger.info("Deleting resume file ID: {} for user: {}", id, email);

        try {
            resumeService.deleteResume(email, id);
            redirectAttributes.addFlashAttribute("success", "Resume deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/resume/upload";
    }
}
