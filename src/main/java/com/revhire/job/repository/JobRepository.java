package com.revhire.job.repository;
import com.revhire.common.enums.JobStatus;
import com.revhire.employer.entity.Employer;
import com.revhire.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByEmployer(Employer employer);

    long countByEmployerId(Long employerId);

    long countByEmployerIdAndStatus(Long employerId, JobStatus status);
    List<Job> findByEmployer_User_EmailAndStatus(String email, JobStatus status);

}