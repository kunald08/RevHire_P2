package com.revhire.job.service;

import com.revhire.job.dto.JobRequest; 
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.JobStatsResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface JobService {

    JobResponse createJob(JobRequest request, String email);
    
    List<JobResponse> getEmployerJobs(String email);

    Page<JobResponse> getEmployerJobs(String email, int page);
    
    JobResponse getJobById(Long id);
    
    JobResponse getJobById(Long id, String email);

    void deleteJob(Long id, String email);

    void closeJob(Long id, String email);

    void reopenJob(Long id, String email);

    void markAsFilled(Long id, String email);
    
    JobResponse updateJob(Long id, JobRequest request, String email);
    
    JobStatsResponse getJobStatistics(Long jobId, String email);
    
}