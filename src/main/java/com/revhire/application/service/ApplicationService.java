package com.revhire.application.service;

import com.revhire.application.dto.ApplicationRequest;
import com.revhire.application.dto.ApplicationResponse;
import com.revhire.application.dto.WithdrawRequest;
import com.revhire.common.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {
    
    ApplicationResponse applyForJob(Long seekerId, ApplicationRequest request);
    
    Page<ApplicationResponse> getMyApplications(Long seekerId, Pageable pageable);

    Page<ApplicationResponse> getMyApplicationsByStatus(Long seekerId, ApplicationStatus status, Pageable pageable);
    
    ApplicationResponse getApplicationDetails(Long applicationId, Long seekerId);
    
    void withdrawApplication(Long applicationId, Long seekerId, WithdrawRequest request);
    
    boolean hasApplied(Long jobId, Long seekerId);

    long countByStatus(Long seekerId, ApplicationStatus status);
    
    default long getApplicationCountForJob(Long jobId) {
        return 0;
    }
}