package com.revhire.profile.entity;

import jakarta.persistence.*;

@Entity
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private JobSeekerProfile profile;

    // getters & setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JobSeekerProfile getProfile() {
        return profile;
    }

    public void setProfile(JobSeekerProfile profile) {
        this.profile = profile;
    }
}