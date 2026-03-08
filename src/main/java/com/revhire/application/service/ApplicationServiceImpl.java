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
import com.revhire.notification.service.NotificationService;
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
    private final NotificationService notificationService;

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
        log.info("========== APPLY FOR JOB ==========");
        log.info("Seeker ID: {}, Job ID: {}, Resume ID from request: {}", 
            seekerId, 
            request != null ? request.getJobId() : null,
            request != null ? request.getResumeId() : null);
        
        try {
            // Validate inputs
            if (seekerId == null) {
                throw new BadRequestException("Please login to apply");
            }
            if (request == null || request.getJobId() == null) {
                throw new BadRequestException("Invalid application request");
            }

            // Check if already applied
            if (applicationRepository.existsByJobIdAndSeekerId(request.getJobId(), seekerId)) {
                throw new BadRequestException("You have already applied for this job");
            }

            // Get job
            Job job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + request.getJobId()));
            log.info("Found job: {} (ID: {})", job.getTitle(), job.getId());

            // Get user
            User user = userRepository.findById(seekerId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + seekerId));
            log.info("Found user: {} (ID: {})", user.getEmail(), user.getId());

            // Handle resume - THIS IS CRITICAL
            Resume resume = null;
            if (request.getResumeId() != null) {
                log.info("Attempting to fetch resume with ID: {}", request.getResumeId());
                resume = resumeRepository.findById(request.getResumeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + request.getResumeId()));
                log.info("Found resume: {} (ID: {}, Profile ID: {})", 
                    resume.getFileName(), resume.getId(), 
                    resume.getProfile() != null ? resume.getProfile().getId() : "null");
            } else {
                log.warn("No resume ID provided in request - application will have no resume");
            }

            // Create application with resume
            Application application = Application.builder()
                    .job(job)
                    .seeker(user)
                    .resume(resume)  // This sets the resume_id
                    .coverLetter(request.getCoverLetter())
                    .status(ApplicationStatus.APPLIED)
                    .build();

            log.info("Application built with resume: {}", application.getResume() != null ? 
                application.getResume().getId() : "null");

            Application saved = applicationRepository.save(application);
            log.info("✅ Application saved with ID: {}, Resume ID in saved application: {}", 
                saved.getId(), 
                saved.getResume() != null ? saved.getResume().getId() : "NULL");

            // Send notification to the employer about the new application
            try {
                User employerUser = job.getEmployer().getUser();
                if (employerUser != null) {
                    notificationService.notifyNewApplication(employerUser, user.getName(), job.getTitle(), job.getId());
                }
            } catch (Exception notifEx) {
                log.warn("Failed to send notification for new application: {}", notifEx.getMessage());
            }

            return convertToResponse(saved);

        } catch (BadRequestException | ResourceNotFoundException e) {
            log.error("Business error applying for job: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error applying for job: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply: " + e.getMessage());
        } finally {
            log.info("========== APPLY FOR JOB END ==========");
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

            log.info("Application details - Resume ID: {}", 
                application.getResume() != null ? application.getResume().getId() : "null");

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
        log.info("Withdrawing application - id: {}, seekerId: {}", applicationId, seekerId);
        
        try {
            if (applicationId == null || seekerId == null) {
                throw new BadRequestException("Invalid request");
            }

            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

            if (!application.getSeeker().getId().equals(seekerId)) {
                throw new UnauthorizedException("Access denied");
            }

            if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
                throw new BadRequestException("Application is already withdrawn");
            }

            if (application.getStatus() == ApplicationStatus.SHORTLISTED) {
                throw new BadRequestException("Cannot withdraw a shortlisted application");
            }
            if (application.getStatus() == ApplicationStatus.REJECTED) {
                throw new BadRequestException("Cannot withdraw a rejected application");
            }

            application.setStatus(ApplicationStatus.WITHDRAWN);
            
            String reason = (request != null && request.getReason() != null && !request.getReason().isEmpty()) 
                ? request.getReason() : "Withdrawn by user";
            application.setWithdrawReason(reason);
            application.setUpdatedAt(LocalDateTime.now());

            applicationRepository.save(application);
            
            log.info("Application {} withdrawn successfully", applicationId);

            // Send notifications for withdrawal
            try {
                User seeker = application.getSeeker();
                Job job = application.getJob();
                // Notify the seeker about successful withdrawal
                notificationService.notifyApplicationWithdrawn(seeker, job.getTitle(), applicationId);
                // Notify the employer about the withdrawal
                User employerUser = job.getEmployer().getUser();
                if (employerUser != null) {
                    notificationService.notifyEmployerApplicationWithdrawn(
                            employerUser, seeker.getName(), job.getTitle(), job.getId());
                }
            } catch (Exception notifEx) {
                log.warn("Failed to send withdrawal notifications: {}", notifEx.getMessage());
            }

        } catch (Exception e) {
            log.error("Error withdrawing application: {}", e.getMessage(), e);
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
                builder.jobId(application.getJob().getId());
                builder.jobTitle(application.getJob().getTitle() != null ? 
                        application.getJob().getTitle() : "Unknown Job");

                if (application.getJob().getEmployer() != null) {
                    builder.companyName(application.getJob().getEmployer().getCompanyName() != null ? 
                            application.getJob().getEmployer().getCompanyName() : "Unknown Company");
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