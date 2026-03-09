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
        
        Page<Favorite> favoritesPage = favoriteRepository.findBySeekerId(seekerId, pageable);
        log.info("Found {} favorites for seeker: {}", favoritesPage.getTotalElements(), seekerId);
        
        return favoritesPage.map(this::convertToResponse);
    }
    
    @Override
    public boolean isJobFavorited(Long seekerId, Long jobId) {
        return favoriteRepository.existsBySeekerIdAndJobId(seekerId, jobId);
    }
    
    private FavoriteResponse convertToResponse(Favorite favorite) {
        Job job = favorite.getJob();
        
        String requiredSkills = job.getRequiredSkills();
        if (requiredSkills == null) {
            requiredSkills = "";
        }
        
        return FavoriteResponse.builder()
            .id(favorite.getId())
            .jobId(job.getId())
            .jobTitle(job.getTitle() != null ? job.getTitle() : "Untitled")
            .companyName(job.getEmployer() != null && job.getEmployer().getCompanyName() != null ? 
                job.getEmployer().getCompanyName() : "Unknown Company")
            .location(job.getLocation() != null ? job.getLocation() : "Location not specified")
            .jobType(job.getJobType() != null ? job.getJobType().toString() : null)
            .industry(job.getEmployer() != null ? job.getEmployer().getIndustry() : null)
            .salaryMin(job.getSalaryMin())
            .salaryMax(job.getSalaryMax())
            .experienceMin(job.getExperienceMin())
            .experienceMax(job.getExperienceMax())
            .requiredSkills(requiredSkills)
            .deadline(job.getDeadline())
            .savedAt(favorite.getSavedAt())
            .build();
    }
}