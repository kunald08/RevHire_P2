package com.revhire.auth.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Global Security Auditor. 
 * This class automatically catches all login successes and failures 
 * across the entire application without needing to modify your Service logic.
 */
@Component
public class SecurityAuditLogger {

    private static final Logger logger = LogManager.getLogger(SecurityAuditLogger.class);

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        logger.info("SECURITY AUDIT [SUCCESS]: User '{}' logged in successfully.", username);
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        // Principal might be the username String or a UserDetails object depending on the provider
        Object principal = event.getAuthentication().getPrincipal();
        String username = principal.toString();
        
        logger.warn("SECURITY AUDIT [FAILURE]: Failed login attempt for user: {} | Reason: Invalid Credentials", username);
    }
}