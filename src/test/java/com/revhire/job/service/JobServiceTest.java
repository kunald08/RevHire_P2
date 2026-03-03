package com.revhire.job.service;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.JobStatus;
import com.revhire.common.enums.JobType;
import com.revhire.common.enums.Role;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.exception.BadRequestException;
import com.revhire.job.dto.JobRequest;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class JobServiceTest {

    @Autowired
    private JobService jobService;

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private EmployerRepository employerRepository;

    @MockBean
    private UserRepository userRepository;

    // ==============================
    // SUCCESS CASE
    // ==============================

    @Test
    void testCreateJob_Success() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer employer = new Employer();
        employer.setId(1L);
        employer.setUser(user);

        JobRequest request = validRequest();

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(employer));

        Mockito.when(jobRepository.save(Mockito.any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertNotNull(jobService.createJob(request, "emp@test.com"));
    }

    // ==============================
    // INVALID SALARY RANGE
    // ==============================

    @Test
    void testCreateJob_InvalidSalaryRange() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer employer = new Employer();
        employer.setUser(user);

        JobRequest request = validRequest();
        request.setSalaryMin(new BigDecimal("10000"));
        request.setSalaryMax(new BigDecimal("5000"));

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(employer));

        assertThrows(BadRequestException.class,
                () -> jobService.createJob(request, "emp@test.com"));
    }

    // ==============================
    // INVALID EXPERIENCE RANGE
    // ==============================

    @Test
    void testCreateJob_InvalidExperienceRange() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer employer = new Employer();
        employer.setUser(user);

        JobRequest request = validRequest();
        request.setExperienceMin(5);
        request.setExperienceMax(2);

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(employer));

        assertThrows(BadRequestException.class,
                () -> jobService.createJob(request, "emp@test.com"));
    }

    // ==============================
    // DELETE FILLED JOB
    // ==============================

    @Test
    void testDeleteFilledJob_ThrowsException() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer employer = new Employer();
        employer.setId(1L);
        employer.setUser(user);

        Job job = new Job();
        job.setId(10L);
        job.setEmployer(employer);
        job.setStatus(JobStatus.FILLED);

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(employer));

        Mockito.when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        assertThrows(BadRequestException.class,
                () -> jobService.deleteJob(10L, "emp@test.com"));
    }

    // ==============================
    // HELPER METHOD
    // ==============================

    private JobRequest validRequest() {

        JobRequest request = new JobRequest();
        request.setTitle("Software Engineer");
        request.setDescription("Develop backend services");
        request.setLocation("Hyderabad");
        request.setSalaryMin(new BigDecimal("5000"));
        request.setSalaryMax(new BigDecimal("10000"));
        request.setExperienceMin(1);
        request.setExperienceMax(3);
        request.setRequiredSkills("Java, Spring");
        request.setEducationReq("B.Tech");
        request.setDeadline(LocalDate.now().plusDays(5));
        request.setNumOpenings(2);
        request.setJobType(JobType.FULL_TIME);

        return request;
    }
}