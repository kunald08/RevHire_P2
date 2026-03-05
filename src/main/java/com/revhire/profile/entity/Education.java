package com.revhire.profile.entity;

import jakarta.persistence.*;

@Entity
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String degree;
    private String institution;
    private String startYear;
    private String endYear;
    private String yearOfPassing; 
    @Column(length = 1000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private JobSeekerProfile profile;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getStartYear() {
        return startYear;
    }

    public void setStartYear(String startYear) {
        this.startYear = startYear;
    }

    public String getEndYear() {
        return endYear;
    }

    public void setEndYear(String endYear) {
        this.endYear = endYear;
    }
    public String getYearOfPassing() {   // ✅ MUST EXIST
        return yearOfPassing;
    }

    public void setYearOfPassing(String yearOfPassing) {   // ✅ MUST EXIST
        this.yearOfPassing = yearOfPassing;
    }
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JobSeekerProfile getProfile() {
        return profile;
    }

    public void setProfile(JobSeekerProfile profile) {
        this.profile = profile;
    }
}
