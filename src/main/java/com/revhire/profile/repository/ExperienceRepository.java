package com.revhire.profile.repository;

import com.revhire.profile.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Experience entity.
 */
@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    /**
     * Find all experiences for a given profile.
     */
    List<Experience> findByProfileId(Long profileId);
    
    /**
     * To calculate the experience years of applicant for filtering purpose of employer dashboard.
     */
    @Query("SELECT SUM(TIMESTAMPDIFF(MONTH, e.startDate, COALESCE(e.endDate, CURRENT_DATE))) " +
    	       "FROM Experience e WHERE e.profile.id = :profileId")
    	Optional<Integer> calculateTotalMonthsByProfileId(@Param("profileId") Long profileId);
}
