package com.revhire.employer.repository;

import com.revhire.job.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicantRepository extends JpaRepository<Job, Long> {

    // Navigates: Job -> Employer -> User -> Email
    Page<Job> findByEmployerUserEmailAndTitleContainingIgnoreCase(
            String email, 
            String title, 
            Pageable pageable
    );
}