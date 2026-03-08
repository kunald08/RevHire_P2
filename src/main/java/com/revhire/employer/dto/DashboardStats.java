package com.revhire.employer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStats {

    private String companyName;
    private long totalJobs;
    private long activeJobs;
    private long totalApplications;
    private long pendingReviews;
}