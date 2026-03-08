package com.revhire.job.dto;

import com.revhire.common.enums.JobStatus;
import com.revhire.common.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only DTO representing a job posting for the view layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private Long id;

    private String title;
    private String description;
    private String location;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    private Integer experienceMin;
    private Integer experienceMax;

    private String requiredSkills;
    private String educationReq;

    private Integer numOpenings;

    private JobType jobType;
    private JobStatus status;

    private LocalDate deadline;
    private Long viewCount;

    // employer info (for public views)
    private Long employerId;
    private String companyName;
    private String companyLocation;
    private String industry;

    private long applicationCount;
    private LocalDateTime createdAt;

    /** Human-readable salary display, e.g. "₹5,00,000 – ₹12,00,000" */
    public String getSalaryDisplay() {
        if (salaryMin == null && salaryMax == null) return "Not disclosed";
        if (salaryMin != null && salaryMax != null) {
            return "₹" + formatIndian(salaryMin) + " – ₹" + formatIndian(salaryMax);
        }
        if (salaryMin != null) return "From ₹" + formatIndian(salaryMin);
        return "Up to ₹" + formatIndian(salaryMax);
    }

    /** Human-readable experience display */
    public String getExperienceDisplay() {
        if (experienceMin == null && experienceMax == null) return "Not specified";
        if (experienceMin != null && experienceMax != null) {
            if (experienceMin.equals(experienceMax)) return experienceMin + " years";
            return experienceMin + " – " + experienceMax + " years";
        }
        if (experienceMin != null) return experienceMin + "+ years";
        return "Up to " + experienceMax + " years";
    }

    /** Human-readable job type */
    public String getJobTypeDisplay() {
        if (jobType == null) return "N/A";
        return switch (jobType) {
            case FULL_TIME -> "Full Time";
            case PART_TIME -> "Part Time";
            case CONTRACT -> "Contract";
            case INTERNSHIP -> "Internship";
            case REMOTE -> "Remote";
        };
    }

    /** Days remaining until deadline */
    public long getDaysRemaining() {
        if (deadline == null) return -1;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), deadline);
    }

    private static String formatIndian(BigDecimal amount) {
        if (amount == null) return "0";
        long val = amount.longValue();
        if (val >= 10000000) return String.format("%.1fCr", val / 10000000.0);
        if (val >= 100000)   return String.format("%.1fL", val / 100000.0);
        if (val >= 1000)     return String.format("%.1fK", val / 1000.0);
        return String.valueOf(val);
    }
}