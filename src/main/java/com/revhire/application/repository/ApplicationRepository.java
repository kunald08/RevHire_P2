package com.revhire.application.repository;

import com.revhire.application.entity.Application;
import com.revhire.common.enums.ApplicationStatus;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Find by seeker with pagination
    Page<Application> findBySeekerId(Long seekerId, Pageable pageable);

    // Find by seeker and status with pagination
    Page<Application> findBySeekerIdAndStatus(Long seekerId, ApplicationStatus status, Pageable pageable);

    // Count by seeker and status
    long countBySeekerIdAndStatus(Long seekerId, ApplicationStatus status);

    // Count all by seeker
    long countBySeekerId(Long seekerId);
    
    // Check if already applied
    boolean existsByJobIdAndSeekerId(Long jobId, Long seekerId);
    
    // Find by ID and seeker (for authorization)
    Optional<Application> findByIdAndSeekerId(Long id, Long seekerId);
    
    // Count by job
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
    List<Application> findByJobId(Long jobId);

    // Find by job and seeker
    Optional<Application> findByJobIdAndSeekerId(Long jobId, Long seekerId);


    // Find all applications that reference a specific resume
    List<Application> findByResumeId(Long resumeId);

    /** Batch-fetch application counts for multiple jobs in a single query. */
    @Query("SELECT a.job.id, COUNT(a) FROM Application a WHERE a.job.id IN :jobIds GROUP BY a.job.id")
    List<Object[]> countApplicationsByJobIds(@Param("jobIds") java.util.Collection<Long> jobIds);

    /** Eager-load job, employer, seeker, resume in one shot for detail view. */
    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH j.employer " +
           "LEFT JOIN FETCH a.seeker " +
           "LEFT JOIN FETCH a.resume " +
           "WHERE a.id = :id")
    Optional<Application> findByIdWithDetails(@Param("id") Long id);

    /** Eager-load for seeker's list view (no pagination — used by service to map). */
    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH j.employer " +
           "LEFT JOIN FETCH a.seeker " +
           "LEFT JOIN FETCH a.resume " +
           "WHERE a.seeker.id = :seekerId " +
           "ORDER BY a.appliedAt DESC")
    List<Application> findBySeekerIdWithDetails(@Param("seekerId") Long seekerId);

}