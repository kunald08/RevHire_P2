package com.revhire.application.service;

import com.revhire.application.dto.FavoriteResponse;
import com.revhire.application.entity.Favorite;
import com.revhire.application.repository.FavoriteRepository;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.exception.BadRequestException;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class FavoriteServiceImpl implements FavoriteService {
    
    private final FavoriteRepository favoriteRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public void saveJob(Long seekerId, Long jobId) {
        log.info("Saving job {} to favorites for seeker {}", jobId, seekerId);
        
        if (favoriteRepository.existsBySeekerIdAndJobId(seekerId, jobId)) {
            throw new BadRequestException("Job already in favorites");
        }
        
        User seeker = userRepository.findById(seekerId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", seekerId));
        
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));
        
        Favorite favorite = Favorite.builder()
            .seeker(seeker)
            .job(job)
            .build();
        
        favoriteRepository.save(favorite);
        log.info("Job saved to favorites successfully");
    }
    
    @Override
    @Transactional
    public void removeJob(Long seekerId, Long jobId) {
        log.info("Removing job {} from favorites for seeker {}", jobId, seekerId);
        
        if (!favoriteRepository.existsBySeekerIdAndJobId(seekerId, jobId)) {
            throw new ResourceNotFoundException("Favorite not found");
        }
        
        favoriteRepository.deleteBySeekerIdAndJobId(seekerId, jobId);
        log.info("Job removed from favorites successfully");
    }
    
    @Override
    public Page<FavoriteResponse> getMyFavorites(Long seekerId, Pageable pageable) {
        log.info("Fetching favorites for seeker: {}", seekerId);
        
        if (!userRepository.existsById(seekerId)) {
            throw new ResourceNotFoundException("User", "id", seekerId);
        }
        
        return favoriteRepository.findBySeekerId(seekerId, pageable)
            .map(this::convertToResponse);
    }
    
    @Override
    public boolean isJobFavorited(Long seekerId, Long jobId) {
        return favoriteRepository.existsBySeekerIdAndJobId(seekerId, jobId);
    }
    
    private FavoriteResponse convertToResponse(Favorite favorite) {
        return FavoriteResponse.builder()
            .id(favorite.getId())
            .jobId(favorite.getJob().getId())
            .jobTitle(favorite.getJob().getTitle())
            .companyName(favorite.getJob().getEmployer().getCompanyName())
            .location(favorite.getJob().getLocation())
            .jobType(favorite.getJob().getJobType() != null ? favorite.getJob().getJobType().toString() : null)
            .savedAt(favorite.getSavedAt())
            .build();
    }
}