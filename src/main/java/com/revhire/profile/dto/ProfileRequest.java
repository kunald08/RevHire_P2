package com.revhire.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating a Job Seeker profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {

    @Size(max = 200, message = "Headline must be at most 200 characters")
    private String headline;

    private String summary;

    @Size(max = 50, message = "Employment status must be at most 50 characters")
    private String currentEmploymentStatus;

    private String profilePictureUrl;
}
