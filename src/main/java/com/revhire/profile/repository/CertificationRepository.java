package com.revhire.profile.repository;

import com.revhire.profile.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Certification entity.
 */
@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

    /**
     * Find all certifications for a given profile.
     */
    List<Certification> findByProfileId(Long profileId);
}
