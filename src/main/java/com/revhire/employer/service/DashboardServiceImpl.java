package com.revhire.employer.service;

import com.revhire.application.repository.ApplicationRepository;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.common.enums.JobStatus;
import com.revhire.employer.dto.DashboardStats;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployerRepository employerRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
  
    @Override
    public DashboardStats getEmployerDashboardStats(String email) {

        Employer employer = employerRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Employer not found"));

        Long employerId = employer.getId();

        long boomingJobs = jobRepository.countByEmployerId(employerId);
        long activeJobs = jobRepository.countByEmployerIdAndStatus(employerId, JobStatus.ACTIVE);
        long totalApplications = applicationRepository.countByJobEmployerId(employerId);
        long pendingReviews = applicationRepository
                .countByJobEmployerIdAndStatus(employerId, ApplicationStatus.APPLIED);

        return new DashboardStats(
                boomingJobs,
                activeJobs,
                totalApplications,
                pendingReviews
        );
    }
}