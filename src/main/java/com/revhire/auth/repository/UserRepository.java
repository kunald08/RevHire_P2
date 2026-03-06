package com.revhire.auth.repository;

import com.revhire.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User repository stub — Ashwathy will add full query methods on feature/auth.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>
{

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    
    Optional<User> findByResetToken(String resetToken);
}
