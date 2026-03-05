package com.revhire.profile.entity;

import jakarta.persistence.*;
import java.util.List;
import com.revhire.auth.entity.User;


@Entity
@Table(name = "job_seeker_profile")
public class JobSeekerProfile {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

    private String fullName;

    private String phone;
    
    private String email;
    
    private String location;
    
    @Column(length = 2000)
    private String objective;

    @Column(length = 3000)
    private String projects;

    @Column(length = 1000)
    private String summary;
    
    @OneToMany(mappedBy = "profile",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Skill> skills;

	@OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Education> educations;

	@OneToMany(mappedBy = "profile",
	           cascade = CascadeType.ALL,
	           orphanRemoval = true)
	private List<Experience> experiences;
	
	@OneToMany(mappedBy = "profile",
	           cascade = CascadeType.ALL,
	           orphanRemoval = true)
	private List<Certification> certifications;

    private String linkedInUrl;

    private String githubUrl;

    private String resumePath;

    // ===== Getters and Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }
    public String getProjects() {
        return projects;
    }

    public void setProjects(String projects) {
        this.projects = projects;
    }
    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills;
    }
    public List<Experience> getExperiences() {
        return experiences;
    }
    
    public void setExperiences(List<Experience> experiences) {
        this.experiences = experiences;
    }
  
    public List<Certification> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<Certification> certifications) {
        this.certifications = certifications;
    }
    
    public String getLinkedInUrl() {
        return linkedInUrl;
    }

    public void setLinkedInUrl(String linkedInUrl) {
        this.linkedInUrl = linkedInUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getResumePath() {
        return resumePath;
    }

    public void setResumePath(String resumePath) {
        this.resumePath = resumePath;
    }
}