package com.revhire.employer.dto;

import lombok.Data;

@Data
public class FilterApplicantDTO {

    private String name;
    private String status;
    private String education;
    private String certification;
    private Integer minExperience;
    private Integer maxExperience;
    private String skills;
    private String notes;
}