package com.revhire.profile.repository;

import com.revhire.profile.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Experience entity.
 */
@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    /**
     * Find all experiences for a given profile.
     */
    List<Experience> findByProfileId(Long profileId);
}
