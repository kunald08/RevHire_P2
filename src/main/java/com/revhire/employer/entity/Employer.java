package com.revhire.employer.entity;

import com.revhire.auth.entity.User;
import com.revhire.job.entity.Job;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**

 * Employer entity — represents a company profile linked to a User with EMPLOYER role.
 * One employer profile per user account. Owns zero-to-many Job postings.
 */
@Entity
@Table(name = "employers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employer {

    @Id
    @SequenceGenerator(
            name = "employer_seq",
            sequenceName = "employer_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "employer_seq"
    )
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(length = 100)
    private String industry;

    @Column(length = 50)
    private String companySize;

    @Column(columnDefinition = "CLOB")
    private String description;

    @Column(length = 500)
    private String website;

    @Column(length = 200)
    private String location;

    @Column(length = 500)
    private String logoUrl;

    @Builder.Default
    @OneToMany(mappedBy = "employer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Job> jobs = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
