package com.revhire.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for skill CRUD operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillDto {

    private Long id;

    @NotBlank(message = "Skill name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String proficiency;
}
