package com.revhire.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    // ── Extra fields used by the stats template ──
    private Integer numOpenings;
    private LocalDate deadline;

    // ── Template-friendly aliases ──
    public Long getTotalViews()      { return viewCount != null ? viewCount : 0L; }
    public Long getPendingApps()     { return appliedCount != null ? appliedCount : 0L; }
    public Long getReviewedApps()    { return underReviewCount != null ? underReviewCount : 0L; }
    public Long getShortlistedApps() { return shortlistedCount != null ? shortlistedCount : 0L; }
    public Long getRejectedApps()    { return rejectedCount != null ? rejectedCount : 0L; }

    /** Conversion rate = (totalApplications / viewCount) * 100 */
    public double getConversionRate() {
        if (viewCount == null || viewCount == 0) return 0.0;
        return Math.round((totalApplications * 100.0 / viewCount) * 10.0) / 10.0;
    }
}