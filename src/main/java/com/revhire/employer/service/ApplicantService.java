package com.revhire.employer.service;

import com.revhire.job.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicantService {
    
    /**
     * Retrieves a paginated list of jobs for a specific employer email,
     * optionally filtered by a keyword in the job title.
     */
    Page<Job> getEmployerJobsWithApplications(
            String email, 
            String keyword, 
            Pageable pageable
    );
}