package com.revhire.application.repository;

import com.revhire.application.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    // Find by seeker
    Page<Favorite> findBySeekerId(Long seekerId, Pageable pageable);
    List<Favorite> findBySeekerId(Long seekerId);
    List<Favorite> findBySeekerIdOrderBySavedAtDesc(Long seekerId);
    
    // Find by job
    List<Favorite> findByJobId(Long jobId);
    
    // Check if exists
    boolean existsBySeekerIdAndJobId(Long seekerId, Long jobId);
    Optional<Favorite> findBySeekerIdAndJobId(Long seekerId, Long jobId);
    
    // Count methods
    long countByJobId(Long jobId);
    long countBySeekerId(Long seekerId);
    
    // Delete methods
    @Modifying
    @Transactional
    void deleteBySeekerIdAndJobId(Long seekerId, Long jobId);
      
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.job.id = :jobId")
    void deleteByJobId(Long jobId);
}