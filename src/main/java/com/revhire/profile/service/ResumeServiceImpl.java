package com.revhire.profile.service;

import com.revhire.exception.BadRequestException;
import com.revhire.exception.FileStorageException;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.profile.entity.JobSeekerProfile;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Implementation of ResumeService — handles textual resume and file upload/download.
 */
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final Logger logger = LogManager.getLogger(ResumeServiceImpl.class);

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ResumeRepository resumeRepository;
    private final ProfileService profileService;

    @Override
    public List<Resume> getResumesByEmail(String email) {
        logger.info("Fetching resumes for user: {}", email);
        JobSeekerProfile profile = profileService.getOrCreateProfile(email);
        return resumeRepository.findByProfileId(profile.getId());
    }

    @Override
    public Resume getResumeById(Long resumeId) {
        logger.info("Fetching resume by ID: {}", resumeId);
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
    }

    @Override
    @Transactional
    public void saveTextualResume(String email, String objective, String projects) {
        logger.info("Saving textual resume for user: {}", email);
        JobSeekerProfile profile = profileService.getOrCreateProfile(email);

        // Find existing textual resume (one without file data) or create new
        Resume resume = resumeRepository.findTopByProfileIdOrderByCreatedAtDesc(profile.getId())
                .filter(r -> r.getFileData() == null)
                .orElse(Resume.builder()
                        .profile(profile)
                        .build());

        resume.setObjective(objective);
        resume.setProjects(projects);
        resumeRepository.save(resume);
        logger.info("Textual resume saved successfully for user: {}", email);
    }

    @Override
    @Transactional
    public void uploadResumeFile(String email, MultipartFile file) {
        logger.info("Uploading resume file for user: {}", email);

        // Validate file
        if (file.isEmpty()) {
            throw new BadRequestException("Please select a file to upload");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds the maximum limit of 2MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("Only PDF and DOCX files are allowed");
        }

        try {
            JobSeekerProfile profile = profileService.getOrCreateProfile(email);

            String fileType = contentType.contains("pdf") ? "PDF" : "DOCX";

            Resume resume = Resume.builder()
                    .profile(profile)
                    .fileName(file.getOriginalFilename())
                    .fileType(fileType)
                    .fileData(file.getBytes())
                    .fileSize(file.getSize())
                    .build();

            resumeRepository.save(resume);
            logger.info("Resume file uploaded successfully: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        } catch (IOException e) {
            logger.error("Failed to upload resume file: {}", e.getMessage());
            throw new FileStorageException("Failed to store resume file", e);
        }
    }

    @Override
    public Resume downloadResume(Long resumeId) {
        logger.info("Downloading resume file ID: {}", resumeId);
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));

        if (resume.getFileData() == null) {
            throw new BadRequestException("This resume does not have an uploaded file");
        }

        return resume;
    }
    
    
}
