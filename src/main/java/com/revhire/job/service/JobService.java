package com.revhire.job.service;

import com.revhire.job.dto.JobRequest;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.JobStatsResponse;

import java.util.List;

public interface JobService {

    JobResponse createJob(JobRequest request, String email);

    List<JobResponse> getEmployerJobs(String email);

    JobResponse getJobById(Long id);

    void deleteJob(Long id, String email);

    void closeJob(Long id, String email);

    void reopenJob(Long id, String email);

    void markAsFilled(Long id, String email);
    
    JobResponse updateJob(Long id, JobRequest request, String email);
    
    JobStatsResponse getJobStatistics(Long jobId, String email);
    
}