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

            if (!application.getSeeker().getId().equals(seekerId)) {
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
            return applicationRepository.existsByJobIdAndSeekerId(jobId, seekerId);
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
            return applicationRepository.countByJobId(jobId);
        } catch (Exception e) {
            log.error("Error getting application count: {}", e.getMessage());
            return 0;
        }
    }

    private ApplicationResponse convertToResponse(Application application) {
        try {
            if (application == null) {
                return null;
            }

            ApplicationResponse.ApplicationResponseBuilder builder = ApplicationResponse.builder()
                    .id(application.getId())
                    .status(application.getStatus())
                    .coverLetter(application.getCoverLetter())
                    .employerComment(application.getEmployerComment())
                    .appliedAt(application.getAppliedAt())
                    .updatedAt(application.getUpdatedAt());

            // CRITICAL: Resume details - check if resume exists
            if (application.getResume() != null) {
                builder.resumeFileName(application.getResume().getFileName());
                log.debug("Added resume filename to response: {}", application.getResume().getFileName());
            } else {
                log.debug("No resume found for application {}", application.getId());
            }

            // Job details
            if (application.getJob() != null) {
                builder.jobId(application.getJob().getId());
                builder.jobTitle(application.getJob().getTitle() != null ? 
                        application.getJob().getTitle() : "Unknown Job");

                if (application.getJob().getEmployer() != null) {
                    builder.companyName(application.getJob().getEmployer().getCompanyName() != null ? 
                            application.getJob().getEmployer().getCompanyName() : "Unknown Company");
                }
            }

            // Seeker details
            if (application.getSeeker() != null) {
                builder.seekerName(application.getSeeker().getName() != null ? 
                        application.getSeeker().getName() : "Unknown User");
                builder.seekerEmail(application.getSeeker().getEmail());
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Error converting application: {}", e.getMessage());
            return ApplicationResponse.builder()
                    .id(application != null ? application.getId() : null)
                    .jobTitle("Error loading details")
                    .companyName("Unknown")
                    .seekerName("Unknown")
                    .status(application != null ? application.getStatus() : null)
                    .build();
        }
    }
}