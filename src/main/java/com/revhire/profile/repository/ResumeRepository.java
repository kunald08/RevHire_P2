package com.revhire.profile.repository;

import com.revhire.profile.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Resume entity.
 */
@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    /**
     * Find all resumes for a given profile.
     */
    List<Resume> findByProfileId(Long profileId);

    /**
     * Find the latest resume for a profile (by creation date descending).
     */
    Optional<Resume> findTopByProfileIdOrderByCreatedAtDesc(Long profileId);
}
