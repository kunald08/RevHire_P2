package com.revhire.employer.service;

import com.revhire.employer.dto.EmployerRequest;
import com.revhire.employer.dto.EmployerResponse;

public interface EmployerService {

    EmployerResponse createOrUpdateEmployer(EmployerRequest request, String email);

    EmployerResponse getEmployerByEmail(String email);

    EmployerResponse getEmployerById(Long id);
}