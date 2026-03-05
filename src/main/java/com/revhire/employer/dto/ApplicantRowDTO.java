package com.revhire.employer.dto;

import java.time.LocalDateTime;

public class ApplicantRowDTO {

    private Long applicationId;
    private String applicantName;
    private String status;
    private LocalDateTime appliedDate;
    private String employerComment;
    private String reviewerNote;

    public ApplicantRowDTO(Long applicationId,
                            String applicantName,
                            String status,
                            LocalDateTime appliedDate,
                            String employerComment,
                            String reviewerNote) {
        this.applicationId = applicationId;
        this.applicantName = applicantName;
        this.status = status;
        this.appliedDate = appliedDate;
        this.employerComment = employerComment;
        this.reviewerNote = reviewerNote;
    }

    public Long getApplicationId() { return applicationId; }
    public String getApplicantName() { return applicantName; }
    public String getStatus() { return status; }
    public LocalDateTime getAppliedDate() { return appliedDate; }
    public String getEmployerComment() { return employerComment; }
    public String getReviewerNote() { return reviewerNote; }
}