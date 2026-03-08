package com.revhire.employer.service;

import com.revhire.application.repository.ApplicationRepository;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.common.enums.JobStatus;
import com.revhire.employer.dto.DashboardStats;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.ApplicantRepository;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployerRepository employerRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
  
    @Override
    public DashboardStats getEmployerDashboardStats(String email) {

        Employer employer = employerRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Employer not found"));

        Long employerId = employer.getId();

        long boomingJobs = jobRepository.countByEmployerId(employerId);
        long activeJobs = jobRepository.countByEmployerIdAndStatus(employerId, JobStatus.ACTIVE);
        long totalApplications = applicationRepository.countByJobEmployerId(employerId);
        
        // Use a list of statuses to ensure consistency with the Review Page
        List<ApplicationStatus> pendingStatuses = List.of(
            ApplicationStatus.APPLIED, 
            ApplicationStatus.UNDER_REVIEW
        );
        
        long pendingReviews = applicantRepository
                .countByJobEmployerIdAndStatusIn(employerId, pendingStatuses);

        return new DashboardStats(
                employer.getCompanyName(),
                boomingJobs,
                activeJobs,
                totalApplications,
                pendingReviews
        );
    }
}