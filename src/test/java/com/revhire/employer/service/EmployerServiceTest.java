package com.revhire.employer.service;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.Role;
import com.revhire.employer.dto.EmployerRequest;
import com.revhire.employer.dto.EmployerResponse;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.exception.UnauthorizedException;
import com.revhire.job.repository.JobRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EmployerServiceTest {

    @Autowired
    private EmployerService employerService;

    @MockBean
    private EmployerRepository employerRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JobRepository jobRepository;

    // ── CREATE PROFILE SUCCESS ──
    @Test
    public void createEmployer_success() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        EmployerRequest request = validRequest();

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.empty());

        Mockito.when(employerRepository.save(Mockito.any(Employer.class)))
                .thenAnswer(i -> i.getArgument(0));

        EmployerResponse response = employerService.createOrUpdateEmployer(request, "emp@test.com");
        assertNotNull(response);
        assertEquals("Tech Corp", response.getCompanyName());
        assertEquals("IT", response.getIndustry());
    }

    // ── UPDATE EXISTING PROFILE ──
    @Test
    public void updateEmployer_success() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer existing = new Employer();
        existing.setId(1L);
        existing.setUser(user);
        existing.setCompanyName("Old Name");

        EmployerRequest request = validRequest();
        request.setCompanyName("New Name");

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(existing));

        Mockito.when(employerRepository.save(Mockito.any(Employer.class)))
                .thenAnswer(i -> i.getArgument(0));

        EmployerResponse response = employerService.createOrUpdateEmployer(request, "emp@test.com");
        assertNotNull(response);
        assertEquals("New Name", response.getCompanyName());
    }

    // ── UNAUTHORIZED (SEEKER) ──
    @Test(expected = UnauthorizedException.class)
    public void createEmployer_unauthorized() {

        User user = new User();
        user.setEmail("user@test.com");
        user.setRole(Role.SEEKER);

        EmployerRequest request = validRequest();

        Mockito.when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user));

        employerService.createOrUpdateEmployer(request, "user@test.com");
    }

    // ── GET PROFILE SUCCESS ──
    @Test
    public void getProfile_success() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Employer employer = new Employer();
        employer.setId(1L);
        employer.setUser(user);
        employer.setCompanyName("Tech Corp");
        employer.setIndustry("IT");
        employer.setCompanySize("50-100");

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.of(employer));

        EmployerResponse response = employerService.getEmployerByEmail("emp@test.com");
        assertNotNull(response);
        assertEquals("Tech Corp", response.getCompanyName());
    }

    // ── GET PROFILE NOT FOUND ──
    @Test(expected = ResourceNotFoundException.class)
    public void getProfile_notFound() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.empty());

        employerService.getEmployerByEmail("emp@test.com");
    }

    // ── HAS PROFILE ──
    @Test
    public void hasProfile_true() {

        Mockito.when(employerRepository.findByUserEmail("emp@test.com"))
                .thenReturn(Optional.of(new Employer()));

        assertTrue(employerService.hasProfile("emp@test.com"));
    }

    @Test
    public void hasProfile_false() {

        Mockito.when(employerRepository.findByUserEmail("emp@test.com"))
                .thenReturn(Optional.empty());

        assertFalse(employerService.hasProfile("emp@test.com"));
    }

    // ── USER NOT FOUND ──
    @Test(expected = ResourceNotFoundException.class)
    public void createEmployer_userNotFound() {

        EmployerRequest request = validRequest();

        Mockito.when(userRepository.findByEmail("nope@test.com"))
                .thenReturn(Optional.empty());

        employerService.createOrUpdateEmployer(request, "nope@test.com");
    }

    // ── HELPER ──
    private EmployerRequest validRequest() {

        EmployerRequest request = new EmployerRequest();
        request.setCompanyName("Tech Corp");
        request.setIndustry("IT");
        request.setCompanySize("50-100");
        request.setDescription("A great software company with modern practices.");
        request.setWebsite("https://techcorp.com");
        request.setLocation("Hyderabad");

        return request;
    }
}