package com.revhire.notification.service;

import com.revhire.auth.entity.User;
import com.revhire.common.enums.Role;
import com.revhire.auth.repository.UserRepository;
import com.revhire.job.entity.Job;
import com.revhire.profile.entity.JobSeekerProfile;
import com.revhire.profile.entity.Skill;
import com.revhire.profile.repository.ProfileRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to handle sending job recommendation notifications to seekers
 * when new jobs are posted that match their skills.
 */
@Service
public class NotificationEventService {

    private static final Logger logger = LogManager.getLogger(NotificationEventService.class);

    private final NotificationService notificationService;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public NotificationEventService(NotificationService notificationService,
                                     ProfileRepository profileRepository,
                                     UserRepository userRepository) {
        this.notificationService = notificationService;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    /**
     * When a new job is created, find seekers with matching skills and send them
     * a job recommendation notification.
     */
    @Transactional
    public void sendJobRecommendations(Job job) {
        try {
            logger.info("Processing job recommendations for job: {} (ID: {})", job.getTitle(), job.getId());

            String requiredSkills = job.getRequiredSkills();
            if (requiredSkills == null || requiredSkills.trim().isEmpty()) {
                logger.info("No required skills specified for job {}, skipping recommendations", job.getId());
                return;
            }

            // Parse required skills (comma-separated)
            Set<String> jobSkills = Arrays.stream(requiredSkills.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            if (jobSkills.isEmpty()) {
                logger.info("No valid skills parsed for job {}, skipping recommendations", job.getId());
                return;
            }

            // Find all seeker profiles
            List<JobSeekerProfile> allProfiles = profileRepository.findAll();
            String companyName = job.getEmployer() != null ? job.getEmployer().getCompanyName() : "Unknown Company";
            int notifCount = 0;

            for (JobSeekerProfile profile : allProfiles) {
                try {
                    // Get seeker's skills
                    Set<String> seekerSkills = profile.getSkills().stream()
                            .map(Skill::getName)
                            .map(String::toLowerCase)
                            .collect(Collectors.toSet());

                    // Check if there's at least one matching skill
                    boolean hasMatch = seekerSkills.stream()
                            .anyMatch(seekerSkill -> jobSkills.stream()
                                    .anyMatch(jobSkill -> seekerSkill.contains(jobSkill) || jobSkill.contains(seekerSkill)));

                    if (hasMatch) {
                        User seeker = profile.getUser();
                        if (seeker != null && seeker.getRole() == Role.SEEKER) {
                            notificationService.notifyJobRecommendation(
                                    seeker, job.getTitle(), companyName, job.getId());
                            notifCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error processing recommendations for profile {}: {}", 
                            profile.getId(), e.getMessage());
                }
            }

            logger.info("Sent {} job recommendation notifications for job {} ({})", 
                    notifCount, job.getTitle(), job.getId());

        } catch (Exception e) {
            logger.error("Error sending job recommendations for job {}: {}", job.getId(), e.getMessage(), e);
        }
    }
}
