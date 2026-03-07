package com.revhire.employer.service;

import com.revhire.employer.dto.EmployerRequest;
import com.revhire.employer.dto.EmployerResponse;

/**
 * Service interface for Employer / Company profile operations.
 */
public interface EmployerService {

    /** Create a new employer profile or update the existing one. */
    EmployerResponse createOrUpdateEmployer(EmployerRequest request, String email);

    /** Retrieve employer profile by the user's email address. */
    EmployerResponse getEmployerByEmail(String email);

    /** Retrieve employer profile by its primary key (public view). */
    EmployerResponse getEmployerById(Long id);

    /** Check whether a profile already exists for the given email. */
    boolean hasProfile(String email);
}