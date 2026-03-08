package com.revhire.job.service;

import com.revhire.application.repository.ApplicationRepository;
import com.revhire.application.repository.FavoriteRepository;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.ApplicationStatus;
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
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production-grade implementation of {@link JobService}.
 * Module 3 — Employer Profile & Job Posting.
 */
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private static final Logger logger = LogManager.getLogger(JobServiceImpl.class);
    private static final int PAGE_SIZE = 6;

    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final FavoriteRepository favoriteRepository;

    // ══════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════

    @Override
    @Transactional
    public JobResponse createJob(JobRequest request, String email) {

        logger.info("Creating job — employer: {}", email);

        Employer employer = getEmployerForEmail(email);
        validateJobRequest(request);

        Job job = Job.builder()
                .employer(employer)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .requiredSkills(request.getRequiredSkills())
                .experienceMin(request.getExperienceMin())
                .experienceMax(request.getExperienceMax())
                .educationReq(request.getEducationReq())
                .location(request.getLocation().trim())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .jobType(request.getJobType())
                .deadline(request.getDeadline())
                .numOpenings(request.getNumOpenings())
                .status(JobStatus.ACTIVE)
                .viewCount(0L)
                .build();

        Job saved = jobRepository.save(job);
        logger.info("Job created — ID: {}, title: {}", saved.getId(), saved.getTitle());

        return mapToResponse(saved);
    }

    // ══════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════

    @Override
    @Transactional
    public JobResponse updateJob(Long id, JobRequest request, String email) {

        logger.info("Updating job ID: {} — employer: {}", id, email);

        Job job = getJobIfOwner(id, email);
        validateJobRequest(request);

        job.setTitle(request.getTitle().trim());
        job.setDescription(request.getDescription().trim());
        job.setLocation(request.getLocation().trim());
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
        logger.info("Job updated — ID: {}", id);

        return mapToResponse(updated);
    }

    // ══════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════

    @Override
    @Transactional
    public void deleteJob(Long id, String email) {

        logger.info("Deleting job ID: {} — employer: {}", id, email);

        Job job = getJobIfOwner(id, email);

        if (job.getStatus() == JobStatus.FILLED) {
            throw new BadRequestException("Cannot delete a filled job posting.");
        }
        
        long applications = applicationRepository.countByJobId(id);
        if (applications > 0) {
            throw new BadRequestException("Cannot delete a job with applications.");
        }

        // remove favorites first
        favoriteRepository.deleteByJobId(id);


        jobRepository.delete(job);
        logger.info("Job deleted — ID: {}", id);
    }

    // ══════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════

    @Override
    @Transactional
    public void closeJob(Long id, String email) {
        changeStatus(id, JobStatus.CLOSED, email);
    }

    @Override
    @Transactional
    public void reopenJob(Long id, String email) {
        changeStatus(id, JobStatus.ACTIVE, email);
    }

    @Override
    @Transactional
    public void markAsFilled(Long id, String email) {
        changeStatus(id, JobStatus.FILLED, email);
    }

    private void changeStatus(Long id, JobStatus newStatus, String email) {
        Job job = getJobIfOwner(id, email);
        job.setStatus(newStatus);
        jobRepository.save(job);
        logger.info("Job ID: {} status changed to {}", id, newStatus);
    }

    // ══════════════════════════════════════════════
    //  READ — single
    // ══════════════════════════════════════════════

    @Override
    public JobResponse getJobById(Long id, String email) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found — ID: " + id));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // Increment view count only for job seekers
        if (user.getRole() == Role.SEEKER) {
            job.setViewCount((job.getViewCount() == null ? 0L : job.getViewCount()) + 1);
            jobRepository.save(job);
        }

        return mapToResponse(job);
    }

    @Override
    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found — ID: " + id));
        return mapToResponse(job);
    }

    // ══════════════════════════════════════════════
    //  READ — lists
    // ══════════════════════════════════════════════

    @Override
    public List<JobResponse> getEmployerJobs(String email) {

        Employer employer = getEmployerForEmail(email);

        return jobRepository.findByEmployer(employer)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<JobResponse> getEmployerJobs(String email, int page) {

        Employer employer = getEmployerForEmail(email);

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
        Page<Job> jobsPage = jobRepository.findByEmployer(employer, pageable);

        return jobsPage.map(this::mapToResponse);
    }

    @Override
    public List<JobResponse> getActiveJobsByEmployer(String email) {

        return jobRepository.findByEmployer_User_EmailAndStatus(email, JobStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public Page<JobResponse> getEmployerJobs(String email, int page, String keyword, String status) {

        Employer employer = getEmployerForEmail(email);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());

        JobStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = JobStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException ignored) { /* invalid status → no filter */ }
        }

        Page<Job> jobsPage = jobRepository.findByEmployerFiltered(
                employer,
                keyword != null && keyword.isBlank() ? null : keyword,
                statusEnum,
                pageable);

        return jobsPage.map(this::mapToResponse);
    }

    // ══════════════════════════════════════════════
    //  DRAFT + DUPLICATE
    // ══════════════════════════════════════════════

    @Override
    @Transactional
    public JobResponse createDraftJob(JobRequest request, String email) {

        logger.info("Saving draft job — employer: {}", email);

        Employer employer = getEmployerForEmail(email);

        Job job = Job.builder()
                .employer(employer)
                .title(request.getTitle() != null ? request.getTitle().trim() : "Untitled Draft")
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .requiredSkills(request.getRequiredSkills())
                .experienceMin(request.getExperienceMin())
                .experienceMax(request.getExperienceMax())
                .educationReq(request.getEducationReq())
                .location(request.getLocation() != null ? request.getLocation().trim() : "")
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .jobType(request.getJobType())
                .deadline(request.getDeadline())
                .numOpenings(request.getNumOpenings() != null ? request.getNumOpenings() : 1)
                .status(JobStatus.DRAFT)
                .viewCount(0L)
                .build();

        Job saved = jobRepository.save(job);
        logger.info("Draft saved — ID: {}, title: {}", saved.getId(), saved.getTitle());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void publishDraft(Long id, String email) {

        Job job = getJobIfOwner(id, email);

        if (job.getStatus() != JobStatus.DRAFT) {
            throw new BadRequestException("Only draft jobs can be published.");
        }

        // Validate before publishing
        if (job.getTitle() == null || job.getTitle().isBlank()) {
            throw new BadRequestException("Job title is required before publishing.");
        }
        if (job.getDeadline() == null || !job.getDeadline().isAfter(LocalDate.now())) {
            throw new BadRequestException("A future deadline is required before publishing.");
        }

        job.setStatus(JobStatus.ACTIVE);
        jobRepository.save(job);
        logger.info("Draft published — Job ID: {}", id);
    }

    @Override
    @Transactional
    public JobResponse duplicateJob(Long id, String email) {

        Job source = getJobIfOwner(id, email);

        Job copy = Job.builder()
                .employer(source.getEmployer())
                .title(source.getTitle() + " (Copy)")
                .description(source.getDescription())
                .requiredSkills(source.getRequiredSkills())
                .experienceMin(source.getExperienceMin())
                .experienceMax(source.getExperienceMax())
                .educationReq(source.getEducationReq())
                .location(source.getLocation())
                .salaryMin(source.getSalaryMin())
                .salaryMax(source.getSalaryMax())
                .jobType(source.getJobType())
                .deadline(source.getDeadline())
                .numOpenings(source.getNumOpenings())
                .status(JobStatus.DRAFT)
                .viewCount(0L)
                .build();

        Job saved = jobRepository.save(copy);
        logger.info("Job duplicated — source: {}, new: {}", id, saved.getId());

        return mapToResponse(saved);
    }

    // ══════════════════════════════════════════════
    //  AUTO-EXPIRY
    // ══════════════════════════════════════════════

    @Override
    @Transactional
    public int closeExpiredJobs() {
        int count = jobRepository.closeExpiredJobs(LocalDate.now());
        if (count > 0) {
            logger.info("Auto-closed {} expired job(s)", count);
        }
        return count;
    }

    // ══════════════════════════════════════════════
    //  STATISTICS
    // ══════════════════════════════════════════════

    @Override
    public JobStatsResponse getJobStatistics(Long jobId, String email) {

        Job job = getJobIfOwner(jobId, email);

        long total       = applicationRepository.countByJobId(jobId);
        long applied     = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.APPLIED);
        long underReview = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.UNDER_REVIEW);
        long shortlisted = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.SHORTLISTED);
        long rejected    = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.REJECTED);
        long withdrawn   = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.WITHDRAWN);

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
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0L)
                .numOpenings(job.getNumOpenings())
                .deadline(job.getDeadline())
                .build();
    }

    // ══════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════

    private Employer getEmployerForEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (user.getRole() != Role.EMPLOYER) {
            throw new UnauthorizedException("Only employers can perform this action.");
        }

        return employerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employer profile not found. Please create your company profile first."));
    }

    private Job getJobIfOwner(Long id, String email) {

        Employer employer = getEmployerForEmail(email);

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found — ID: " + id));

        if (!job.getEmployer().getId().equals(employer.getId())) {
            throw new UnauthorizedException("You do not have permission to modify this job.");
        }

        return job;
    }

    private void validateJobRequest(JobRequest request) {

        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new BadRequestException("Minimum salary cannot exceed maximum salary.");
        }

        if (request.getExperienceMin() != null && request.getExperienceMax() != null
                && request.getExperienceMin() > request.getExperienceMax()) {
            throw new BadRequestException("Minimum experience cannot exceed maximum experience.");
        }

        if (request.getDeadline() != null && !request.getDeadline().isAfter(LocalDate.now())) {
            throw new BadRequestException("Application deadline must be a future date.");
        }

        if (request.getNumOpenings() == null || request.getNumOpenings() < 1) {
            throw new BadRequestException("At least one position must be open.");
        }

        if (request.getJobType() == null) {
            throw new BadRequestException("Job type is required.");
        }
    }

    private JobResponse mapToResponse(Job job) {

        long appCount = applicationRepository.countByJobId(job.getId());

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
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0L)
                .employerId(job.getEmployer().getId())
                .companyName(job.getEmployer().getCompanyName())
                .companyLocation(job.getEmployer().getLocation())
                .industry(job.getEmployer().getIndustry())
                .applicationCount(appCount)
                .createdAt(job.getCreatedAt())
                .build();
    }
}