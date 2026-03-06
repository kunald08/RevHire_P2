package com.revhire.profile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Experience entity — stores work experience entries of a job seeker.
 */
@Entity
@Table(name = "experiences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JobSeekerProfile profile;

    @Column(nullable = false, length = 200)
    private String company;

    @Column(length = 100)
    private String title;

    @Column(length = 100)
    private String location;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;
}
