package com.revhire.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO carrying analytics for a single job posting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /** Conversion rate = (totalApplications / viewCount) * 100 */
    public double getConversionRate() {
        if (viewCount == null || viewCount == 0) return 0.0;
        return Math.round((totalApplications * 100.0 / viewCount) * 10.0) / 10.0;
    }
}