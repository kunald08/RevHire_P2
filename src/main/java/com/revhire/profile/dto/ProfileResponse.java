package com.revhire.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for returning complete profile information including
 * education, experience, skills, certifications, and resumes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String userLocation;
    private String headline;
    private String summary;
    private String currentEmploymentStatus;
    private String profilePictureUrl;

    private List<EducationDto> educations;
    private List<ExperienceDto> experiences;
    private List<SkillDto> skills;
    private List<CertificationDto> certifications;
}
