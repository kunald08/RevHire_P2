package com.revhire.employer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployerResponse {

    private Long id;
    private String companyName;
    private String industry;
    private String companySize;
    private String description;
    private String website;
    private String location;
}