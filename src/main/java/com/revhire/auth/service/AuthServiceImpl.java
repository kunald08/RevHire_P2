package com.revhire.auth.service;

import com.revhire.auth.dto.RegisterRequest;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * Author: Aswathy J Lal
 * why this code is better: 
 * It validates 'raw' data before processing. We check email existence first 
 * because it's a quick indexed lookup. We validate password strength before 
 * encoding it because PasswordEncoder.encode() is a heavy CPU operation.

 */

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterRequest request) {
        // 1. Validate if the email is already taken using your existing repository method
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered!");
        }
        
        // 2. Password Complexity Validation
     // This protects the database from weak passwords and the CPU from encoding junk.
        validatePasswordStrength(request.getPassword());

        // 2. Map the DTO to the User Entity and encrypt the password
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                // Encrypting the password before it touches the database
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .location(request.getLocation())
                // Mapping the string role from the request to your Role Enum
                .role(Role.valueOf(request.getRole().toUpperCase()))
                .build();

        // 3. Save the new user to MySQL
        userRepository.save(user);
    }
    
    private void validatePasswordStrength(String password) {
    	//Requires 1 Number, 1 Lower, 1 Upper, AND 1 Special Character
    	String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
    	if (password == null || !password.matches(pattern)) {
            throw new RuntimeException("Password must be at least 8 characters long and " +
                                       "include uppercase, lowercase, a number, and a special character.");
        }
    }
}