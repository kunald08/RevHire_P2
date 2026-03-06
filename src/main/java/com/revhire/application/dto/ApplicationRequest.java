package com.revhire.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequest {
    
    @NotNull(message = "Job ID is required")
    private Long jobId;
    
    private Long resumeId;
    
    @Size(max = 5000, message = "Cover letter cannot exceed 5000 characters")
    private String coverLetter;
    
    private MultipartFile newResume;
}