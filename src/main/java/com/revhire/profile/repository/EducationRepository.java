package com.revhire.profile.repository;

import com.revhire.profile.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Education entity.
 */
@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {

    /**
     * Find all educations for a given profile.
     */
    List<Education> findByProfileId(Long profileId);
}
