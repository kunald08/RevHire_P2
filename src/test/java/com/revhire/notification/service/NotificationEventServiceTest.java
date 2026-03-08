package com.revhire.notification.service;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.Role;
import com.revhire.employer.entity.Employer;
import com.revhire.job.entity.Job;
import com.revhire.profile.entity.JobSeekerProfile;
import com.revhire.profile.entity.Skill;
import com.revhire.profile.repository.ProfileRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NotificationEventServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationEventService notificationEventService;

    private User seekerUser;
    private User employerUser;
    private Employer employer;
    private Job testJob;

    @Before
    public void setUp() {
        seekerUser = User.builder()
                .id(1L)
                .name("John Seeker")
                .email("seeker@test.com")
                .password("pass")
                .role(Role.SEEKER)
                .build();

        employerUser = User.builder()
                .id(2L)
                .name("Employer")
                .email("employer@test.com")
                .password("pass")
                .role(Role.EMPLOYER)
                .build();

        employer = Employer.builder()
                .id(1L)
                .user(employerUser)
                .companyName("TechCorp")
                .build();

        testJob = Job.builder()
                .id(10L)
                .title("Java Developer")
                .employer(employer)
                .requiredSkills("Java, Spring Boot, MySQL")
                .build();
    }

    @Test
    public void testSendJobRecommendations_MatchingSkills() {
        // Create profile with matching skill
        Skill javaSkill = Skill.builder().id(1L).name("Java").build();
        JobSeekerProfile profile = JobSeekerProfile.builder()
                .id(1L)
                .user(seekerUser)
                .skills(new ArrayList<>(Arrays.asList(javaSkill)))
                .build();

        when(profileRepository.findAll()).thenReturn(Arrays.asList(profile));

        notificationEventService.sendJobRecommendations(testJob);

        verify(notificationService, times(1)).notifyJobRecommendation(
                eq(seekerUser), eq("Java Developer"), eq("TechCorp"), eq(10L));
    }

    @Test
    public void testSendJobRecommendations_NoMatchingSkills() {
        // Create profile with non-matching skill
        Skill pythonSkill = Skill.builder().id(1L).name("Python").build();
        JobSeekerProfile profile = JobSeekerProfile.builder()
                .id(1L)
                .user(seekerUser)
                .skills(new ArrayList<>(Arrays.asList(pythonSkill)))
                .build();

        when(profileRepository.findAll()).thenReturn(Arrays.asList(profile));

        notificationEventService.sendJobRecommendations(testJob);

        verify(notificationService, never()).notifyJobRecommendation(
                any(), anyString(), anyString(), anyLong());
    }

    @Test
    public void testSendJobRecommendations_NoRequiredSkills() {
        Job jobWithNoSkills = Job.builder()
                .id(11L)
                .title("General Role")
                .employer(employer)
                .requiredSkills(null)
                .build();

        notificationEventService.sendJobRecommendations(jobWithNoSkills);

        verify(notificationService, never()).notifyJobRecommendation(
                any(), anyString(), anyString(), anyLong());
    }

    @Test
    public void testSendJobRecommendations_EmptyRequiredSkills() {
        Job jobWithEmptySkills = Job.builder()
                .id(12L)
                .title("General Role")
                .employer(employer)
                .requiredSkills("  ")
                .build();

        notificationEventService.sendJobRecommendations(jobWithEmptySkills);

        verify(notificationService, never()).notifyJobRecommendation(
                any(), anyString(), anyString(), anyLong());
    }

    @Test
    public void testSendJobRecommendations_MultipleProfiles_OnlyMatchingNotified() {
        // Matching profile
        Skill springSkill = Skill.builder().id(1L).name("Spring Boot").build();
        JobSeekerProfile matchingProfile = JobSeekerProfile.builder()
                .id(1L)
                .user(seekerUser)
                .skills(new ArrayList<>(Arrays.asList(springSkill)))
                .build();

        // Non-matching profile
        User anotherSeeker = User.builder()
                .id(3L).name("Jane").email("jane@test.com")
                .password("pass").role(Role.SEEKER).build();
        Skill reactSkill = Skill.builder().id(2L).name("React").build();
        JobSeekerProfile nonMatchingProfile = JobSeekerProfile.builder()
                .id(2L)
                .user(anotherSeeker)
                .skills(new ArrayList<>(Arrays.asList(reactSkill)))
                .build();

        when(profileRepository.findAll()).thenReturn(Arrays.asList(matchingProfile, nonMatchingProfile));

        notificationEventService.sendJobRecommendations(testJob);

        // Only the matching profile should be notified
        verify(notificationService, times(1)).notifyJobRecommendation(
                eq(seekerUser), anyString(), anyString(), anyLong());
        verify(notificationService, never()).notifyJobRecommendation(
                eq(anotherSeeker), anyString(), anyString(), anyLong());
    }

    @Test
    public void testSendJobRecommendations_EmployerUserSkipped() {
        // Profile belonging to an employer (shouldn't happen, but defensive)
        Skill javaSkill = Skill.builder().id(1L).name("Java").build();
        JobSeekerProfile profile = JobSeekerProfile.builder()
                .id(1L)
                .user(employerUser) // Employer role
                .skills(new ArrayList<>(Arrays.asList(javaSkill)))
                .build();

        when(profileRepository.findAll()).thenReturn(Arrays.asList(profile));

        notificationEventService.sendJobRecommendations(testJob);

        verify(notificationService, never()).notifyJobRecommendation(
                any(), anyString(), anyString(), anyLong());
    }

    @Test
    public void testSendJobRecommendations_ProfileWithNoSkills() {
        JobSeekerProfile profileNoSkills = JobSeekerProfile.builder()
                .id(1L)
                .user(seekerUser)
                .skills(new ArrayList<>())
                .build();

        when(profileRepository.findAll()).thenReturn(Arrays.asList(profileNoSkills));

        notificationEventService.sendJobRecommendations(testJob);

        verify(notificationService, never()).notifyJobRecommendation(
                any(), anyString(), anyString(), anyLong());
    }

    @Test
    public void testSendJobRecommendations_CaseInsensitiveMatching() {
        // Skill with different case
        Skill skill = Skill.builder().id(1L).name("JAVA").build();
        JobSeekerProfile profile = JobSeekerProfile.builder()
                .id(1L)
                .user(seekerUser)
                .skills(new ArrayList<>(Arrays.asList(skill)))
                .build();

        when(profileRepository.findAll()).thenReturn(Arrays.asList(profile));

        notificationEventService.sendJobRecommendations(testJob);

        verify(notificationService, times(1)).notifyJobRecommendation(
                eq(seekerUser), anyString(), anyString(), anyLong());
    }
}
