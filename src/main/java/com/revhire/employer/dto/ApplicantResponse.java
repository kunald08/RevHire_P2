package com.revhire.employer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ApplicantResponse {

    private Long jobId;
    private String jobTitle;
    private String status;
    private LocalDate deadline;
    private int totalApplicants;
}