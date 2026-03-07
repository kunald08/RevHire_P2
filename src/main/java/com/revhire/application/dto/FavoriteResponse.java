package com.revhire.application.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
public class FavoriteResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String location;
    private String jobType;
    private LocalDateTime savedAt;
    
    public String getFormattedSavedDate() {
        if (savedAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return savedAt.format(formatter);
    }
    
    public String getFormattedJobType() {
        if (jobType == null) return "Not Specified";
        switch (jobType) {
            case "FULL_TIME": return "Full Time";
            case "PART_TIME": return "Part Time";
            case "CONTRACT": return "Contract";
            case "INTERNSHIP": return "Internship";
            case "REMOTE": return "Remote";
            default: return jobType;
        }
    }
}