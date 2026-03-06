package com.revhire.profile.service;

import com.revhire.profile.dto.*;
import com.revhire.profile.entity.JobSeekerProfile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for Job Seeker profile operations.
 */
public interface ProfileService {

    /**
     * Get or create profile for the current user.
     */
    JobSeekerProfile getOrCreateProfile(String email);

    /**
     * Get profile by user email and convert to response DTO.
     */
    ProfileResponse getProfileByEmail(String email);

    /**
     * Get profile by user ID and convert to response DTO (public view for employers).
     */
    ProfileResponse getProfileByUserId(Long userId);

    /**
     * Save or update the profile basic info (headline, summary, employment status).
     */
    void saveProfile(String email, ProfileRequest request);

    /**
     * Add education entry to a profile.
     */
    void addEducation(String email, EducationDto dto);

    /**
     * Update an existing education entry.
     */
    void updateEducation(String email, Long educationId, EducationDto dto);

    /**
     * Delete an education entry.
     */
    void deleteEducation(String email, Long educationId);

    /**
     * Add experience entry to a profile.
     */
    void addExperience(String email, ExperienceDto dto);

    /**
     * Update an existing experience entry.
     */
    void updateExperience(String email, Long experienceId, ExperienceDto dto);

    /**
     * Delete an experience entry.
     */
    void deleteExperience(String email, Long experienceId);

    /**
     * Add a skill to the profile.
     */
    void addSkill(String email, SkillDto dto);

    /**
     * Delete a skill from the profile.
     */
    void deleteSkill(String email, Long skillId);

    /**
     * Add a certification to the profile.
     */
    void addCertification(String email, CertificationDto dto);

    /**
     * Delete a certification from the profile.
     */
    void deleteCertification(String email, Long certificationId);

    /**
     * Upload profile photo and return the URL path.
     */
    String uploadProfilePhoto(String email, MultipartFile file);

    /**
     * Remove the current profile photo.
     */
    void removeProfilePhoto(String email);
}
