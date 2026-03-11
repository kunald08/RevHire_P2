package com.revhire.job.dto;

import com.revhire.common.enums.JobType;
import com.revhire.job.validation.ValidSalaryRange;
import com.revhire.job.validation.ValidExperienceRange;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@ValidSalaryRange
@ValidExperienceRange
public class JobRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @NotBlank(message = "Job description is required")
    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Minimum salary is required")
    @DecimalMin(value = "0.0", message = "Minimum salary cannot be negative")
    private BigDecimal salaryMin;

    @NotNull(message = "Maximum salary is required")
    @DecimalMin(value = "0.0", message = "Maximum salary cannot be negative")
    private BigDecimal salaryMax;

    @NotNull(message = "Minimum experience is required")
    @Min(value = 0, message = "Minimum experience cannot be negative")
    private Integer experienceMin;

    @NotNull(message = "Maximum experience is required")
    @Min(value = 0, message = "Maximum experience cannot be negative")
    private Integer experienceMax;

    @NotNull(message = "Job type is required")
    private JobType jobType;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    @NotNull(message = "Number of openings is required")
    @Min(value = 1, message = "There must be at least 1 opening")
    private Integer numOpenings;

    @NotBlank(message = "Required skills is required")
    private String requiredSkills;

    @NotBlank(message = "Education requirement is required")
    private String educationReq;
}