package com.revhire.profile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**

 * Education entity — stores educational qualifications of a job seeker.
 */
@Entity
@Table(name = "educations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education {

    @Id
    @SequenceGenerator(
            name = "education_seq",
            sequenceName = "education_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "education_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JobSeekerProfile profile;

    @Column(nullable = false, length = 200)
    private String institution;

    @Column(length = 100)
    private String degree;

    @Column(name = "field_of_study", length = 100)
    private String fieldOfStudy;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 20)
    private String grade;
}
