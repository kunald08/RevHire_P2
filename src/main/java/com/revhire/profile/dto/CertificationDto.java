package com.revhire.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for certification CRUD operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificationDto {

    private Long id;

    @NotBlank(message = "Certification name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 200)
    private String issuingOrg;

    private LocalDate issueDate;
    private LocalDate expiryDate;

    @Size(max = 500)
    private String credentialUrl;
}
