package com.revhire.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Data
@Builder
public class FavoriteResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String location;
    private String jobType;
    private String industry;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Integer experienceMin;
    private Integer experienceMax;
    private String requiredSkills;
    private LocalDate deadline;
    private LocalDateTime savedAt;

    public String getFormattedSavedDate() {
        if (savedAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return savedAt.format(formatter);
    }

    public String getFormattedJobType() {
        if (jobType == null) return "Not Specified";
        switch (jobType) {
            case "FULL_TIME": return "Full Time";
            case "PART_TIME": return "Part Time";
            case "CONTRACT": return "Contract";
            case "INTERNSHIP": return "Internship";
            case "REMOTE": return "Remote";
            default: return jobType;
        }
    }

    public String getSalaryDisplay() {
        if (salaryMin == null && salaryMax == null) return "Not disclosed";
        if (salaryMin != null && salaryMax != null) {
            return "₹" + formatIndian(salaryMin) + " – ₹" + formatIndian(salaryMax);
        }
        if (salaryMin != null) return "From ₹" + formatIndian(salaryMin);
        return "Up to ₹" + formatIndian(salaryMax);
    }

    public String getExperienceDisplay() {
        if (experienceMin == null && experienceMax == null) return "Not specified";
        if (experienceMin != null && experienceMax != null) {
            return experienceMin + " – " + experienceMax + " yrs";
        }
        if (experienceMin != null) return experienceMin + "+ yrs";
        return "Up to " + experienceMax + " yrs";
    }

    public Long getDaysRemaining() {
        if (deadline == null) return null;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        return days < 0 ? 0 : days;
    }

    private static String formatIndian(BigDecimal amount) {
        if (amount == null) return "0";
        long val = amount.longValue();
        if (val >= 10000000) return String.format("%.1fCr", val / 10000000.0);
        if (val >= 100000) return String.format("%.1fL", val / 100000.0);
        if (val >= 1000) return String.format("%.1fK", val / 1000.0);
        return String.valueOf(val);
    }
}