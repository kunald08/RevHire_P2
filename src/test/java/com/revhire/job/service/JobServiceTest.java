package com.revhire.job.service;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.JobStatus;
import com.revhire.common.enums.JobType;
import com.revhire.common.enums.Role;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.exception.BadRequestException;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.job.dto.JobRequest;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JobServiceTest {

    @Autowired
    private JobService jobService;

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private EmployerRepository employerRepository;

    @MockBean
    private UserRepository userRepository;

    // ── CREATE JOB SUCCESS ──
    @Test
    public void createJob_success() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer employer = new Employer();
        employer.setId(1L);
        employer.setUser(user);
        employer.setCompanyName("Test Corp");
        employer.setIndustry("IT");
        employer.setLocation("Hyderabad");

        JobRequest request = validRequest();

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(employer));

        Mockito.when(jobRepository.save(Mockito.any(Job.class)))
                .thenAnswer(i -> {
                    Job j = i.getArgument(0);
                    j.setId(1L);
                    return j;
                });

        JobResponse response = jobService.createJob(request, "emp@test.com");
        assertNotNull(response);
        assertEquals("Software Engineer", response.getTitle());
        assertEquals(JobStatus.ACTIVE, response.getStatus());
    }

    // ── INVALID SALARY RANGE ──
    @Test(expected = BadRequestException.class)
    public void createJob_invalidSalary() {

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

        jobService.createJob(request, "emp@test.com");
    }

    // ── INVALID EXPERIENCE RANGE ──
    @Test(expected = BadRequestException.class)
    public void createJob_invalidExperience() {

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

        jobService.createJob(request, "emp@test.com");
    }

    // ── DELETE FILLED JOB → BLOCKED ──
    @Test(expected = BadRequestException.class)
    public void deleteFilledJob() {

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

        jobService.deleteJob(10L, "emp@test.com");
    }

    // ── UPDATE JOB SUCCESS ──
    @Test
    public void updateJob_success() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer employer = new Employer();
        employer.setId(1L);
        employer.setUser(user);
        employer.setCompanyName("Test Corp");
        employer.setIndustry("IT");
        employer.setLocation("Hyderabad");

        Job job = new Job();
        job.setId(1L);
        job.setEmployer(employer);

        JobRequest request = validRequest();
        request.setTitle("Updated Title");

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(employer));

        Mockito.when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        Mockito.when(jobRepository.save(Mockito.any(Job.class)))
                .thenAnswer(i -> i.getArgument(0));

        JobResponse response = jobService.updateJob(1L, request, "emp@test.com");
        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
    }

    // ── GET JOB BY ID ──
    @Test
    public void getJobById_success() {

        Employer employer = new Employer();
        employer.setId(1L);
        employer.setCompanyName("Test Corp");
        employer.setIndustry("IT");
        employer.setLocation("Hyderabad");

        Job job = new Job();
        job.setId(1L);
        job.setTitle("Developer");
        job.setEmployer(employer);
        job.setStatus(JobStatus.ACTIVE);
        job.setViewCount(10L);
        job.setJobType(JobType.FULL_TIME);
        job.setDeadline(LocalDate.now().plusDays(10));
        job.setSalaryMin(new BigDecimal("500000"));
        job.setSalaryMax(new BigDecimal("1200000"));
        job.setExperienceMin(2);
        job.setExperienceMax(5);
        job.setNumOpenings(3);
        job.setRequiredSkills("Java, Spring");
        job.setEducationReq("B.Tech");
        job.setLocation("Hyderabad");

        Mockito.when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        Mockito.when(jobRepository.save(Mockito.any(Job.class)))
                .thenAnswer(i -> i.getArgument(0));

        JobResponse response = jobService.getJobById(1L);
        assertNotNull(response);
        assertEquals("Developer", response.getTitle());
        assertEquals(Long.valueOf(10), Long.valueOf(response.getViewCount())); // no increment on getJobById(id) without email
    }

    // ── GET JOB NOT FOUND ──
    @Test(expected = ResourceNotFoundException.class)
    public void getJobById_notFound() {

        Mockito.when(jobRepository.findById(999L))
                .thenReturn(Optional.empty());

        jobService.getJobById(999L);
    }

    // ── CLOSE JOB ──
    @Test
    public void closeJob_success() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer employer = new Employer();
        employer.setId(1L);
        employer.setUser(user);

        Job job = new Job();
        job.setId(1L);
        job.setEmployer(employer);
        job.setStatus(JobStatus.ACTIVE);

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(employer));

        Mockito.when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        Mockito.when(jobRepository.save(Mockito.any(Job.class)))
                .thenAnswer(i -> i.getArgument(0));

        jobService.closeJob(1L, "emp@test.com");
        assertEquals(JobStatus.CLOSED, job.getStatus());
    }

    // ── FILL JOB ──
    @Test
    public void fillJob_success() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer employer = new Employer();
        employer.setId(1L);
        employer.setUser(user);

        Job job = new Job();
        job.setId(1L);
        job.setEmployer(employer);
        job.setStatus(JobStatus.ACTIVE);

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(employer));

        Mockito.when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        Mockito.when(jobRepository.save(Mockito.any(Job.class)))
                .thenAnswer(i -> i.getArgument(0));

        jobService.markAsFilled(1L, "emp@test.com");
        assertEquals(JobStatus.FILLED, job.getStatus());
    }

    // ── EMPLOYER NOT FOUND ──
    @Test(expected = ResourceNotFoundException.class)
    public void createJob_employerNotFound() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.empty());

        jobService.createJob(validRequest(), "emp@test.com");
    }

    // ── GET JOBS BY EMPLOYER ──
    @Test
    public void getMyJobs_empty() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer employer = new Employer();
        employer.setId(1L);
        employer.setUser(user);

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(employer));

        Mockito.when(jobRepository.findByEmployer(
                Mockito.eq(employer), Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Page<JobResponse> page = jobService.getEmployerJobs("emp@test.com", 0);
        assertNotNull(page);
        assertEquals(0, page.getTotalElements());
    }

    // ── HELPER ──
    private JobRequest validRequest() {

        JobRequest request = new JobRequest();
        request.setTitle("Software Engineer");
        request.setDescription("Backend development with Spring Boot and microservices.");
        request.setLocation("Hyderabad");
        request.setSalaryMin(new BigDecimal("500000"));
        request.setSalaryMax(new BigDecimal("1200000"));
        request.setExperienceMin(1);
        request.setExperienceMax(3);
        request.setRequiredSkills("Java, Spring Boot");
        request.setEducationReq("B.Tech");
        request.setDeadline(LocalDate.now().plusDays(10));
        request.setNumOpenings(2);
        request.setJobType(JobType.FULL_TIME);

        return request;
    }
}