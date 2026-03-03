package com.revhire.job.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobStatsResponse {

    private Long jobId;
    private String jobTitle;

    private Long totalApplications;
    private Long appliedCount;
    private Long underReviewCount;
    private Long shortlistedCount;
    private Long rejectedCount;
    private Long withdrawnCount;

    private String jobStatus;
    private Long viewCount;
}