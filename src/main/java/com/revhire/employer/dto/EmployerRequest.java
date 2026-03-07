package com.revhire.employer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating employer (company) profile.
 * All required fields carry Bean Validation annotations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployerRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 200, message = "Company name must be 2–200 characters")
    private String companyName;

    @NotBlank(message = "Industry is required")
    @Size(max = 100, message = "Industry cannot exceed 100 characters")
    private String industry;

    @NotBlank(message = "Company size is required")
    private String companySize;

    @NotBlank(message = "Company description is required")
    @Size(min = 10, max = 5000, message = "Description must be 10–5000 characters")
    private String description;

    @Size(max = 500, message = "Website URL is too long")
    @Pattern(
        regexp = "^$|^(https?://)?(www\\.)?.+\\..+.*$",
        message = "Please enter a valid website URL"
    )
    private String website;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location cannot exceed 200 characters")
    private String location;
}