package com.revhire.employer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApplicantProfileDTO {

    private Long applicationId;

    private Long userId;
    private Long seekerId;

    private String applicantName;;

    private Long resumeId;
    
 // Profile Data
    private String headline;
    private String summary;
    private String profilePicture;

    private String coverLetter;

    private String status;

    private String employerComment;

    private LocalDateTime appliedAt;
    private boolean hasFile;

}