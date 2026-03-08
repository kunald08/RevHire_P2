package com.revhire.application.service;

import com.revhire.job.dto.JobSearchFilter;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobSearchServiceImpl implements JobSearchService {
    
    private final JobRepository jobRepository;
    
    @Override
    public Page<JobResponse> searchJobs(JobSearchFilter filter, Pageable pageable) {
        log.info("Starting job search with filter: {}", filter);
        
        try {
            List<Job> allJobs = jobRepository.findAll();
            log.info("Total jobs in database: {}", allJobs.size());
            
            if (allJobs.isEmpty()) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
            
            List<Job> filteredJobs = allJobs.stream()
                .filter(job -> job != null)
                .filter(job -> applyFilters(job, filter))
                .collect(Collectors.toList());
            
            log.info("Jobs after filtering: {}", filteredJobs.size());
            
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredJobs.size());
            List<Job> pageContent = start < filteredJobs.size() ? 
                filteredJobs.subList(start, end) : new ArrayList<>();
            
            List<JobResponse> responses = pageContent.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return new PageImpl<>(responses, pageable, filteredJobs.size());
            
        } catch (Exception e) {
            log.error("Error in job search: {}", e.getMessage(), e);
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
    }
    
    @Override
    public JobResponse getJobDetails(Long jobId) {
        log.info("Fetching job details for ID: {}", jobId);
        
        try {
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
            
            return convertToResponse(job);
            
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching job details: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch job details", e);
        }
    }
    
    private boolean applyFilters(Job job, JobSearchFilter filter) {
        if (filter == null) return true;
        
        try {
            if (!isJobActive(job)) {
                return false;
            }
            
            if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
                String keyword = filter.getKeyword().toLowerCase();
                boolean keywordMatch = false;
                
                if (job.getTitle() != null && job.getTitle().toLowerCase().contains(keyword)) keywordMatch = true;
                if (!keywordMatch && job.getDescription() != null && job.getDescription().toLowerCase().contains(keyword)) keywordMatch = true;
                if (!keywordMatch && job.getRequiredSkills() != null && job.getRequiredSkills().toLowerCase().contains(keyword)) keywordMatch = true;
                
                if (!keywordMatch) return false;
            }
            
            if (filter.getLocation() != null && !filter.getLocation().isEmpty()) {
                String location = filter.getLocation().toLowerCase();
                if (job.getLocation() == null || !job.getLocation().toLowerCase().contains(location)) return false;
            }
            
            if (filter.getMinSalary() != null && filter.getMinSalary() > 0) {
                BigDecimal minSalary = BigDecimal.valueOf(filter.getMinSalary());
                if (job.getSalaryMax() == null || job.getSalaryMax().compareTo(minSalary) < 0) return false;
            }
            
            if (filter.getMaxSalary() != null && filter.getMaxSalary() > 0) {
                BigDecimal maxSalary = BigDecimal.valueOf(filter.getMaxSalary());
                if (job.getSalaryMin() == null || job.getSalaryMin().compareTo(maxSalary) > 0) return false;
            }
            
            if (filter.getJobType() != null) {
                if (job.getJobType() == null || !job.getJobType().toString().equals(filter.getJobType().toString())) return false;
            }
            
            if (filter.getCompany() != null && !filter.getCompany().isEmpty()) {
                String company = filter.getCompany().toLowerCase();
                boolean companyMatch = false;
                
                if (job.getEmployer() != null) {
                    try {
                        java.lang.reflect.Method method = job.getEmployer().getClass().getMethod("getCompanyName");
                        String companyName = (String) method.invoke(job.getEmployer());
                        if (companyName != null && companyName.toLowerCase().contains(company)) companyMatch = true;
                    } catch (Exception e) {
                        log.debug("Could not get company name for job {}", job.getId());
                    }
                }
                
                if (!companyMatch) return false;
            }
            
            if (filter.getMinExperience() != null) {
                if (job.getExperienceMin() == null || job.getExperienceMin() > filter.getMinExperience()) return false;
            }
            
            if (filter.getMaxExperience() != null) {
                if (job.getExperienceMax() == null || job.getExperienceMax() < filter.getMaxExperience()) return false;
            }
            
            if (filter.getDatePostedAfter() != null) {
                if (job.getCreatedAt() == null || job.getCreatedAt().toLocalDate().isBefore(filter.getDatePostedAfter())) return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error filtering job {}: {}", job.getId(), e.getMessage());
            return false;
        }
    }
    
    private boolean isJobActive(Job job) {
        try {
            Object status = job.getStatus();
            return status != null && "ACTIVE".equals(status.toString());
        } catch (Exception e) {
            return false;
        }
    }
    
    private JobResponse convertToResponse(Job job) {
        if (job == null) return null;
        
        try {
            String companyName = null;
            if (job.getEmployer() != null) {
                companyName = job.getEmployer().getCompanyName();
            }
            
            return JobResponse.builder()
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
                .companyName(companyName)
                .build();
            
        } catch (Exception e) {
            log.error("Error converting job {}: {}", job.getId(), e.getMessage());
            return null;
        }
    }
}