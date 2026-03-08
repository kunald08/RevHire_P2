package com.revhire.employer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for employer / company data sent to the view layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployerResponse {

    private Long id;
    private String companyName;
    private String industry;
    private String companySize;
    private String description;
    private String website;
    private String location;
    private String logoUrl;
    private LocalDateTime createdAt;
    private long totalJobs;
    private long activeJobs;
}