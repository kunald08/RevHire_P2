package com.revhire.application.service;

import com.revhire.application.dto.ApplicationRequest;
import com.revhire.application.dto.ApplicationResponse;
import com.revhire.application.dto.WithdrawRequest;
import com.revhire.application.entity.Application;
import com.revhire.application.repository.ApplicationRepository;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.exception.BadRequestException;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.exception.UnauthorizedException;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;

    @PostConstruct
    public void init() {
        log.info("Initializing ApplicationServiceImpl - checking for applications with invalid data");
        
        try {
            // Fix applications with invalid resume references
            List<Application> applications = applicationRepository.findAll();
            int fixedCount = 0;
            
            for (Application app : applications) {
                boolean needsFix = false;
                
                // Check if resume exists (if application has a resume)
                if (app.getResume() != null) {
                    Long resumeId = app.getResume().getId();
                    boolean resumeExists = resumeRepository.existsById(resumeId);
                    if (!resumeExists) {
                        log.warn("Application ID {} has invalid resume_id: {}. Setting to NULL.", 
                            app.getId(), resumeId);
                        app.setResume(null);
                        needsFix = true;
                    }
                }
                
                if (needsFix) {
                    applicationRepository.save(app);
                    fixedCount++;
                }
            }
            
            if (fixedCount > 0) {
                log.info("Fixed {} applications with invalid data", fixedCount);
            }
            
            // Log summary of applications
            log.info("Total applications in database: {}", applications.size());
            
        } catch (Exception e) {
            log.error("Error during initialization: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ApplicationResponse applyForJob(Long seekerId, ApplicationRequest request) {
        try {
            log.info("========== APPLY FOR JOB START ==========");
            log.info("Applying for job - seekerId: {}, jobId: {}", seekerId, request != null ? request.getJobId() : null);
            
            // Log request details safely
            Long resumeId = request != null ? request.getResumeId() : null;
            MultipartFile newResume = request != null ? request.getNewResume() : null;
            boolean hasNewResume = newResume != null && !newResume.isEmpty();
            
            log.info("Request details - resumeId: {}, hasNewResume: {}, newResumeFileName: {}", 
                resumeId,
                hasNewResume,
                hasNewResume ? newResume.getOriginalFilename() : "No file");

            // Validate inputs
            if (seekerId == null) {
                throw new BadRequestException("Please login to apply");
            }
            if (request == null || request.getJobId() == null) {
                throw new BadRequestException("Invalid application request");
            }

            // Validate that either resumeId is provided OR newResume is provided
            if (resumeId == null && !hasNewResume) {
                throw new BadRequestException("Please select an existing resume or upload a new one");
            }

            // Check if already applied
            if (applicationRepository.existsByJobIdAndSeekerId(request.getJobId(), seekerId)) {
                throw new BadRequestException("You have already applied for this job");
            }

            // Get job
            Job job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + request.getJobId()));
            log.info("Found job: {} - Title: {}, Company: {}", 
                job.getId(), 
                job.getTitle(),
                job.getEmployer() != null ? job.getEmployer().getCompanyName() : "No Company");

            // Get user
            User user = userRepository.findById(seekerId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + seekerId));
            log.info("Found user: {} - Email: {}, Name: {}", 
                user.getId(), 
                user.getEmail(),
                user.getName());

            // Create application
            Application application = new Application();
            application.setJob(job);
            application.setSeeker(user);
            application.setCoverLetter(request.getCoverLetter());
            application.setStatus(ApplicationStatus.APPLIED);
            
            // ========== CRITICAL FIX: Handle both existing and new resumes ==========
            Resume resumeToAttach = null;
            
            // Case 1: User selected an existing resume (resumeId is provided)
            if (resumeId != null && resumeId > 0) {
                log.info("Attempting to attach existing resume ID: {}", resumeId);
                
                resumeToAttach = resumeRepository.findById(resumeId)
                    .orElse(null);
                    
                if (resumeToAttach != null) {
                    log.info("✅ Found existing resume - ID: {}, FileName: {}", 
                        resumeToAttach.getId(), resumeToAttach.getFileName());
                } else {
                    log.error("❌ Existing resume ID {} not found in database", resumeId);
                    throw new ResourceNotFoundException("Resume not found with id: " + resumeId);
                }
            }
            
            // Case 2: User uploaded a new resume (newResume is provided)
            else if (hasNewResume) {
                log.info("========== PROCESSING NEW RESUME UPLOAD ==========");
                MultipartFile file = newResume;
                log.info("File details - Original Name: {}, Size: {} bytes, ContentType: {}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
                
                try {
                    // Validate file type (optional)
                    String contentType = file.getContentType();
                    if (contentType == null || (!contentType.contains("pdf") && !contentType.contains("doc") && !contentType.contains("docx"))) {
                        log.warn("File type may not be supported: {}", contentType);
                    }
                    
                    // Create new resume entity
                    Resume newResumeEntity = new Resume();
                    newResumeEntity.setFileName(file.getOriginalFilename());
                    newResumeEntity.setFileType(contentType);
                    newResumeEntity.setFileData(file.getBytes());
                    newResumeEntity.setFileSize(file.getSize());
                    
                    // TODO: Set the profile here once you have ProfileRepository
                    // You'll need to inject ProfileRepository to get the profile for this user
                    // JobSeekerProfile profile = profileRepository.findByUserId(seekerId).orElse(null);
                    // if (profile != null) {
                    //     newResumeEntity.setProfile(profile);
                    // }
                    
                    Resume savedResume = resumeRepository.save(newResumeEntity);
                    log.info("✅ NEW RESUME SAVED TO DATABASE - ID: {}, FileName: {}, Size: {} bytes", 
                        savedResume.getId(), savedResume.getFileName(), savedResume.getFileSize());
                    
                    resumeToAttach = savedResume;
                    
                } catch (IOException e) {
                    log.error("❌ FAILED TO PROCESS UPLOADED RESUME: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to process uploaded resume: " + e.getMessage(), e);
                }
                log.info("========== NEW RESUME UPLOAD COMPLETE ==========");
            }
            
            // Attach the resume to the application if we have one
            if (resumeToAttach != null) {
                application.setResume(resumeToAttach);
                log.info("✅ RESUME ATTACHED TO APPLICATION - ID: {}, FileName: {}", 
                    resumeToAttach.getId(), resumeToAttach.getFileName());
            } else {
                log.warn("⚠️ NO RESUME ATTACHED TO APPLICATION - This should not happen due to validation");
            }
            // ========== END FIX ==========

            Application saved = applicationRepository.save(application);
            log.info("✅ APPLICATION SAVED TO DATABASE - ID: {}, jobId: {}, resumeId: {}, resumeFileName: {}", 
                saved.getId(), 
                saved.getJob() != null ? saved.getJob().getId() : null,
                saved.getResume() != null ? saved.getResume().getId() : "NULL",
                saved.getResume() != null ? saved.getResume().getFileName() : "NULL");
            
            log.info("========== APPLY FOR JOB END ==========");

            return convertToResponse(saved);

        } catch (BadRequestException | ResourceNotFoundException e) {
            log.error("Business error applying for job: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error applying for job: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ApplicationResponse> getMyApplications(Long seekerId, Pageable pageable) {
        try {
            log.info("Getting applications for seekerId: {}", seekerId);

            if (seekerId == null) {
                return new PageImpl<>(new ArrayList<>());
            }

            Page<Application> applications = applicationRepository.findBySeekerId(seekerId, pageable);
            log.info("Found {} applications for seekerId: {}", applications.getTotalElements(), seekerId);
            
            return applications.map(this::convertToResponse);

        } catch (Exception e) {
            log.error("Error getting applications: {}", e.getMessage());
            return new PageImpl<>(new ArrayList<>());
        }
    }

    @Override
    public ApplicationResponse getApplicationDetails(Long applicationId, Long seekerId) {
        try {
            log.info("Getting application details - id: {}, seekerId: {}", applicationId, seekerId);

            if (applicationId == null || seekerId == null) {
                throw new BadRequestException("Invalid request");
            }

            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

            log.info("Found application - ID: {}, Job ID: {}, Status: {}, Resume: {}", 
                application.getId(), 
                application.getJob() != null ? application.getJob().getId() : null,
                application.getStatus(),
                application.getResume() != null ? 
                    "ID: " + application.getResume().getId() + ", File: " + application.getResume().getFileName() : 
                    "No Resume");

            // Verify ownership
            if (!application.getSeeker().getId().equals(seekerId)) {
                log.error("Ownership mismatch: Application belongs to user {}, but requested by user {}", 
                    application.getSeeker().getId(), seekerId);
                throw new UnauthorizedException("Access denied");
            }

            return convertToResponse(application);

        } catch (BadRequestException | ResourceNotFoundException | UnauthorizedException e) {
            log.error("Error getting application details: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting application details: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load application");
        }
    }

    @Override
    @Transactional
    public void withdrawApplication(Long applicationId, Long seekerId, WithdrawRequest request) {
        log.info("========== WITHDRAW ATTEMPT ==========");
        log.info("Attempting to withdraw application ID: {} for user ID: {}", applicationId, seekerId);
        
        try {
            // Validate inputs
            if (applicationId == null) {
                log.error("Application ID is null");
                throw new BadRequestException("Application ID is required");
            }
            if (seekerId == null) {
                log.error("User ID is null");
                throw new BadRequestException("User ID is required");
            }

            // Find the application
            log.info("Searching for application with ID: {}", applicationId);
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> {
                        log.error("No application found with ID: {}", applicationId);
                        // Log all available application IDs for debugging
                        List<Application> allApps = applicationRepository.findAll();
                        log.info("Available application IDs in database: {}", 
                            allApps.stream().map(Application::getId).toList());
                        return new ResourceNotFoundException("Application not found with id: " + applicationId);
                    });

            log.info("Found application: ID={}, Status={}, SeekerID={}, JobID={}, ResumeID={}, ResumeFile={}", 
                application.getId(), 
                application.getStatus(), 
                application.getSeeker().getId(),
                application.getJob() != null ? application.getJob().getId() : null,
                application.getResume() != null ? application.getResume().getId() : "No Resume",
                application.getResume() != null ? application.getResume().getFileName() : "No File");

            // Verify ownership
            if (!application.getSeeker().getId().equals(seekerId)) {
                log.error("Ownership mismatch: Application owner ID={}, Requesting user ID={}", 
                    application.getSeeker().getId(), seekerId);
                throw new UnauthorizedException("You don't have permission to withdraw this application");
            }

            // Check if already withdrawn
            if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
                log.error("Application {} is already withdrawn", applicationId);
                throw new BadRequestException("Application is already withdrawn");
            }

            // Check if can withdraw (not shortlisted or rejected)
            if (application.getStatus() == ApplicationStatus.SHORTLISTED) {
                log.error("Application {} is shortlisted and cannot be withdrawn", applicationId);
                throw new BadRequestException("Cannot withdraw a shortlisted application");
            }
            if (application.getStatus() == ApplicationStatus.REJECTED) {
                log.error("Application {} is rejected and cannot be withdrawn", applicationId);
                throw new BadRequestException("Cannot withdraw a rejected application");
            }

            // Update status
            log.info("Updating application {} status from {} to WITHDRAWN", applicationId, application.getStatus());
            application.setStatus(ApplicationStatus.WITHDRAWN);
            
            // Set withdraw reason
            String reason = (request != null && request.getReason() != null && !request.getReason().isEmpty()) 
                ? request.getReason() : "Withdrawn by user";
            application.setWithdrawReason(reason);
            application.setUpdatedAt(LocalDateTime.now());
            log.info("Withdraw reason: {}", reason);

            // Save
            applicationRepository.save(application);
            
            log.info("✅ Application {} withdrawn successfully", applicationId);
            log.info("========== WITHDRAW SUCCESS ==========");

        } catch (ResourceNotFoundException e) {
            log.error("❌ Application not found: {}", e.getMessage());
            throw e;
        } catch (BadRequestException e) {
            log.error("❌ Bad request: {}", e.getMessage());
            throw e;
        } catch (UnauthorizedException e) {
            log.error("❌ Unauthorized: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error withdrawing application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to withdraw application: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasApplied(Long jobId, Long seekerId) {
        try {
            if (jobId == null || seekerId == null) {
                return false;
            }
            boolean applied = applicationRepository.existsByJobIdAndSeekerId(jobId, seekerId);
            log.info("User {} has applied for job {}: {}", seekerId, jobId, applied);
            return applied;
        } catch (Exception e) {
            log.error("Error checking application status: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public long getApplicationCountForJob(Long jobId) {
        try {
            if (jobId == null) {
                return 0;
            }
            long count = applicationRepository.countByJobId(jobId);
            log.info("Job {} has {} applications", jobId, count);
            return count;
        } catch (Exception e) {
            log.error("Error getting application count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Convert Application entity to ApplicationResponse DTO
     */
    private ApplicationResponse convertToResponse(Application application) {
        try {
            if (application == null) {
                log.warn("Attempted to convert null application");
                return null;
            }

            log.debug("Converting application {} to response", application.getId());

            ApplicationResponse.ApplicationResponseBuilder builder = ApplicationResponse.builder()
                    .id(application.getId())
                    .status(application.getStatus())
                    .coverLetter(application.getCoverLetter() != null ? application.getCoverLetter() : "")
                    .employerComment(application.getEmployerComment())
                    .appliedAt(application.getAppliedAt())
                    .updatedAt(application.getUpdatedAt());

            // Job details
            if (application.getJob() != null) {
                Job job = application.getJob();
                builder.jobId(job.getId());
                
                String jobTitle = job.getTitle();
                builder.jobTitle(jobTitle != null && !jobTitle.trim().isEmpty() ? jobTitle : "Position #" + job.getId());
                
                if (job.getEmployer() != null) {
                    String companyName = job.getEmployer().getCompanyName();
                    builder.companyName(companyName != null && !companyName.trim().isEmpty() ? companyName : "Company #" + job.getEmployer().getId());
                } else {
                    builder.companyName("Company Not Specified");
                }
            } else {
                builder.jobId(null);
                builder.jobTitle("Job Unavailable");
                builder.companyName("Not Available");
            }

            // Seeker details
            if (application.getSeeker() != null) {
                User seeker = application.getSeeker();
                
                String seekerName = seeker.getName();
                if (seekerName != null && !seekerName.trim().isEmpty()) {
                    builder.seekerName(seekerName);
                } else {
                    String email = seeker.getEmail();
                    if (email != null && email.contains("@")) {
                        builder.seekerName(email.substring(0, email.indexOf('@')));
                    } else {
                        builder.seekerName("Job Seeker #" + seeker.getId());
                    }
                }
                
                String seekerEmail = seeker.getEmail();
                builder.seekerEmail(seekerEmail != null && !seekerEmail.trim().isEmpty() ? seekerEmail : "email" + seeker.getId() + "@example.com");
            } else {
                builder.seekerName("Job Seeker");
                builder.seekerEmail("email@example.com");
            }

            // Resume details
            String resumeDisplayName = null;
            
            if (application.getResume() != null) {
                Resume resume = application.getResume();
                builder.resumeId(resume.getId());
                
                String resumeFileName = resume.getFileName();
                if (resumeFileName != null && !resumeFileName.trim().isEmpty()) {
                    builder.resumeFileName(resumeFileName);
                    resumeDisplayName = resumeFileName;
                } else {
                    String objective = resume.getObjective();
                    if (objective != null && !objective.trim().isEmpty()) {
                        String shortObjective = objective.length() > 30 ? objective.substring(0, 27) + "..." : objective;
                        builder.resumeFileName("Resume: " + shortObjective);
                        resumeDisplayName = "Resume: " + shortObjective;
                    } else {
                        builder.resumeFileName("Resume #" + resume.getId());
                        resumeDisplayName = "Resume #" + resume.getId();
                    }
                }
                log.debug("Mapped resume - ID: {}, FileName: {}", resume.getId(), resumeDisplayName);
            } else {
                builder.resumeId(null);
                builder.resumeFileName(null);
            }

            ApplicationResponse response = builder.build();
            
            log.info("=== APPLICATION RESPONSE ===");
            log.info("ID: {}, Job: {}, Company: {}", response.getId(), response.getJobTitle(), response.getCompanyName());
            log.info("Seeker: {}, Email: {}", response.getSeekerName(), response.getSeekerEmail());
            log.info("Resume: {} ({})", response.getResumeFileName(), response.getResumeId());
            log.info("==========================");
            
            return response;

        } catch (Exception e) {
            log.error("Error converting application {}: {}", 
                application != null ? application.getId() : "null", e.getMessage(), e);
            
            return ApplicationResponse.builder()
                    .id(application != null ? application.getId() : null)
                    .jobId(application != null && application.getJob() != null ? application.getJob().getId() : null)
                    .jobTitle("Error Loading Details")
                    .companyName("Not Available")
                    .seekerName("Job Seeker")
                    .seekerEmail("email@example.com")
                    .status(application != null ? application.getStatus() : null)
                    .build();
        }
    }
}