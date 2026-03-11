package com.revhire.profile.repository;

import com.revhire.profile.entity.JobSeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for JobSeekerProfile entity.
 */
@Repository
public interface ProfileRepository extends JpaRepository<JobSeekerProfile, Long> {

    /**
     * Find profile by user ID.
     */
    Optional<JobSeekerProfile> findByUserId(Long userId);

    /**
     * Find profile by user email.
     */
    Optional<JobSeekerProfile> findByUserEmail(String email);

    /**
     * Check if a profile exists for a given user.
     */
    boolean existsByUserId(Long userId);
}
