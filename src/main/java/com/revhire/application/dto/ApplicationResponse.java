package com.revhire.application.dto;

import com.revhire.common.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
public class ApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;  // Add this field
    private String seekerName;
    private String seekerEmail;
    private String resumeFileName;
    private String coverLetter;
    private ApplicationStatus status;
    private String employerComment;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    
    public String getStatusBadgeClass() {
        switch (status) {
            case APPLIED: return "badge bg-primary";
            case UNDER_REVIEW: return "badge bg-info";
            case SHORTLISTED: return "badge bg-success";
            case REJECTED: return "badge bg-danger";
            case WITHDRAWN: return "badge bg-secondary";
            default: return "badge bg-light";
        }
    }
    
    public String getFormattedAppliedDate() {
        if (appliedAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return appliedAt.format(formatter);
    }
    
    public boolean isWithdrawable() {
        return status != ApplicationStatus.WITHDRAWN && 
               status != ApplicationStatus.SHORTLISTED && 
               status != ApplicationStatus.REJECTED;
    }
}