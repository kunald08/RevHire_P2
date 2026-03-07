package com.revhire.application.repository;

import com.revhire.application.entity.Application;
import com.revhire.common.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Find by seeker with pagination
    Page<Application> findBySeekerId(Long seekerId, Pageable pageable);
    
    // Check if already applied
    boolean existsByJobIdAndSeekerId(Long jobId, Long seekerId);
    
    // Find by ID and seeker (for authorization)
    Optional<Application> findByIdAndSeekerId(Long id, Long seekerId);
    
    // Count by job
    long countByJobId(Long jobId);
    long countByJobIdAndStatus(Long jobId, ApplicationStatus status);
    // Find by job and seeker
    Optional<Application> findByJobIdAndSeekerId(Long jobId, Long seekerId);
}