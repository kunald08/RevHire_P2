package com.revhire.employer.service;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.JobStatus;
import com.revhire.common.enums.Role;
import com.revhire.employer.dto.EmployerRequest;
import com.revhire.employer.dto.EmployerResponse;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.exception.BadRequestException;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.exception.UnauthorizedException;
import com.revhire.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link EmployerService}.
 * Handles company-profile CRUD and authorization checks.
 */
@Service
@RequiredArgsConstructor
public class EmployerServiceImpl implements EmployerService {

    private static final Logger logger = LogManager.getLogger(EmployerServiceImpl.class);

    private final EmployerRepository employerRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    // ────────────────────────────────────────────
    // CREATE / UPDATE
    // ────────────────────────────────────────────

    @Override
    @Transactional
    public EmployerResponse createOrUpdateEmployer(EmployerRequest request, String email) {

        logger.info("Create/update employer profile request from: {}", email);

        User user = findUserByEmail(email);
        assertEmployerRole(user);
        validateEmployerRequest(request);

        Employer employer = employerRepository.findByUser(user)
                .orElseGet(() -> {
                    logger.info("Creating new employer profile for: {}", email);
                    return Employer.builder().user(user).build();
                });

        employer.setCompanyName(request.getCompanyName().trim());
        employer.setIndustry(request.getIndustry().trim());
        employer.setCompanySize(request.getCompanySize());
        employer.setDescription(request.getDescription().trim());
        employer.setWebsite(request.getWebsite() != null ? request.getWebsite().trim() : null);
        employer.setLocation(request.getLocation().trim());

        Employer saved = employerRepository.save(employer);
        logger.info("Employer profile saved — ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    // ────────────────────────────────────────────
    // READ
    // ────────────────────────────────────────────

    @Override
    public EmployerResponse getEmployerByEmail(String email) {

        logger.debug("Fetching employer profile for email: {}", email);

        User user = findUserByEmail(email);

        Employer employer = employerRepository.findByUser(user)
                .orElseThrow(() -> {
                    logger.warn("Employer profile not found for: {}", email);
                    return new ResourceNotFoundException("Employer profile not found. Please create one first.");
                });

        return mapToResponse(employer);
    }

    @Override
    public EmployerResponse getEmployerById(Long id) {

        logger.debug("Fetching employer profile — ID: {}", id);

        Employer employer = employerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + id));

        return mapToResponse(employer);
    }

    @Override
    public boolean hasProfile(String email) {
        return employerRepository.findByUserEmail(email).isPresent();
    }

    @Override
    public String getCompanyName(String email) {
        return employerRepository.findCompanyNameByUserEmail(email).orElse(null);
    }

    // ────────────────────────────────────────────
    // PRIVATE HELPERS
    // ────────────────────────────────────────────

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void assertEmployerRole(User user) {
        if (user.getRole() != Role.EMPLOYER) {
            throw new UnauthorizedException("Only employers can manage company profiles.");
        }
    }

    private void validateEmployerRequest(EmployerRequest request) {
        if (request.getCompanyName().trim().length() < 2) {
            throw new BadRequestException("Company name must be at least 2 characters.");
        }
        if (request.getDescription().trim().length() < 10) {
            throw new BadRequestException("Description must be at least 10 characters.");
        }
    }

    private EmployerResponse mapToResponse(Employer employer) {

        long totalJobs = jobRepository.countByEmployerId(employer.getId());
        long activeJobs = jobRepository.countByEmployerIdAndStatus(employer.getId(), JobStatus.ACTIVE);

        return EmployerResponse.builder()
                .id(employer.getId())
                .companyName(employer.getCompanyName())
                .industry(employer.getIndustry())
                .companySize(employer.getCompanySize())
                .description(employer.getDescription())
                .website(employer.getWebsite())
                .location(employer.getLocation())
                .logoUrl(employer.getLogoUrl())
                .createdAt(employer.getCreatedAt())
                .totalJobs(totalJobs)
                .activeJobs(activeJobs)
                .build();
    }
}