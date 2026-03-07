package com.revhire.application.service;

import com.revhire.application.dto.FavoriteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteService {
    
    /**
     * Save a job to user's favorites
     * @param seekerId the ID of the job seeker
     * @param jobId the ID of the job to save
     */
    void saveJob(Long seekerId, Long jobId);
    
    /**
     * Remove a job from user's favorites
     * @param seekerId the ID of the job seeker
     * @param jobId the ID of the job to remove
     */
    void removeJob(Long seekerId, Long jobId);
    
    /**
     * Get all favorites for a user with pagination
     * @param seekerId the ID of the job seeker
     * @param pageable pagination information
     * @return paginated list of favorites
     */
    Page<FavoriteResponse> getMyFavorites(Long seekerId, Pageable pageable);
    
    /**
     * Check if a job is in user's favorites
     * @param seekerId the ID of the job seeker
     * @param jobId the ID of the job
     * @return true if job is favorited, false otherwise
     */
    boolean isJobFavorited(Long seekerId, Long jobId);
}