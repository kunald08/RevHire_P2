package com.revhire.employer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmployerRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name cannot exceed 200 characters")
    private String companyName;

    @NotBlank(message = "Industry is required")
    @Size(max = 100, message = "Industry cannot exceed 100 characters")
    private String industry;

    @NotBlank(message = "Company size is required")
    @Size(max = 50, message = "Company size cannot exceed 50 characters")
    private String companySize;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @Size(max = 500, message = "Website URL too long")
    @Pattern(
        regexp = "^(https?://)?(www\\.)?.+\\..+.*$",
        message = "Invalid website URL format"
    )
    private String website;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location cannot exceed 200 characters")
    private String location;
}