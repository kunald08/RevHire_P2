package com.revhire.employer.service;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.Role;
import com.revhire.employer.dto.EmployerRequest;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class EmployerServiceTest {

    @Autowired
    private EmployerService employerService;

    @MockBean
    private EmployerRepository employerRepository;

    @MockBean
    private UserRepository userRepository;

    // ==============================
    // CREATE OR UPDATE SUCCESS
    // ==============================

    @Test
    void testCreateOrUpdateEmployer_Success() {

        User user = new User();
        user.setEmail("emp@test.com");
        user.setRole(Role.EMPLOYER);

        EmployerRequest request = validRequest();

        Mockito.when(userRepository.findByEmail("emp@test.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(employerRepository.findByUser(user))
                .thenReturn(Optional.empty()); // simulate first-time profile

        Mockito.when(employerRepository.save(Mockito.any(Employer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertNotNull(
                employerService.createOrUpdateEmployer(request, "emp@test.com")
        );
    }

    // ==============================
    // UNAUTHORIZED USER
    // ==============================

    @Test
    void testCreateOrUpdateEmployer_Unauthorized() {

        User user = new User();
        user.setEmail("user@test.com");
        user.setRole(Role.SEEKER);

        EmployerRequest request = validRequest();

        Mockito.when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class,
                () -> employerService.createOrUpdateEmployer(request, "user@test.com"));
    }

    // ==============================
    // HELPER METHOD
    // ==============================

    private EmployerRequest validRequest() {

        EmployerRequest request = new EmployerRequest();
        request.setCompanyName("Tech Corp");
        request.setIndustry("IT");
        request.setCompanySize("50-100");
        request.setDescription("Software company");
        request.setWebsite("https://techcorp.com");
        request.setLocation("Hyderabad");

        return request;
    }
}