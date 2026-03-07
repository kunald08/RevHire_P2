package com.revhire.job.repository;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.common.enums.JobStatus;
import com.revhire.employer.entity.Employer;
import com.revhire.job.entity.Job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

	// OLD method (for dashboard)
    List<Job> findByEmployer(Employer employer);

    long countByEmployerId(Long employerId);

    long countByEmployerIdAndStatus(Long employerId, JobStatus status);
    List<Job> findByEmployer_User_EmailAndStatus(String email, JobStatus status);
    @Query("SELECT DISTINCT j FROM Job j JOIN j.applications a " +
    	       "WHERE j.employer.user.email = :email " +
    	       "AND a.status IN :statuses " +
    	       "AND (:keyword IS NULL OR :keyword = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    	Page<Job> findPendingJobsByEmailAndStatusAndTitle(
    	        @Param("email") String email, 
    	        @Param("statuses") List<ApplicationStatus> statuses, 
    	        @Param("keyword") String keyword, 
    	        Pageable pageable);
 // Add this to JobRepository.java
    Page<Job> findByEmployerUserEmailAndTitleContainingIgnoreCase(
            String email, 
            String title, 
            Pageable pageable);

    // NEW method (for pagination)
    Page<Job> findByEmployer(Employer employer, Pageable pageable);

}


