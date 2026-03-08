package com.revhire.employer.service;

import com.revhire.application.repository.ApplicationRepository;
import com.revhire.common.enums.JobStatus;
import com.revhire.employer.dto.DashboardStats;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.ApplicantRepository;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private EmployerRepository employerRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicantRepository applicantRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Employer mockEmployer;

    @BeforeEach
    void setUp() {
        mockEmployer = new Employer();
        mockEmployer.setId(1L);
        mockEmployer.setCompanyName("RevHire Tech");
    }

    @Test
    void getEmployerDashboardStats_ShouldReturnCorrectStats() {
        // Arrange
        String email = "employer@test.com";
        when(employerRepository.findByUserEmail(email)).thenReturn(Optional.of(mockEmployer));
        
        when(jobRepository.countByEmployerId(1L)).thenReturn(5L);
        when(jobRepository.countByEmployerIdAndStatus(1L, JobStatus.ACTIVE)).thenReturn(3L);
        when(applicationRepository.countByJobEmployerId(1L)).thenReturn(10L);
        when(applicantRepository.countByJobEmployerIdAndStatusIn(eq(1L), anyList())).thenReturn(2L);

        // Act
        DashboardStats stats = dashboardService.getEmployerDashboardStats(email);

        // Assert
        assertNotNull(stats);
        assertEquals("RevHire Tech", stats.getCompanyName());
        assertEquals(5L, stats.getTotalJobs());
        assertEquals(3L, stats.getActiveJobs());
        assertEquals(10L, stats.getTotalApplications());
        assertEquals(2L, stats.getPendingReviews());
    }

    @Test
    void getEmployerDashboardStats_ShouldThrowException_WhenEmployerNotFound() {
        // Arrange
        String email = "invalid@test.com";
        when(employerRepository.findByUserEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> dashboardService.getEmployerDashboardStats(email));
    }
}