package com.revhire.profile.service;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.exception.UnauthorizedException;
import com.revhire.profile.dto.*;
import com.revhire.profile.entity.*;
import com.revhire.profile.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ProfileService — handles all profile CRUD operations.
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final Logger logger = LogManager.getLogger(ProfileServiceImpl.class);

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final EducationRepository educationRepository;
    private final ExperienceRepository experienceRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;

    // ==================== Profile ====================

    @Override
    @Transactional
    public JobSeekerProfile getOrCreateProfile(String email) {
        logger.info("Getting or creating profile for user: {}", email);
        User user = findUserByEmail(email);

        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    logger.info("Creating new profile for user: {}", email);
                    JobSeekerProfile profile = JobSeekerProfile.builder()
                            .user(user)
                            .build();
                    return profileRepository.save(profile);
                });
    }

    @Override
    public ProfileResponse getProfileByEmail(String email) {
        logger.info("Fetching profile for email: {}", email);
        JobSeekerProfile profile = getOrCreateProfile(email);
        return mapToResponse(profile);
    }

    @Override
    public ProfileResponse getProfileByUserId(Long userId) {
        logger.info("Fetching public profile for user ID: {}", userId);
        JobSeekerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "userId", userId));
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public void saveProfile(String email, ProfileRequest request) {
        logger.info("Saving profile for user: {}", email);
        JobSeekerProfile profile = getOrCreateProfile(email);

        profile.setHeadline(request.getHeadline());
        profile.setSummary(request.getSummary());
        profile.setCurrentEmploymentStatus(request.getCurrentEmploymentStatus());
        profile.setProfilePictureUrl(request.getProfilePictureUrl());

        profileRepository.save(profile);
        logger.info("Profile saved successfully for user: {}", email);
    }

    // ==================== Education ====================

    @Override
    @Transactional
    public void addEducation(String email, EducationDto dto) {
        logger.info("Adding education for user: {}", email);
        JobSeekerProfile profile = getOrCreateProfile(email);

        Education education = Education.builder()
                .profile(profile)
                .institution(dto.getInstitution())
                .degree(dto.getDegree())
                .fieldOfStudy(dto.getFieldOfStudy())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .grade(dto.getGrade())
                .build();

        educationRepository.save(education);
        logger.info("Education added successfully: {}", dto.getInstitution());
    }

    @Override
    @Transactional
    public void updateEducation(String email, Long educationId, EducationDto dto) {
        logger.info("Updating education ID: {} for user: {}", educationId, email);
        JobSeekerProfile profile = getOrCreateProfile(email);
        Education education = findEducationAndVerifyOwnership(educationId, profile.getId());

        education.setInstitution(dto.getInstitution());
        education.setDegree(dto.getDegree());
        education.setFieldOfStudy(dto.getFieldOfStudy());
        education.setStartDate(dto.getStartDate());
        education.setEndDate(dto.getEndDate());
        education.setGrade(dto.getGrade());

        educationRepository.save(education);
        logger.info("Education updated successfully: ID {}", educationId);
    }

    @Override
    @Transactional
    public void deleteEducation(String email, Long educationId) {
        logger.info("Deleting education ID: {} for user: {}", educationId, email);
        JobSeekerProfile profile = getOrCreateProfile(email);
        Education education = findEducationAndVerifyOwnership(educationId, profile.getId());
        educationRepository.delete(education);
        logger.info("Education deleted successfully: ID {}", educationId);
    }

    // ==================== Experience ====================

    @Override
    @Transactional
    public void addExperience(String email, ExperienceDto dto) {
        logger.info("Adding experience for user: {}", email);
        JobSeekerProfile profile = getOrCreateProfile(email);

        Experience experience = Experience.builder()
                .profile(profile)
                .company(dto.getCompany())
                .title(dto.getTitle())
                .location(dto.getLocation())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .description(dto.getDescription())
                .isCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false)
                .build();

        experienceRepository.save(experience);
        logger.info("Experience added successfully: {}", dto.getCompany());
    }

    @Override
    @Transactional
    public void updateExperience(String email, Long experienceId, ExperienceDto dto) {
        logger.info("Updating experience ID: {} for user: {}", experienceId, email);
        JobSeekerProfile profile = getOrCreateProfile(email);
        Experience experience = findExperienceAndVerifyOwnership(experienceId, profile.getId());

        experience.setCompany(dto.getCompany());
        experience.setTitle(dto.getTitle());
        experience.setLocation(dto.getLocation());
        experience.setStartDate(dto.getStartDate());
        experience.setEndDate(dto.getEndDate());
        experience.setDescription(dto.getDescription());
        experience.setIsCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false);

        experienceRepository.save(experience);
        logger.info("Experience updated successfully: ID {}", experienceId);
    }

    @Override
    @Transactional
    public void deleteExperience(String email, Long experienceId) {
        logger.info("Deleting experience ID: {} for user: {}", experienceId, email);
        JobSeekerProfile profile = getOrCreateProfile(email);
        Experience experience = findExperienceAndVerifyOwnership(experienceId, profile.getId());
        experienceRepository.delete(experience);
        logger.info("Experience deleted successfully: ID {}", experienceId);
    }

    // ==================== Skills ====================

    @Override
    @Transactional
    public void addSkill(String email, SkillDto dto) {
        logger.info("Adding skill for user: {}", email);
        JobSeekerProfile profile = getOrCreateProfile(email);

        Skill skill = Skill.builder()
                .profile(profile)
                .name(dto.getName())
                .proficiency(dto.getProficiency())
                .build();

        skillRepository.save(skill);
        logger.info("Skill added successfully: {}", dto.getName());
    }

    @Override
    @Transactional
    public void deleteSkill(String email, Long skillId) {
        logger.info("Deleting skill ID: {} for user: {}", skillId, email);
        JobSeekerProfile profile = getOrCreateProfile(email);
        Skill skill = findSkillAndVerifyOwnership(skillId, profile.getId());
        skillRepository.delete(skill);
        logger.info("Skill deleted successfully: ID {}", skillId);
    }

    // ==================== Certifications ====================

    @Override
    @Transactional
    public void addCertification(String email, CertificationDto dto) {
        logger.info("Adding certification for user: {}", email);
        JobSeekerProfile profile = getOrCreateProfile(email);

        Certification certification = Certification.builder()
                .profile(profile)
                .name(dto.getName())
                .issuingOrg(dto.getIssuingOrg())
                .issueDate(dto.getIssueDate())
                .expiryDate(dto.getExpiryDate())
                .credentialUrl(dto.getCredentialUrl())
                .build();

        certificationRepository.save(certification);
        logger.info("Certification added successfully: {}", dto.getName());
    }

    @Override
    @Transactional
    public void deleteCertification(String email, Long certificationId) {
        logger.info("Deleting certification ID: {} for user: {}", certificationId, email);
        JobSeekerProfile profile = getOrCreateProfile(email);
        Certification cert = findCertificationAndVerifyOwnership(certificationId, profile.getId());
        certificationRepository.delete(cert);
        logger.info("Certification deleted successfully: ID {}", certificationId);
    }

    // ==================== Helper Methods ====================

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Education findEducationAndVerifyOwnership(Long educationId, Long profileId) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", educationId));
        if (!education.getProfile().getId().equals(profileId)) {
            throw new UnauthorizedException("You do not own this education entry");
        }
        return education;
    }

    private Experience findExperienceAndVerifyOwnership(Long experienceId, Long profileId) {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", "id", experienceId));
        if (!experience.getProfile().getId().equals(profileId)) {
            throw new UnauthorizedException("You do not own this experience entry");
        }
        return experience;
    }

    private Skill findSkillAndVerifyOwnership(Long skillId, Long profileId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", skillId));
        if (!skill.getProfile().getId().equals(profileId)) {
            throw new UnauthorizedException("You do not own this skill entry");
        }
        return skill;
    }

    private Certification findCertificationAndVerifyOwnership(Long certificationId, Long profileId) {
        Certification cert = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Certification", "id", certificationId));
        if (!cert.getProfile().getId().equals(profileId)) {
            throw new UnauthorizedException("You do not own this certification entry");
        }
        return cert;
    }

    /**
     * Maps a JobSeekerProfile entity to a ProfileResponse DTO.
     */
    private ProfileResponse mapToResponse(JobSeekerProfile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .userName(profile.getUser().getName())
                .userEmail(profile.getUser().getEmail())
                .userPhone(profile.getUser().getPhone())
                .userLocation(profile.getUser().getLocation())
                .headline(profile.getHeadline())
                .summary(profile.getSummary())
                .currentEmploymentStatus(profile.getCurrentEmploymentStatus())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .educations(profile.getEducations().stream()
                        .map(e -> EducationDto.builder()
                                .id(e.getId())
                                .institution(e.getInstitution())
                                .degree(e.getDegree())
                                .fieldOfStudy(e.getFieldOfStudy())
                                .startDate(e.getStartDate())
                                .endDate(e.getEndDate())
                                .grade(e.getGrade())
                                .build())
                        .collect(Collectors.toList()))
                .experiences(profile.getExperiences().stream()
                        .map(e -> ExperienceDto.builder()
                                .id(e.getId())
                                .company(e.getCompany())
                                .title(e.getTitle())
                                .location(e.getLocation())
                                .startDate(e.getStartDate())
                                .endDate(e.getEndDate())
                                .description(e.getDescription())
                                .isCurrent(e.getIsCurrent())
                                .build())
                        .collect(Collectors.toList()))
                .skills(profile.getSkills().stream()
                        .map(s -> SkillDto.builder()
                                .id(s.getId())
                                .name(s.getName())
                                .proficiency(s.getProficiency())
                                .build())
                        .collect(Collectors.toList()))
                .certifications(profile.getCertifications().stream()
                        .map(c -> CertificationDto.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .issuingOrg(c.getIssuingOrg())
                                .issueDate(c.getIssueDate())
                                .expiryDate(c.getExpiryDate())
                                .credentialUrl(c.getCredentialUrl())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    // ==================== Photo Upload ====================

    @Override
    @Transactional
    public String uploadProfilePhoto(String email, MultipartFile file) {
        logger.info("Uploading profile photo for user: {}", email);

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please select a photo to upload.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF, and WEBP images are allowed.");
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw new IllegalArgumentException("Photo must be smaller than 5MB.");
        }

        JobSeekerProfile profile = getOrCreateProfile(email);

        // Delete old photo file if exists
        deleteOldPhoto(profile.getProfilePictureUrl());

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = "photo_" + profile.getUser().getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        try {
            Path photosDir = Paths.get(uploadDir, "photos");
            Files.createDirectories(photosDir);
            Path targetPath = photosDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to save profile photo: {}", e.getMessage());
            throw new RuntimeException("Failed to save profile photo. Please try again.");
        }

        String photoUrl = "/uploads/photos/" + filename;
        profile.setProfilePictureUrl(photoUrl);
        profileRepository.save(profile);

        logger.info("Profile photo uploaded successfully for user: {}", email);
        return photoUrl;
    }

    @Override
    @Transactional
    public void removeProfilePhoto(String email) {
        logger.info("Removing profile photo for user: {}", email);
        JobSeekerProfile profile = getOrCreateProfile(email);

        deleteOldPhoto(profile.getProfilePictureUrl());
        profile.setProfilePictureUrl(null);
        profileRepository.save(profile);

        logger.info("Profile photo removed for user: {}", email);
    }

    private void deleteOldPhoto(String photoUrl) {
        if (photoUrl != null && photoUrl.startsWith("/uploads/photos/")) {
            try {
                String filename = photoUrl.substring("/uploads/photos/".length());
                Path oldFile = Paths.get(uploadDir, "photos", filename);
                Files.deleteIfExists(oldFile);
            } catch (IOException e) {
                logger.warn("Could not delete old profile photo: {}", e.getMessage());
            }
        }
    }
}
