package com.revhire.application.dto;

import com.revhire.common.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Data
@Builder
public class ApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String jobLocation;
    private String jobType;
    // Extra job details
    private String jobDescription;
    private String requiredSkills;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Integer experienceMin;
    private Integer experienceMax;
    private String educationReq;
    private LocalDate deadline;
    private Integer numOpenings;
    // Seeker details
    private String seekerName;
    private String seekerEmail;
    private String resumeFileName;
    private Long resumeId;
    private String coverLetter;
    private ApplicationStatus status;
    private String employerComment;
    private String withdrawReason;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    public String getStatusBadgeClass() {
        if (status == null) return "badge bg-light";
        switch (status) {
            case APPLIED: return "badge bg-primary";
            case UNDER_REVIEW: return "badge bg-info text-dark";
            case SHORTLISTED: return "badge bg-success";
            case REJECTED: return "badge bg-danger";
            case WITHDRAWN: return "badge bg-secondary";
            default: return "badge bg-light";
        }
    }

    public String getStatusIcon() {
        if (status == null) return "bi-question-circle";
        switch (status) {
            case APPLIED: return "bi-send-check";
            case UNDER_REVIEW: return "bi-hourglass-split";
            case SHORTLISTED: return "bi-star-fill";
            case REJECTED: return "bi-x-circle";
            case WITHDRAWN: return "bi-arrow-return-left";
            default: return "bi-question-circle";
        }
    }

    public String getStatusColor() {
        if (status == null) return "#6c757d";
        switch (status) {
            case APPLIED: return "#0d6efd";
            case UNDER_REVIEW: return "#0dcaf0";
            case SHORTLISTED: return "#198754";
            case REJECTED: return "#dc3545";
            case WITHDRAWN: return "#6c757d";
            default: return "#6c757d";
        }
    }

    public String getFormattedAppliedDate() {
        if (appliedAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return appliedAt.format(formatter);
    }

    public String getFormattedUpdatedDate() {
        if (updatedAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        return updatedAt.format(formatter);
    }

    public String getTimeAgo() {
        if (appliedAt == null) return "";
        long days = ChronoUnit.DAYS.between(appliedAt, LocalDateTime.now());
        if (days == 0) return "Today";
        if (days == 1) return "Yesterday";
        if (days < 7) return days + " days ago";
        if (days < 30) return (days / 7) + " weeks ago";
        return (days / 30) + " months ago";
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

    public boolean isWithdrawable() {
        return status != ApplicationStatus.WITHDRAWN &&
               status != ApplicationStatus.SHORTLISTED &&
               status != ApplicationStatus.REJECTED;
    }

    public String getFormattedSalary() {
        if (salaryMin == null && salaryMax == null) return null;
        java.text.NumberFormat fmt = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US);
        fmt.setMaximumFractionDigits(0);
        if (salaryMin != null && salaryMax != null) {
            return fmt.format(salaryMin) + " – " + fmt.format(salaryMax);
        }
        if (salaryMin != null) return "From " + fmt.format(salaryMin);
        return "Up to " + fmt.format(salaryMax);
    }

    public String getFormattedExperience() {
        if (experienceMin == null && experienceMax == null) return null;
        if (experienceMin != null && experienceMax != null) {
            if (experienceMin.equals(experienceMax)) return experienceMin + " years";
            return experienceMin + " – " + experienceMax + " years";
        }
        if (experienceMin != null) return experienceMin + "+ years";
        return "Up to " + experienceMax + " years";
    }

    public String getFormattedDeadline() {
        if (deadline == null) return null;
        return deadline.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
}