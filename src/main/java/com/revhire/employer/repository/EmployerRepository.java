package com.revhire.employer.repository;

import com.revhire.auth.entity.User;
import com.revhire.employer.entity.Employer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployerRepository extends JpaRepository<Employer, Long> {

    Optional<Employer> findByUser(User user);
    Optional<Employer> findByUserEmail(String email);

    /** Single-column projection — avoids loading the full entity + count queries. */
    @Query("SELECT e.companyName FROM Employer e WHERE e.user.email = :email")
    Optional<String> findCompanyNameByUserEmail(@Param("email") String email);

}