package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.EnrollmentContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for enrollment token operations.
 * Handles validation and lifecycle management of enrollment invitation tokens.
 */
public interface IEnrollmentTokenService {
    
    /**
     * Validates an enrollment token and returns the enrollment context.
     * 
     * This method:
     * - Validates the token format and hash
     * - Checks token expiry
     * - Verifies invitation and window status
     * - Marks invitation as "opened" on first access
     * - Creates draft submission if it doesn't exist
     * - Returns complete enrollment context with token for subsequent API calls
     * 
     * @param rawToken Raw token from magic link URL
     * @param request HTTP request for IP extraction and rate limiting
     * @return Enrollment context with employee, window, and submission data
     * @throws com.vimainsurance.vimaadmin.exception.InvalidTokenException if token is invalid
     * @throws com.vimainsurance.vimaadmin.exception.TokenExpiredException if token has expired
     * @throws com.vimainsurance.vimaadmin.exception.TokenAlreadyUsedException if invitation is already completed
     * @throws com.vimainsurance.vimaadmin.exception.EnrollmentWindowClosedException if window is not active
     */
    EnrollmentContext validateToken(String rawToken, HttpServletRequest request);
}
