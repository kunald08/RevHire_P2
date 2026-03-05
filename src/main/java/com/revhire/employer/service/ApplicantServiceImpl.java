package com.revhire.employer.service;

import com.revhire.employer.repository.ApplicantRepository;
import com.revhire.job.entity.Job;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {

    private final ApplicantRepository applicantRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Job> getEmployerJobsWithApplications(
            String email,
            String keyword,
            Pageable pageable) {

        // Handle null keyword to prevent issues with the 'Containing' query
        String searchKeyword = (keyword == null) ? "" : keyword.trim();

        // This calls the custom method in ApplicantRepository 
        // that navigates Job -> Employer -> User -> Email
        return applicantRepository.findByEmployerUserEmailAndTitleContainingIgnoreCase(
                email,
                searchKeyword,
                pageable
        );
    }
}