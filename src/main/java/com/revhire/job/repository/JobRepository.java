package com.revhire.job.repository;

import com.revhire.employer.entity.Employer;
import com.revhire.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

	// OLD method (for dashboard)
    List<Job> findByEmployer(Employer employer);

    // NEW method (for pagination)
    Page<Job> findByEmployer(Employer employer, Pageable pageable);

}







