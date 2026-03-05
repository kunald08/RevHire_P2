package com.revhire.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "experience")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;

    private String jobTitle;

    private String location;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(length = 1000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private JobSeekerProfile profile;
}