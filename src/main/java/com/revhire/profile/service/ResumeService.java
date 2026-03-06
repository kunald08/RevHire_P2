package com.revhire.profile.service;

import com.revhire.profile.entity.Resume;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for resume operations — textual resume and file upload/download.
 */
public interface ResumeService {

    /**
     * Get all resumes for the current user.
     */
    List<Resume> getResumesByEmail(String email);

    /**
     * Get a specific resume by ID.
     */
    Resume getResumeById(Long resumeId);

    /**
     * Save textual resume (objective + projects).
     */
    void saveTextualResume(String email, String objective, String projects);

    /**
     * Upload a resume file (PDF or DOCX, max 2MB).
     */
    void uploadResumeFile(String email, MultipartFile file);

    /**
     * Download a resume file by its ID.
     */
    Resume downloadResume(Long resumeId);
}
