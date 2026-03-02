package com.revhire.employer.service;

import com.revhire.auth.entity.User; 
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.Role;
import com.revhire.employer.dto.EmployerRequest;
import com.revhire.employer.dto.EmployerResponse;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.exception.BadRequestException;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployerServiceImpl implements EmployerService {

    private static final Logger logger = LogManager.getLogger(EmployerServiceImpl.class);

    private final EmployerRepository employerRepository;
    private final UserRepository userRepository;

    @Override
    public EmployerResponse createOrUpdateEmployer(EmployerRequest request, String email) {

        logger.info("Request received to create/update employer profile for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found while creating employer profile. Email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });

        if (user.getRole() != Role.EMPLOYER) {
            logger.warn("Unauthorized employer profile access attempt by user: {}", email);
            throw new UnauthorizedException("Only employers can manage company profiles.");
        }
        
        validateEmployerRequest(request);
        
        Employer employer = employerRepository.findByUser(user)
                .orElseGet(() -> {
                    logger.info("No existing employer profile found. Creating new profile for user: {}", email);
                    return Employer.builder().user(user).build();
                });

        employer.setCompanyName(request.getCompanyName());
        employer.setIndustry(request.getIndustry());
        employer.setCompanySize(request.getCompanySize());
        employer.setDescription(request.getDescription());
        employer.setWebsite(request.getWebsite());
        employer.setLocation(request.getLocation());

        Employer saved = employerRepository.save(employer);

        logger.info("Employer profile saved successfully. Employer ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public EmployerResponse getEmployerByEmail(String email) {

        logger.debug("Fetching employer profile by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found while fetching employer by email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });

        Employer employer = employerRepository.findByUser(user)
                .orElseThrow(() -> {
                    logger.warn("Employer profile not found for user: {}", email);
                    return new ResourceNotFoundException("Employer profile not found.");
                });

        logger.info("Employer profile retrieved successfully for user: {}", email);

        return mapToResponse(employer);
    }

    @Override
    public EmployerResponse getEmployerById(Long id) {

        logger.debug("Fetching employer profile by ID: {}", id);

        Employer employer = employerRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Employer not found with ID: {}", id);
                    return new ResourceNotFoundException("Employer not found with ID: " + id);
                });

        logger.info("Employer profile retrieved successfully for ID: {}", id);

        return mapToResponse(employer);
    }

    private EmployerResponse mapToResponse(Employer employer) {

        logger.debug("Mapping Employer entity to EmployerResponse DTO. ID: {}", employer.getId());

        return EmployerResponse.builder()
                .id(employer.getId())
                .companyName(employer.getCompanyName())
                .industry(employer.getIndustry())
                .companySize(employer.getCompanySize())
                .description(employer.getDescription())
                .website(employer.getWebsite())
                .location(employer.getLocation())
                .build();
    }
    
    private void validateEmployerRequest(EmployerRequest request) {

        if (request.getCompanyName().trim().length() < 2) {
            throw new BadRequestException("Company name must be at least 2 characters.");
        }

        if (request.getDescription().trim().length() < 10) {
            throw new BadRequestException("Description must be at least 10 characters.");
        }
    }
}