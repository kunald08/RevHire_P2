package com.revhire.application.service;

import com.revhire.job.dto.JobSearchFilter;
import com.revhire.job.dto.JobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobSearchService {
    Page<JobResponse> searchJobs(JobSearchFilter filter, Pageable pageable);
    JobResponse getJobDetails(Long jobId);
}