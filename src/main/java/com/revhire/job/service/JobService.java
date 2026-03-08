package com.revhire.job.service;

import com.revhire.job.dto.JobRequest; 
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.JobStatsResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface JobService {

    JobResponse createJob(JobRequest request, String email);
    
    /** Save a job as DRAFT (not visible to seekers yet). */
    JobResponse createDraftJob(JobRequest request, String email);

    /** Publish a DRAFT job (moves to ACTIVE). */
    void publishDraft(Long id, String email);

    /** Duplicate an existing job into a new DRAFT. */
    JobResponse duplicateJob(Long id, String email);

    List<JobResponse> getEmployerJobs(String email);

    Page<JobResponse> getEmployerJobs(String email, int page);

    /** Paginated + filtered employer jobs. */
    Page<JobResponse> getEmployerJobs(String email, int page, String keyword, String status);
    
    JobResponse getJobById(Long id);
    
    JobResponse getJobById(Long id, String email);

    void deleteJob(Long id, String email);

    void closeJob(Long id, String email);

    void reopenJob(Long id, String email);

    void markAsFilled(Long id, String email);
    
    JobResponse updateJob(Long id, JobRequest request, String email);
    
    JobStatsResponse getJobStatistics(Long jobId, String email);
    List<JobResponse> getActiveJobsByEmployer(String email);

    /** Auto-close expired jobs. Called by scheduler. */
    int closeExpiredJobs();
}