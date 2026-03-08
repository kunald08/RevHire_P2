package com.revhire.application.service;

import com.revhire.application.dto.ApplicationRequest;
import com.revhire.application.dto.ApplicationResponse;
import com.revhire.application.dto.WithdrawRequest;
import com.revhire.application.entity.Application;
import com.revhire.application.repository.ApplicationRepository;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.common.enums.Role;
import com.revhire.employer.entity.Employer;
import com.revhire.exception.BadRequestException;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.exception.UnauthorizedException;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import com.revhire.common.enums.JobStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.revhire.profile.repository.ResumeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private User testSeeker;
    private Job testJob;
    private Application testApplication;
    private ApplicationRequest testRequest;

    @Before
    public void setUp() {
        // Setup test seeker
        testSeeker = new User();
        testSeeker.setId(1L);
        testSeeker.setName("John Doe");
        testSeeker.setEmail("john@example.com");
        testSeeker.setRole(Role.SEEKER);

        // Setup test employer
        Employer testEmployer = new Employer();
        testEmployer.setId(1L);
        testEmployer.setCompanyName("Tech Corp");

        // Setup test job
        testJob = new Job();
        testJob.setId(1L);
        testJob.setTitle("Software Engineer");
        testJob.setEmployer(testEmployer);
        testJob.setStatus(JobStatus.ACTIVE);
        testJob.setDeadline(LocalDate.now().plusDays(30));

        // Setup test application
        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setJob(testJob);
        testApplication.setSeeker(testSeeker);
        testApplication.setStatus(ApplicationStatus.APPLIED);
        testApplication.setAppliedAt(LocalDateTime.now());

        // Setup test request
        testRequest = new ApplicationRequest();
        testRequest.setJobId(1L);
        testRequest.setCoverLetter("I am interested");
    }

    @Test
    public void testApplyForJob_Success() {
        when(applicationRepository.existsByJobIdAndSeekerId(1L, 1L)).thenReturn(false);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(testJob));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testSeeker));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);

        ApplicationResponse response = applicationService.applyForJob(1L, testRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId().longValue());
        assertEquals("Software Engineer", response.getJobTitle());
        assertEquals("Tech Corp", response.getCompanyName());
        assertEquals(ApplicationStatus.APPLIED, response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testApplyForJob_AlreadyApplied() {
        when(applicationRepository.existsByJobIdAndSeekerId(1L, 1L)).thenReturn(true);
        applicationService.applyForJob(1L, testRequest);
    }

    @Test
    public void testGetMyApplications() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Application> mockPage = new PageImpl<>(Arrays.asList(testApplication));
        
        when(applicationRepository.findBySeekerId(1L, pageable)).thenReturn(mockPage);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(testJob));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testSeeker));

        Page<ApplicationResponse> result = applicationService.getMyApplications(1L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Software Engineer", result.getContent().get(0).getJobTitle());
    }

    @Test
    public void testGetApplicationDetails() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(testJob));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testSeeker));

        ApplicationResponse response = applicationService.getApplicationDetails(1L, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getId().longValue());
        assertEquals("Software Engineer", response.getJobTitle());
    }

    @Test(expected = UnauthorizedException.class)
    public void testGetApplicationDetails_Unauthorized() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        applicationService.getApplicationDetails(1L, 2L); // Wrong user
    }

    @Test
    public void testWithdrawApplication() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        
        WithdrawRequest withdrawRequest = new WithdrawRequest();
        withdrawRequest.setReason("Found another job");

        applicationService.withdrawApplication(1L, 1L, withdrawRequest);

        assertEquals(ApplicationStatus.WITHDRAWN, testApplication.getStatus());
        assertEquals("Found another job", testApplication.getWithdrawReason());
        verify(applicationRepository, times(1)).save(testApplication);
    }

    @Test
    public void testHasApplied() {
        when(applicationRepository.existsByJobIdAndSeekerId(1L, 1L)).thenReturn(true);
        assertTrue(applicationService.hasApplied(1L, 1L));

        when(applicationRepository.existsByJobIdAndSeekerId(1L, 1L)).thenReturn(false);
        assertFalse(applicationService.hasApplied(1L, 1L));
    }
}