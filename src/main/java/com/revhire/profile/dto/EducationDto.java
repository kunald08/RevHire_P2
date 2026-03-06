package com.revhire.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for education CRUD operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationDto {

    private Long id;

    @NotBlank(message = "Institution name is required")
    @Size(max = 200)
    private String institution;

    @Size(max = 100)
    private String degree;

    @Size(max = 100)
    private String fieldOfStudy;

    private LocalDate startDate;
    private LocalDate endDate;

    @Size(max = 20)
    private String grade;
}
