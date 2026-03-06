package com.revhire.profile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Skill entity — stores skills of a job seeker with proficiency level.
 */
@Entity
@Table(name = "skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JobSeekerProfile profile;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String proficiency;
}
