package com.revhire.application.repository;

import com.revhire.application.entity.Application;
import com.revhire.common.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    long countByJobId(Long jobId);

    long countByJobIdAndStatus(Long jobId, ApplicationStatus status);
    
    /**
     * Traverses Application -> Job -> Employer -> ID
     */
    long countByJobEmployerId(Long employerId);

    /**
     * Traverses Application -> Job -> Employer -> ID + Application Status
     */
    long countByJobEmployerIdAndStatus(Long employerId, ApplicationStatus status);
}