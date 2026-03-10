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
import java.util.ArrayList;
import java.util.List;

/**

 * Job entity — represents a single job posting belonging to an Employer.
 * Lifecycle: DRAFT → ACTIVE → CLOSED / FILLED.
 */
@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @SequenceGenerator(
            name = "job_seq",
            sequenceName = "job_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "job_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id", nullable = false)
    @ToString.Exclude
    private Employer employer;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String description;

    @Column(length = 500)
    private String requiredSkills;

    private Integer experienceMin;
    private Integer experienceMax;

    @Column(length = 100)
    private String educationReq;

    @Column(length = 200)
    private String location;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private JobStatus status;

    private LocalDate deadline;

    @Builder.Default
    private Integer numOpenings = 1;

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

        if (this.viewCount == null) {
            this.viewCount = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**

     * Bi-directional mapping to Application.
     */
    @Builder.Default
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Application> applications = new ArrayList<>();

    /** Transient field populated by service layer — avoids N+1 lazy-load. */
    @Transient
    private Long applicationCount;
}
