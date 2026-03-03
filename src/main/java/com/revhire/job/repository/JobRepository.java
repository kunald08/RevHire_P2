package com.revhire.job.repository;

import com.revhire.employer.entity.Employer;
import com.revhire.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByEmployer(Employer employer);
}