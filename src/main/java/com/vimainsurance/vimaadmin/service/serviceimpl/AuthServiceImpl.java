package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.ChallengeLoginRequestDto;
import com.vimainsurance.vimaadmin.dto.ChallengeRequestDto;
import com.vimainsurance.vimaadmin.dto.ChallengeResponseDto;
import com.vimainsurance.vimaadmin.dto.LoginRequestDto;
import com.vimainsurance.vimaadmin.dto.LoginResponseDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenRequestDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.audit.PlatformAuditPublisher;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.security.LoginAttemptService;
import com.vimainsurance.vimaadmin.service.IAuthService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.JwtUtil;
import com.vimainsurance.vimaadmin.util.RSAKeyPairUtil;

@Service
public class AuthServiceImpl implements IAuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private RSAKeyPairUtil rsaKeyPairUtil;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private PlatformAuditPublisher platformAuditPublisher;

    // Database-based nonce storage (no longer using in-memory map)

    @Override
    public ResponseEntity<ResponseDto<LoginResponseDto>> login(LoginRequestDto requestDto) {
        logger.info("[correlationId:{}] login called", MDC.get("correlationId"));
        BaseResponse<LoginResponseDto> responseObj = new BaseResponse<>();
        try {
            // F-16: per-username lockout replaces the reCAPTCHA layer that previously gated this endpoint.
            // Combined with the per-IP rate limit in GlobalRateLimitFilter (5 req/min on /auth/**).
            try {
                loginAttemptService.assertNotLocked(requestDto.getUsername());
            } catch (LoginAttemptService.AccountLockedException locked) {
                logger.warn("[correlationId:{}] Login refused — account locked: {}",
                        MDC.get("correlationId"), requestDto.getUsername());
                publishAudit(
                        "admin",
                        "admin_users",
                        "LOGIN",
                        "FAILED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("username", requestDto.getUsername(), "reason", "LOCKED"));
                return responseObj.render(responseObj.formErrorResponse(locked.getMessage()));
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        requestDto.getUsername(),
                        requestDto.getPassword()
                    )
            );

            AdminUser adminUser = adminUserRepository.findByUsername(requestDto.getUsername()).orElseThrow();
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String organizationId = "";
            if (adminUser.getOrganization() != null && adminUser.getOrganization().getOrganizationId() != null) {
                organizationId = adminUser.getOrganization().getOrganizationId().toString();
            }
            String token = jwtUtil.generateToken(requestDto.getUsername(), authentication.getAuthorities().iterator().next().getAuthority(), adminUser.getEmail(), adminUser.getAgentId(), organizationId );
            String refreshToken = jwtUtil.generateRefreshToken(requestDto.getUsername(), authentication.getAuthorities().iterator().next().getAuthority());

            loginAttemptService.onSuccess(requestDto.getUsername());
            UUID orgId = adminUser.getOrganization() != null ? adminUser.getOrganization().getOrganizationId() : null;
            publishAudit(
                    "admin",
                    "admin_users",
                    "LOGIN",
                    "SUCCESS",
                    adminUser.getId().toString(),
                    orgId,
                    adminUser.getId(),
                    adminUser.getEmail(),
                    adminUser.getRole(),
                    null,
                    Map.of("flow", "PASSWORD", "username", adminUser.getUsername()));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new LoginResponseDto(token, refreshToken)));
        } catch (BadCredentialsException ex) {
            loginAttemptService.onFailure(requestDto.getUsername());
            logger.error("Bad credentials provided for user: {}", requestDto.getUsername());
            publishAudit(
                    "admin",
                    "admin_users",
                    "LOGIN",
                    "FAILED",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of("username", requestDto.getUsername(), "reason", "BAD_CREDENTIALS"));
            return responseObj.render(responseObj.formErrorResponse("Invalid username or password!!"));
        } catch (Exception ex) {
            logger.error("Error in login: {}", ex.getMessage());
            publishAudit(
                    "admin",
                    "admin_users",
                    "LOGIN",
                    "FAILED",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of("username", requestDto.getUsername(), "reason", "ERROR"));
            return responseObj.render(responseObj.formErrorResponse("Internal Server Error!!"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<RefreshTokenResponseDto>> getRefreshToken(RefreshTokenRequestDto requestDto) {
        logger.info("[correlationId:{}] getRefreshToken called", MDC.get("correlationId"));
        BaseResponse<RefreshTokenResponseDto> responseObj = new BaseResponse<>();
        try {
            String refreshToken = requestDto.getRefreshToken();

            if (!jwtUtil.validateToken(refreshToken)) {
                publishAudit(
                        "admin",
                        "admin_users",
                        "LOGIN",
                        "REFRESH_FAILED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("reason", "INVALID_REFRESH_TOKEN"));
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            String username = jwtUtil.extractUsername(refreshToken);
            AdminUser adminUser = adminUserRepository.findByUsername(username).orElseThrow();

            String organizationId = "";
            if (adminUser.getOrganization() != null && adminUser.getOrganization().getOrganizationId() != null) {
                organizationId = adminUser.getOrganization().getOrganizationId().toString();
            }
            String newAccessToken = jwtUtil.generateToken(username, adminUser.getRole(), adminUser.getEmail(), adminUser.getAgentId(), organizationId);

            UUID orgUuid = adminUser.getOrganization() != null ? adminUser.getOrganization().getOrganizationId() : null;
            publishAudit(
                    "admin",
                    "admin_users",
                    "LOGIN",
                    "REFRESH_TOKEN",
                    adminUser.getId().toString(),
                    orgUuid,
                    adminUser.getId(),
                    adminUser.getEmail(),
                    adminUser.getRole(),
                    null,
                    Map.of("username", username));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new RefreshTokenResponseDto(newAccessToken)));

        } catch (Exception ex) {
            publishAudit(
                    "admin",
                    "admin_users",
                    "LOGIN",
                    "REFRESH_FAILED",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of("reason", "ERROR"));
            return responseObj.render(responseObj.formErrorResponse(ex.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<ChallengeResponseDto>> getChallenge(ChallengeRequestDto requestDto) {
        logger.info("[correlationId:{}] getChallenge called for username", MDC.get("correlationId"));
        BaseResponse<ChallengeResponseDto> responseObj = new BaseResponse<>();
        
        try {
            String username = requestDto.getHashedUsername(); // Keep existing field name for compatibility
            if (username == null || username.trim().isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("username is required"));
            }
            
            // Find the user first
            AdminUser adminUser = adminUserRepository.findByUsername(username).orElse(null);
            if (adminUser == null) {
                logger.warn("[correlationId:{}] User not found: {}", MDC.get("correlationId"), username);
                return responseObj.render(responseObj.formErrorResponse("User not found"));
            }
            
            // Generate a UUID nonce
            String nonce = java.util.UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusMinutes(2); // 2 minutes expiration
            
            // Store nonce in the database
            adminUser.setNonce(nonce);
            adminUser.setNonceTimestamp(now);
            adminUser.setNonceExpiresAt(expiresAt);
            adminUserRepository.save(adminUser);
            
            logger.info("[correlationId:{}] Generated nonce for user: {}", MDC.get("correlationId"), username);
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new ChallengeResponseDto(nonce)));
            
        } catch (Exception ex) {
            logger.error("Error in getChallenge: {}", ex.getMessage());
            return responseObj.render(responseObj.formErrorResponse("Internal Server Error"));
        }
    }
    
    @Override
    public ResponseEntity<ResponseDto<LoginResponseDto>> challengeLogin(ChallengeLoginRequestDto requestDto) {
        logger.info("[correlationId:{}] challengeLogin called for username", MDC.get("correlationId"));
        BaseResponse<LoginResponseDto> responseObj = new BaseResponse<>();
        
        try {
            String username = requestDto.getUsername(); // Keep existing field name for compatibility
            String clientProof = requestDto.getClientproof(); // This will contain the client proof
            String hashedPassword = requestDto.getHashedPassword();

            logger.info("[correlationId:{}] Received username: {}", MDC.get("correlationId"), username);
            // recaptchaToken field on the DTO is retained for wire-compat but no longer verified.

            if (username == null || username.trim().isEmpty() ||
                hashedPassword == null || hashedPassword.trim().isEmpty()) {
                publishAudit(
                        "admin",
                        "admin_users",
                        "LOGIN",
                        "FAILED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("flow", "CHALLENGE", "reason", "VALIDATION"));
                return responseObj.render(responseObj.formErrorResponse("Username, client proof, and hashed password are required"));
            }

            // F-16: per-username lockout (replaces removed reCAPTCHA layer).
            try {
                loginAttemptService.assertNotLocked(username);
            } catch (LoginAttemptService.AccountLockedException locked) {
                logger.warn("[correlationId:{}] challengeLogin refused — account locked: {}",
                        MDC.get("correlationId"), username);
                publishAudit(
                        "admin",
                        "admin_users",
                        "LOGIN",
                        "FAILED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("username", username, "flow", "CHALLENGE", "reason", "LOCKED"));
                return responseObj.render(responseObj.formErrorResponse(locked.getMessage()));
            }

            // Find user by username
            AdminUser adminUser = adminUserRepository.findByUsername(username).orElse(null);
            if (adminUser == null) {
                logger.warn("[correlationId:{}] User not found: {}", MDC.get("correlationId"), username);
                loginAttemptService.onFailure(username);
                publishAudit(
                        "admin",
                        "admin_users",
                        "LOGIN",
                        "FAILED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("username", username, "flow", "CHALLENGE", "reason", "USER_NOT_FOUND"));
                return responseObj.render(responseObj.formErrorResponse("Invalid username"));
            }
            
            // Retrieve and validate nonce from database
            String nonce = adminUser.getNonce();
            LocalDateTime nonceExpiresAt = adminUser.getNonceExpiresAt();
            
            if (nonce == null || nonceExpiresAt == null) {
                logger.warn("[correlationId:{}] No nonce found for user: {}", MDC.get("correlationId"), username);
                publishAudit(
                        "admin",
                        "admin_users",
                        "LOGIN",
                        "FAILED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("username", username, "flow", "CHALLENGE", "reason", "NONCE_MISSING"));
                return responseObj.render(responseObj.formErrorResponse("Invalid Credentials"));
            }
            
            // Check if nonce has expired
            if (LocalDateTime.now().isAfter(nonceExpiresAt)) {
                logger.warn("[correlationId:{}] Expired nonce for user: {}", MDC.get("correlationId"), username);
                publishAudit(
                        "admin",
                        "admin_users",
                        "LOGIN",
                        "FAILED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("username", username, "flow", "CHALLENGE", "reason", "NONCE_EXPIRED"));
                return responseObj.render(responseObj.formErrorResponse("Invalid Credentials"));
            }
            
            // Clear the nonce after use (one-time use)
            adminUser.setNonce(null);
            adminUser.setNonceTimestamp(null);
            adminUser.setNonceExpiresAt(null);
            adminUserRepository.save(adminUser);
            
            // Verify bcrypt(SHA-256(password)) against stored hash
            if (!passwordEncoder.matches(hashedPassword, adminUser.getPasswordHash())) {
                logger.warn("[correlationId:{}] Invalid password for user: {}", MDC.get("correlationId"), username);
                loginAttemptService.onFailure(username);
                publishAudit(
                        "admin",
                        "admin_users",
                        "LOGIN",
                        "FAILED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("username", username, "flow", "CHALLENGE", "reason", "BAD_PASSWORD"));
                return responseObj.render(responseObj.formErrorResponse("Invalid credentials"));
            }

            // Recompute expected proof: HMAC_SHA256(hashedPassword, nonce)
            String expectedProof = computeHmacSha256(hashedPassword, nonce);
            logger.info("[correlationId:{}] Expected proof: {}", MDC.get("correlationId"), expectedProof);
            logger.info("[correlationId:{}] Client proof: {}", MDC.get("correlationId"), clientProof);

            // Compare client proof with expected proof
            if (!clientProof.equals(expectedProof)) {
                logger.warn("[correlationId:{}] Invalid client proof for user: {}", MDC.get("correlationId"), username);
                logger.warn("[correlationId:{}] Expected: {}, Received: {}", MDC.get("correlationId"), expectedProof, clientProof);
                loginAttemptService.onFailure(username);
                publishAudit(
                        "admin",
                        "admin_users",
                        "LOGIN",
                        "FAILED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("username", username, "flow", "CHALLENGE", "reason", "BAD_CLIENT_PROOF"));
                return responseObj.render(responseObj.formErrorResponse("Invalid credentials"));
            }

            loginAttemptService.onSuccess(username);

            String organizationId = "";
            if (adminUser.getOrganization() != null && adminUser.getOrganization().getOrganizationId() != null) {
                organizationId = adminUser.getOrganization().getOrganizationId().toString();
            }
            UUID orgUuid = adminUser.getOrganization() != null ? adminUser.getOrganization().getOrganizationId() : null;
            publishAudit(
                    "admin",
                    "admin_users",
                    "LOGIN",
                    "SUCCESS",
                    adminUser.getId().toString(),
                    orgUuid,
                    adminUser.getId(),
                    adminUser.getEmail(),
                    adminUser.getRole(),
                    null,
                    Map.of("flow", "CHALLENGE", "username", adminUser.getUsername()));
            String token = jwtUtil.generateToken(requestDto.getUsername(), adminUser.getRole(), adminUser.getEmail(), adminUser.getAgentId(), organizationId);
            String refreshToken = jwtUtil.generateRefreshToken(requestDto.getUsername(), adminUser.getRole());

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new LoginResponseDto(token, refreshToken)));
        } catch (BadCredentialsException ex) {
            logger.error("Bad credentials provided for user");
            String u = requestDto.getUsername();
            publishAudit(
                    "admin",
                    "admin_users",
                    "LOGIN",
                    "FAILED",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of("username", u != null ? u : "", "flow", "CHALLENGE", "reason", "BAD_CREDENTIALS"));
            return responseObj.render(responseObj.formErrorResponse("Invalid username or password!!"));
        } catch (Exception ex) {
            logger.error("Error in challengeLogin: {}", ex.getMessage());
            String u = requestDto.getUsername();
            publishAudit(
                    "admin",
                    "admin_users",
                    "LOGIN",
                    "FAILED",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of("username", u != null ? u : "", "flow", "CHALLENGE", "reason", "ERROR"));
            return responseObj.render(responseObj.formErrorResponse("Internal Server Error"));
        }
    }

    private void publishAudit(
            String schemaName,
            String tableName,
            String entityType,
            String action,
            String entityId,
            UUID organizationId,
            UUID actorUserIdOverride,
            String actorEmailOverride,
            String actorRoleOverride,
            String oldSnapshot,
            Object newSnapshot) {
        platformAuditPublisher.publish(schemaName, tableName, entityType, action, entityId, organizationId,
                actorUserIdOverride, actorEmailOverride, actorRoleOverride, oldSnapshot, newSnapshot);
    }
    
   
    

    
    /**
     * Computes HMAC-SHA256 hash using the data as the key and salt as the message
     * This matches the frontend Web Crypto API implementation
     */
    private String computeHmacSha256(String data, String salt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(data.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(salt.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error computing HMAC: {}", e.getMessage());
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }
    
    /**
     * Converts byte array to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    

}
