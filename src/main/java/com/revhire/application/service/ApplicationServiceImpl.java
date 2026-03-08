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
import com.revhire.employer.entity.Employer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;

    @PersistenceContext
    private EntityManager entityManager;

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
            
            // Set resume if provided
            if (request.getResumeId() != null) {
                Resume resume = resumeRepository.findById(request.getResumeId()).orElse(null);
                if (resume != null) {
                    application.setResume(resume);
                }
            }

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
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(Long seekerId, Pageable pageable) {
        try {
            log.info("Getting applications for seekerId: {}", seekerId);

            if (seekerId == null) {
                return new PageImpl<>(new ArrayList<>());
            }

            Page<Application> appPage = applicationRepository.findBySeekerId(seekerId, pageable);
            List<ApplicationResponse> responses = appPage.getContent().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return new PageImpl<>(responses, pageable, appPage.getTotalElements());

        } catch (Exception e) {
            log.error("Error getting applications: {}", e.getMessage(), e);
            return new PageImpl<>(new ArrayList<>());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationDetails(Long applicationId, Long seekerId) {
        try {
            log.info("Getting application details - id: {}, seekerId: {}", applicationId, seekerId);

            if (applicationId == null || seekerId == null) {
                throw new BadRequestException("Invalid request");
            }

            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

            // Authorization check
            Long appSeekerId = application.getSeeker() != null ? application.getSeeker().getId() : null;
            if (appSeekerId == null) {
                // Fallback: lookup seeker_id directly via native
                appSeekerId = application.getSeeker().getId();
            }
            if (!seekerId.equals(appSeekerId)) {
                throw new UnauthorizedException("Access denied");
            }

            // Build response manually to avoid lazy loading issues
            return buildResponseManually(application);

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

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplicationsByStatus(Long seekerId, ApplicationStatus status, Pageable pageable) {
        try {
            log.info("Getting applications for seekerId: {} with status: {}", seekerId, status);

            if (seekerId == null) {
                return new PageImpl<>(new ArrayList<>());
            }

            Page<Application> appPage;
            if (status == null) {
                appPage = applicationRepository.findBySeekerId(seekerId, pageable);
            } else {
                appPage = applicationRepository.findBySeekerIdAndStatus(seekerId, status, pageable);
            }
            
            List<ApplicationResponse> responses = appPage.getContent().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return new PageImpl<>(responses, pageable, appPage.getTotalElements());

        } catch (Exception e) {
            log.error("Error getting applications by status: {}", e.getMessage(), e);
            return new PageImpl<>(new ArrayList<>());
        }
    }

    @Override
    public long countByStatus(Long seekerId, ApplicationStatus status) {
        try {
            if (seekerId == null) {
                return 0;
            }
            if (status == null) {
                return applicationRepository.countBySeekerId(seekerId);
            }
            return applicationRepository.countBySeekerIdAndStatus(seekerId, status);
        } catch (Exception e) {
            log.error("Error counting applications by status: {}", e.getMessage());
            return 0;
        }
    }

    private ApplicationResponse convertToResponse(Application application) {
        if (application == null) return null;
        return buildResponseManually(application);
    }

    /**
     * Build ApplicationResponse by loading related entities through separate repository calls
     * to completely avoid Hibernate lazy-loading proxy issues.
     */
    private ApplicationResponse buildResponseManually(Application application) {
        try {
            ApplicationResponse.ApplicationResponseBuilder builder = ApplicationResponse.builder()
                    .id(application.getId())
                    .status(application.getStatus())
                    .coverLetter(application.getCoverLetter())
                    .employerComment(application.getEmployerComment())
                    .withdrawReason(application.getWithdrawReason())
                    .appliedAt(application.getAppliedAt())
                    .updatedAt(application.getUpdatedAt());

            // Load Job details via separate query to avoid proxy issues
            try {
                Long jobId = null;
                try {
                    if (application.getJob() != null) {
                        jobId = application.getJob().getId();
                    }
                } catch (Exception e) {
                    log.warn("Could not access job proxy for application {}, will try native query", application.getId());
                }

                if (jobId == null) {
                    // Fallback: native query to get job_id
                    try {
                        Object result = entityManager.createNativeQuery(
                            "SELECT job_id FROM applications WHERE id = :appId")
                            .setParameter("appId", application.getId())
                            .getSingleResult();
                        if (result != null) {
                            jobId = ((Number) result).longValue();
                        }
                    } catch (Exception e) {
                        log.warn("Native query fallback failed for application {}", application.getId());
                    }
                }

                if (jobId != null) {
                    builder.jobId(jobId);
                    Job job = jobRepository.findById(jobId).orElse(null);
                    if (job != null) {
                        builder.jobTitle(job.getTitle() != null ? job.getTitle() : "Unknown Job");
                        builder.jobLocation(job.getLocation());
                        builder.jobType(job.getJobType() != null ? job.getJobType().toString() : null);
                        builder.jobDescription(job.getDescription());
                        builder.requiredSkills(job.getRequiredSkills());
                        builder.salaryMin(job.getSalaryMin());
                        builder.salaryMax(job.getSalaryMax());
                        builder.experienceMin(job.getExperienceMin());
                        builder.experienceMax(job.getExperienceMax());
                        builder.educationReq(job.getEducationReq());
                        builder.deadline(job.getDeadline());
                        builder.numOpenings(job.getNumOpenings());

                        // Load employer
                        try {
                            Employer employer = job.getEmployer();
                            if (employer != null) {
                                builder.companyName(employer.getCompanyName() != null ? 
                                    employer.getCompanyName() : "Unknown Company");
                            }
                        } catch (Exception e) {
                            log.warn("Could not load employer for job {}", jobId);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error loading job for application {}: {}", application.getId(), e.getMessage());
            }

            // Load Seeker details
            try {
                Long seekerId = null;
                try {
                    if (application.getSeeker() != null) {
                        seekerId = application.getSeeker().getId();
                    }
                } catch (Exception e) {
                    log.warn("Could not access seeker proxy for application {}", application.getId());
                }

                if (seekerId == null) {
                    try {
                        Object result = entityManager.createNativeQuery(
                            "SELECT seeker_id FROM applications WHERE id = :appId")
                            .setParameter("appId", application.getId())
                            .getSingleResult();
                        if (result != null) {
                            seekerId = ((Number) result).longValue();
                        }
                    } catch (Exception e) {
                        log.warn("Native query fallback failed for seeker lookup");
                    }
                }

                if (seekerId != null) {
                    User seeker = userRepository.findById(seekerId).orElse(null);
                    if (seeker != null) {
                        builder.seekerName(seeker.getName() != null ? seeker.getName() : "Unknown User");
                        builder.seekerEmail(seeker.getEmail());
                    }
                }
            } catch (Exception e) {
                log.error("Error loading seeker for application {}: {}", application.getId(), e.getMessage());
            }

            // Load Resume details
            try {
                Long resumeId = null;
                try {
                    if (application.getResume() != null) {
                        resumeId = application.getResume().getId();
                    }
                } catch (Exception e) {
                    log.warn("Could not access resume proxy for application {}", application.getId());
                }

                if (resumeId == null) {
                    try {
                        Object result = entityManager.createNativeQuery(
                            "SELECT resume_id FROM applications WHERE id = :appId")
                            .setParameter("appId", application.getId())
                            .getSingleResult();
                        if (result != null) {
                            resumeId = ((Number) result).longValue();
                        }
                    } catch (Exception e) {
                        // resume_id can be null, this is fine
                    }
                }

                if (resumeId != null) {
                    Resume resume = resumeRepository.findById(resumeId).orElse(null);
                    if (resume != null) {
                        builder.resumeFileName(resume.getFileName());
                        builder.resumeId(resume.getId());
                    }
                }
            } catch (Exception e) {
                log.error("Error loading resume for application {}: {}", application.getId(), e.getMessage());
            }

            ApplicationResponse response = builder.build();
            log.info("Built response for app {}: jobTitle={}, status={}, resume={}, coverLetter={}", 
                application.getId(), response.getJobTitle(), response.getStatus(), 
                response.getResumeFileName(), 
                response.getCoverLetter() != null ? response.getCoverLetter().substring(0, Math.min(20, response.getCoverLetter().length())) : "null");
            return response;

        } catch (Exception e) {
            log.error("Error building response for application {}: {}", application.getId(), e.getMessage(), e);
            return ApplicationResponse.builder()
                    .id(application.getId())
                    .jobTitle("Error loading details")
                    .companyName("Unknown")
                    .seekerName("Unknown")
                    .status(application.getStatus())
                    .build();
        }
    }
}