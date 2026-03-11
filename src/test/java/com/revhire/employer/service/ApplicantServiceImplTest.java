package com.revhire.employer.service;

import com.revhire.application.entity.Application;
import com.revhire.application.repository.ApplicationRepository;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.employer.dto.ApplicantRowDTO;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.*;
import com.revhire.notification.service.NotificationService;
import com.revhire.profile.entity.JobSeekerProfile;
import com.revhire.profile.repository.ProfileRepository;
import com.revhire.profile.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicantServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicantRepository applicantRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private ApplicantNoteRepository applicantNoteRepository;
    @Mock private NotificationService notificationService;
    @Mock private ProfileService profileService;

    @InjectMocks
    private ApplicantServiceImpl applicantService;

    private Application mockApp;

    @BeforeEach
    void setUp() {
        mockApp = new Application();
        mockApp.setId(1L);
        mockApp.setStatus(ApplicationStatus.APPLIED);
        
        // Fixes the NPE: Ensure seeker is never null
        com.revhire.auth.entity.User mockSeeker = new com.revhire.auth.entity.User();
        mockSeeker.setId(99L);
        mockSeeker.setName("John Doe");
        mockApp.setSeeker(mockSeeker); 
    }

    @Test
    void bulkUpdateStatus_ShouldUpdateAndNotify() {
        // Arrange
        List<Long> ids = List.of(1L);
        
        // Add a Job to the mockApp so getJob() doesn't return null
        com.revhire.job.entity.Job mockJob = new com.revhire.job.entity.Job();
        mockJob.setTitle("Software Engineer");
        mockApp.setJob(mockJob); 
        
        // Mock the user/seeker as well, since createNotification uses it
        com.revhire.auth.entity.User mockSeeker = new com.revhire.auth.entity.User();
        mockApp.setSeeker(mockSeeker);

        when(applicationRepository.findAllById(ids)).thenReturn(List.of(mockApp));

        // Act
        applicantService.bulkUpdateStatus(ids, "SHORTLIST", "Great candidate!");

        // Assert
        assertEquals(ApplicationStatus.SHORTLISTED, mockApp.getStatus());
        verify(notificationService, times(1)).createNotification(any(), anyString(), any(), anyString());
    }

    @Test
    void getFilteredApplicants_ShouldApplyFiltersCorrectly() {
        // Arrange
        Long jobId = 10L;
        JobSeekerProfile profile = new JobSeekerProfile();
        profile.setId(1L);
        
        when(applicationRepository.findByJobId(jobId)).thenReturn(List.of(mockApp));
        when(profileRepository.findByUserId(anyLong())).thenReturn(Optional.of(profile));
        when(profileService.getExperienceInYears(anyLong())).thenReturn(5);

        // Act - Testing MinExp filter
        List<ApplicantRowDTO> results = applicantService.getFilteredApplicants(
                jobId, null, null, 3, null, null, null);

        // Assert
        assertFalse(results.isEmpty());
        verify(profileService).getExperienceInYears(anyLong());
    }

	@Test
	void getFilteredApplicants_ShouldReturnEmpty_WhenFilterDoesNotMatch() {
	    // Arrange
	    Long jobId = 1L;
	    Long seekerId = 99L; // Must match the ID in setUp()
	    
	    when(applicationRepository.findByJobId(jobId)).thenReturn(List.of(mockApp));
	    
	    // Stub the profile so the filter logic proceeds to the experience check
	    JobSeekerProfile mockProfile = new JobSeekerProfile();
	    mockProfile.setId(500L); // Profile ID for experience check
	    when(profileRepository.findByUserId(seekerId)).thenReturn(Optional.of(mockProfile));
	    
	    // Stub the experience service to return a value lower than the filter requirement (10)
	    when(profileService.getExperienceInYears(500L)).thenReturn(1);
	    
	    // Act - Filter with minExp=10, but profile only has 1 year
	    List<ApplicantRowDTO> results = applicantService.getFilteredApplicants(
	            jobId, null, null, 10, null, null, null);
	
	    // Assert
	    assertTrue(results.isEmpty(), "Results should be empty because experience (1) is less than required (10)");
	    
	    // Verify interactions happened as expected
	    verify(profileService).getExperienceInYears(500L);
	}
}