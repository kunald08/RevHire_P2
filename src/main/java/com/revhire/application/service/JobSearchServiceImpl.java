package com.revhire.application.service;

import com.revhire.application.repository.ApplicationRepository;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.JobSearchFilter;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import com.revhire.job.specification.JobSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Job search service using Spring Data JPA Specifications for efficient
 * database-level filtering instead of in-memory filtering.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class JobSearchServiceImpl implements JobSearchService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    public Page<JobResponse> searchJobs(JobSearchFilter filter, Pageable pageable) {
        log.info("Starting job search with filter: {}", filter);

        try {
            // Build specification from filter criteria
            Specification<Job> spec = Specification.where(JobSpecification.isActive());

            if (filter != null) {
                if (filter.hasKeyword()) {
                    spec = spec.and(JobSpecification.keywordSearch(filter.getKeyword()));
                }
                if (filter.hasLocation()) {
                    spec = spec.and(JobSpecification.locationContains(filter.getLocation()));
                }
                if (filter.hasCompany()) {
                    spec = spec.and(JobSpecification.companyName(filter.getCompany()));
                }
                if (filter.getMinExperience() != null) {
                    spec = spec.and(JobSpecification.minExperience(filter.getMinExperience()));
                }
                if (filter.getMaxExperience() != null) {
                    spec = spec.and(JobSpecification.maxExperience(filter.getMaxExperience()));
                }
                if (filter.getMinSalary() != null && filter.getMinSalary() > 0) {
                    spec = spec.and(JobSpecification.minSalary(BigDecimal.valueOf(filter.getMinSalary())));
                }
                if (filter.getMaxSalary() != null && filter.getMaxSalary() > 0) {
                    spec = spec.and(JobSpecification.maxSalary(BigDecimal.valueOf(filter.getMaxSalary())));
                }
                if (filter.getJobType() != null) {
                    spec = spec.and(JobSpecification.jobType(filter.getJobType()));
                }
                if (filter.getDatePostedAfter() != null) {
                    spec = spec.and(JobSpecification.postedAfter(filter.getDatePostedAfter()));
                }
            }

            Page<Job> jobs = jobRepository.findAll(spec, pageable);
            log.info("Jobs found: {}", jobs.getTotalElements());

            return jobs.map(this::convertToResponse);

        } catch (Exception e) {
            log.error("Error in job search: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }

    @Override
    public JobResponse getJobDetails(Long jobId) {
        log.info("Fetching job details for ID: {}", jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        return convertToResponse(job);
    }

    /**
     * Convert Job entity to JobResponse DTO including employer/company info.
     */
    private JobResponse convertToResponse(Job job) {
        if (job == null) return null;

        try {
            long appCount = applicationRepository.countByJobId(job.getId());

            JobResponse.JobResponseBuilder builder = JobResponse.builder()
                    .id(job.getId())
                    .title(job.getTitle() != null ? job.getTitle() : "Untitled")
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
                    .createdAt(job.getCreatedAt())
                    .applicationCount(appCount);

            // Include employer/company info
            if (job.getEmployer() != null) {
                builder.employerId(job.getEmployer().getId())
                        .companyName(job.getEmployer().getCompanyName())
                        .companyLocation(job.getEmployer().getLocation())
                        .industry(job.getEmployer().getIndustry());
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Error converting job {}: {}", job.getId(), e.getMessage());
            return null;
        }
    }
}