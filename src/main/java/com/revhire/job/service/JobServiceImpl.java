package com.revhire.job.service;

import com.revhire.application.repository.ApplicationRepository;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.JobStatus;
import com.revhire.common.enums.Role;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.exception.BadRequestException;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.exception.UnauthorizedException;
import com.revhire.job.dto.JobRequest;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.JobStatsResponse;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import com.revhire.notification.service.NotificationEventService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.revhire.common.enums.ApplicationStatus;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private static final Logger logger = LogManager.getLogger(JobServiceImpl.class);

    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationEventService notificationEventService;

    // ==============================
    // CREATE JOB
    // ==============================

    @Override
    @Transactional
    public JobResponse createJob(JobRequest request, String email) {

        logger.info("Create job request received from employer: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.getRole() != Role.EMPLOYER) {
            throw new UnauthorizedException("Only employers can create jobs.");
        }

        Employer employer = employerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Employer profile not found."));

        // 🔒 SERVICE DEFENSIVE VALIDATION
        validateJobRequest(request);

        Job job = Job.builder()
                .employer(employer)
                .title(request.getTitle())
                .description(request.getDescription())
                .requiredSkills(request.getRequiredSkills())
                .experienceMin(request.getExperienceMin())
                .experienceMax(request.getExperienceMax())
                .educationReq(request.getEducationReq())
                .location(request.getLocation())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .jobType(request.getJobType())
                .deadline(request.getDeadline())
                .numOpenings(request.getNumOpenings())
                .status(JobStatus.ACTIVE)
                .build();

        Job saved = jobRepository.save(job);

        logger.info("Job created successfully. ID: {}", saved.getId());

        // Send job recommendation notifications to seekers with matching skills
        try {
            notificationEventService.sendJobRecommendations(saved);
        } catch (Exception e) {
            logger.warn("Failed to send job recommendation notifications: {}", e.getMessage());
        }

        return mapToResponse(saved);
    }

    // ==============================
    // UPDATE JOB
    // ==============================

    @Override
    @Transactional
    public JobResponse updateJob(Long id, JobRequest request, String email) {

        logger.info("Update job request received. ID: {}, User: {}", id, email);

        Job job = getJobIfOwner(id, email);

        // 🔒 SERVICE DEFENSIVE VALIDATION
        validateJobRequest(request);

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setLocation(request.getLocation());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setExperienceMin(request.getExperienceMin());
        job.setExperienceMax(request.getExperienceMax());
        job.setEducationReq(request.getEducationReq());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setJobType(request.getJobType());
        job.setDeadline(request.getDeadline());
        job.setNumOpenings(request.getNumOpenings());

        Job updated = jobRepository.save(job);

        logger.info("Job updated successfully. ID: {}", id);

        return mapToResponse(updated);
    }

    // ==============================
    // DELETE JOB
    // ==============================

    @Override
    @Transactional
    public void deleteJob(Long id, String email) {

        Job job = getJobIfOwner(id, email);

        if (job.getStatus() == JobStatus.FILLED) {
            throw new BadRequestException("Cannot delete a filled job.");
        }

        jobRepository.delete(job);
    }

    // ==============================
    // LIFECYCLE MANAGEMENT
    // ==============================

    @Override
    public void closeJob(Long id, String email) {
        updateStatus(id, JobStatus.CLOSED, email);
    }

    @Override
    public void reopenJob(Long id, String email) {
        updateStatus(id, JobStatus.ACTIVE, email);
    }

    @Override
    public void markAsFilled(Long id, String email) {
        updateStatus(id, JobStatus.FILLED, email);
    }

    @Transactional
    private void updateStatus(Long id, JobStatus status, String email) {

        Job job = getJobIfOwner(id, email);
        job.setStatus(status);
        jobRepository.save(job);
    }

    // ==============================
    // GET JOB
    // ==============================

    @Override
    public JobResponse getJobById(Long id, String email) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + id));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Count only seeker views
        if (user.getRole() == Role.SEEKER) {

            if (job.getViewCount() == null) {
                job.setViewCount(0L);
            }

            job.setViewCount(job.getViewCount() + 1);
            jobRepository.save(job);
        }

        return mapToResponse(job);
    }

    // ==============================
    // GET EMPLOYER JOBS
    // ==============================

    @Override
    public List<JobResponse> getEmployerJobs(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Employer employer = employerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Employer profile not found."));

        return jobRepository.findByEmployer(employer)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==============================
    // STATS
    // ==============================

    @Override
    public JobStatsResponse getJobStatistics(Long jobId, String email) {

        Job job = getJobIfOwner(jobId, email);

        long total = applicationRepository.countByJobId(jobId);
        long applied = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.APPLIED);
        long underReview = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.UNDER_REVIEW);
        long shortlisted = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.SHORTLISTED);
        long rejected = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.REJECTED);
        long withdrawn = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.WITHDRAWN);

        return JobStatsResponse.builder()
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .totalApplications(total)
                .appliedCount(applied)
                .underReviewCount(underReview)
                .shortlistedCount(shortlisted)
                .rejectedCount(rejected)
                .withdrawnCount(withdrawn)
                .jobStatus(job.getStatus().name())
                .viewCount(job.getViewCount())
                .build();
    }

    // ==============================
    // PRIVATE METHODS
    // ==============================

    private Job getJobIfOwner(Long id, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Employer employer = employerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Employer profile not found."));

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + id));

        if (!job.getEmployer().getId().equals(employer.getId())) {
            throw new UnauthorizedException("You do not have permission to modify this job.");
        }

        return job;
    }

    private void validateJobRequest(JobRequest request) {

        if (request.getSalaryMin() != null && request.getSalaryMax() != null) {
            if (request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
                throw new BadRequestException("Minimum salary cannot be greater than maximum salary.");
            }
        }

        if (request.getExperienceMin() != null && request.getExperienceMax() != null) {
            if (request.getExperienceMin() > request.getExperienceMax()) {
                throw new BadRequestException("Minimum experience cannot be greater than maximum experience.");
            }
        }

        if (request.getDeadline() != null) {
            if (!request.getDeadline().isAfter(LocalDate.now())) {
                throw new BadRequestException("Deadline must be a future date.");
            }
        }

        if (request.getNumOpenings() == null || request.getNumOpenings() < 1) {
            throw new BadRequestException("There must be at least one opening.");
        }

        if (request.getJobType() == null) {
            throw new BadRequestException("Job type is required.");
        }
    }

    private JobResponse mapToResponse(Job job) {

        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .experienceMin(job.getExperienceMin())
                .experienceMax(job.getExperienceMax())
                .requiredSkills(job.getRequiredSkills())
                .educationReq(job.getEducationReq())
                .numOpenings(job.getNumOpenings())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .deadline(job.getDeadline())
                .viewCount(job.getViewCount())
                .build();
    }
    
    @Override
    public List<JobResponse> getActiveJobsByEmployer(String email) {

        List<Job> jobs = jobRepository
                .findByEmployer_User_EmailAndStatus(email, JobStatus.ACTIVE);

        return jobs.stream()
                .map(this::mapToResponse)
                .toList();
        }
    
    @Override
    public JobResponse getJobById(Long id) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + id));

        return mapToResponse(job);
    }
    
    @Override
    public Page<JobResponse> getEmployerJobs(String email, int page) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Employer employer = employerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Employer profile not found."));

        Pageable pageable = PageRequest.of(page, 5, Sort.by("id").descending());

        Page<Job> jobsPage = jobRepository.findByEmployer(employer, pageable);

        return jobsPage.map(this::mapToResponse);
    }
}