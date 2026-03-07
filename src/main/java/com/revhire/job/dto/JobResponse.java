package com.revhire.job.dto;

import com.revhire.common.enums.JobStatus;
import com.revhire.common.enums.JobType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class JobResponse {

    private Long id;

    private String title;
    private String description;
    private String location;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    private Integer experienceMin;
    private Integer experienceMax;

    private String requiredSkills;
    private String educationReq;

    private Integer numOpenings;

    private JobType jobType;
    private JobStatus status;

    private LocalDate deadline;

    private Long viewCount;
    private String companyName;
}