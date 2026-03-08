package com.revhire.application.entity;

import com.revhire.auth.entity.User;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.job.entity.Job;
import com.revhire.profile.entity.Resume;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "seeker_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seeker_id", nullable = false)
    private User seeker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "employer_comment", columnDefinition = "TEXT")
    private String employerComment;

    @Column(name = "withdraw_reason", columnDefinition = "TEXT")
    private String withdrawReason;

    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    

    public boolean canWithdraw() {
        return status != ApplicationStatus.WITHDRAWN && 
               status != ApplicationStatus.SHORTLISTED && 
               status != ApplicationStatus.REJECTED;

    }
}