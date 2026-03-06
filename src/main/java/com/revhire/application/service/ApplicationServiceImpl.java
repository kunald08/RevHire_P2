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

    @Override
    @Transactional
    public ApplicationResponse applyForJob(Long seekerId, ApplicationRequest request) {
        try {
            log.info("Applying for job - seekerId: {}, jobId: {}", seekerId, request != null ? request.getJobId() : null);

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
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

            // Get user
            User user = userRepository.findById(seekerId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Create application
            Application application = new Application();
            application.setJob(job);
            application.setSeeker(user);
            application.setCoverLetter(request.getCoverLetter());
            application.setStatus(ApplicationStatus.APPLIED);

            Application saved = applicationRepository.save(application);
            log.info("Application saved with id: {}", saved.getId());

            return convertToResponse(saved);

        } catch (BadRequestException | ResourceNotFoundException e) {
            log.error("Business error applying for job: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error applying for job: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply: " + e.getMessage());
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
                            allApps.stream().map(a -> a.getId()).toList());
                        return new ResourceNotFoundException("Application not found with id: " + applicationId);
                    });

            log.info("Found application: ID={}, Status={}, SeekerID={}", 
                application.getId(), application.getStatus(), application.getSeeker().getId());

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

            // Job details
            if (application.getJob() != null) {
                builder.jobId(application.getJob().getId());
                builder.jobTitle(application.getJob().getTitle() != null ? 
                        application.getJob().getTitle() : "Unknown Job");

                // Company name from employer
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