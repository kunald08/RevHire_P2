package com.revhire.profile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**

 * Certification entity — stores professional certifications of a job seeker.
 */
@Entity
@Table(name = "certifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certification {

    @Id
    @SequenceGenerator(
            name = "certification_seq",
            sequenceName = "certification_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "certification_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JobSeekerProfile profile;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "issuing_org", length = 200)
    private String issuingOrg;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "credential_url", length = 500)
    private String credentialUrl;
}
