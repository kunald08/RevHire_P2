package com.revhire.job.entity;

import com.revhire.application.entity.Application;
import com.revhire.common.enums.JobStatus;
import com.revhire.common.enums.JobType;
import com.revhire.employer.entity.Employer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many jobs belong to one employer
    @ManyToOne
    @JoinColumn(name = "employer_id", nullable = false)
    private Employer employer;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String requiredSkills;
    private Integer experienceMin;
    private Integer experienceMax;
    private String educationReq;
    private String location;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private LocalDate deadline;
    private Integer numOpenings;
    
    @Builder.Default
    @Column(nullable = false)
    private Long viewCount = 0L;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = JobStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * CHANGE: Added bi-directional mapping.
     * PURPOSE: To allow Thymeleaf to access job.applications.size().
     * mappedBy="job" refers to the 'job' field inside the Application entity.
     */
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude // Prevent infinite recursion with Lombok's ToString
    private List<Application> applications;
   }