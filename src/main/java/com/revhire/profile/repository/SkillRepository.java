package com.revhire.profile.repository;

import com.revhire.profile.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Skill entity.
 */
@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    /**
     * Find all skills for a given profile.
     */
    List<Skill> findByProfileId(Long profileId);
}
