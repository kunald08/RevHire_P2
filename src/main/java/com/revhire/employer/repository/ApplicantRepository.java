package com.revhire.employer.repository;

import com.revhire.common.enums.ApplicationStatus;
import com.revhire.job.entity.Job;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicantRepository extends JpaRepository<Job, Long> {

    // Navigates: Job -> Employer -> User -> Email
    Page<Job> findByEmployerUserEmailAndTitleContainingIgnoreCase(
            String email, 
            String title, 
            Pageable pageable
    );
    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.id = :jobId AND a.status IN :statuses")
    long countByJobIdAndStatusIn(@Param("jobId") Long jobId, @Param("statuses") List<ApplicationStatus> statuses);
    
    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.employer.id = :employerId AND a.status IN :statuses")
    long countByJobEmployerIdAndStatusIn(@Param("employerId") Long employerId, @Param("statuses") List<ApplicationStatus> statuses);
    
    /**
     * Counts applications for a specific job, excluding a specific status (like WITHDRAWN)
     */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.id = :jobId AND a.status <> :excludedStatus")
    long countByJobIdAndStatusNot(@Param("jobId") Long jobId, @Param("excludedStatus") ApplicationStatus excludedStatus);

    /**
     * Counts all active applications for an employer, excluding withdrawn ones
     */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.employer.id = :employerId AND a.status <> :excludedStatus")
    long countByEmployerIdAndStatusNot(@Param("employerId") Long employerId, @Param("excludedStatus") ApplicationStatus excludedStatus);

}