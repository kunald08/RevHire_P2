package com.revhire.job.dto;

import com.revhire.common.enums.JobType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class JobSearchFilter {
    
    private String keyword;
    private String location;
    private Integer minExperience;
    private Integer maxExperience;
    private String company;
    private Double minSalary;
    private Double maxSalary;
    private JobType jobType;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate datePostedAfter;
    
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }
    
    public boolean hasLocation() {
        return location != null && !location.trim().isEmpty();
    }
    
    public boolean hasCompany() {
        return company != null && !company.trim().isEmpty();
    }
}