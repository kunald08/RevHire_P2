package com.revhire.auth.service;

import com.revhire.auth.dto.RegisterRequest;

/**
 * Interface defining authentication operations.
 * Member A (Aswathy) will implement these.
 */
public interface AuthService {
    void register(RegisterRequest request);
    
    void changePassword(String email, String oldPassword, String newPassword);
    
    void sendForgotPasswordLink(String email);
    void resetPassword(String token, String newPassword);
    
}