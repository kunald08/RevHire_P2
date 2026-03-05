package com.revhire.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for experience CRUD operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceDto {

    private Long id;

    @NotBlank(message = "Company name is required")
    @Size(max = 200)
    private String company;

    @Size(max = 100)
    private String title;

    @Size(max = 100)
    private String location;

    private LocalDate startDate;
    private LocalDate endDate;

    private String description;

    private Boolean isCurrent;
}
