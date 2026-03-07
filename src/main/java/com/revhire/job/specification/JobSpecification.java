package com.revhire.job.specification;

import com.revhire.common.enums.JobType;
import com.revhire.job.entity.Job;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Specification class for building dynamic job search queries
 * Used by Member D (Search & Applications module) for advanced job searching
 */
public class JobSpecification {
    
    /**
     * Filter for active jobs only (status = 'ACTIVE')
     */
    public static Specification<Job> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), com.revhire.common.enums.JobStatus.ACTIVE);
    }
    
    /**
     * Search by keyword in title, description, and required skills
     */
    public static Specification<Job> keywordSearch(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("requiredSkills")), pattern)
            );
        };
    }
    
    /**
     * Filter by location (case-insensitive partial match)
     */
    public static Specification<Job> locationContains(String location) {
        return (root, query, cb) -> {
            if (location == null || location.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(
                cb.lower(root.get("location")), 
                "%" + location.toLowerCase().trim() + "%"
            );
        };
    }
    
    /**
     * Filter by minimum experience required
     */
    public static Specification<Job> minExperience(Integer minExperience) {
        return (root, query, cb) -> {
            if (minExperience == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("experienceMin"), minExperience);
        };
    }
    
    /**
     * Filter by maximum experience required
     */
    public static Specification<Job> maxExperience(Integer maxExperience) {
        return (root, query, cb) -> {
            if (maxExperience == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("experienceMax"), maxExperience);
        };
    }
    
    /**
     * Filter by company name (case-insensitive partial match)
     */
    public static Specification<Job> companyName(String companyName) {
        return (root, query, cb) -> {
            if (companyName == null || companyName.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(
                cb.lower(root.get("employer").get("companyName")), 
                "%" + companyName.toLowerCase().trim() + "%"
            );
        };
    }
    
    /**
     * Filter by minimum salary
     */
    public static Specification<Job> minSalary(BigDecimal minSalary) {
        return (root, query, cb) -> {
            if (minSalary == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("salaryMax"), minSalary);
        };
    }
    
    /**
     * Filter by maximum salary
     */
    public static Specification<Job> maxSalary(BigDecimal maxSalary) {
        return (root, query, cb) -> {
            if (maxSalary == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("salaryMin"), maxSalary);
        };
    }
    
    /**
     * Filter by job type
     */
    public static Specification<Job> jobType(JobType jobType) {
        return (root, query, cb) -> {
            if (jobType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("jobType"), jobType);
        };
    }
    
    /**
     * Filter by jobs posted after a specific date
     */
    public static Specification<Job> postedAfter(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt").as(LocalDate.class), date);
        };
    }
}
