package com.revhire.employer.repository;

import com.revhire.auth.entity.User;
import com.revhire.employer.entity.Employer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerRepository extends JpaRepository<Employer, Long> {

    Optional<Employer> findByUser(User user);
    Optional<Employer> findByUserEmail(String email);

}