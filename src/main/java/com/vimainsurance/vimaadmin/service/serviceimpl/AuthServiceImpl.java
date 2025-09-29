package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.service.IAuthService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.JwtUtil;
import com.vimainsurance.vimaadmin.util.RSAKeyPairUtil;
import com.vimainsurance.vimaadmin.util.ZohoUtil;

@Service
public class AuthServiceImpl implements IAuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ZohoUtil zohoUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private RSAKeyPairUtil rsaKeyPairUtil;

    @Value("${google.recaptcha.secret}")
    private String secret;

    @Value("${google.recaptcha.site}")
    private String site;

    private final RestTemplate restTemplate = new RestTemplate();

    // Database-based nonce storage (no longer using in-memory map)


    // @Autowired
    // private IZohoAuthService zohoAuthService;

    @Override
    public ResponseEntity<ResponseDto<LoginResponseDto>> login(LoginRequestDto requestDto) {
        logger.info("[correlationId:{}] login called", MDC.get("correlationId"));
        BaseResponse<LoginResponseDto> responseObj = new BaseResponse<>();
        try {
            if(requestDto.getRecaptchaToken() == null || !verifyToken(requestDto.getRecaptchaToken())){
                return responseObj.render(responseObj.formErrorResponse("Invalid Captcha!!"));
            }          
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        requestDto.getUsername(),
                        requestDto.getPassword()
                    )
            );
            
            AdminUser adminUser = adminUserRepository.findByUsername(requestDto.getUsername()).orElseThrow();    
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtUtil.generateToken(requestDto.getUsername(), authentication.getAuthorities().iterator().next().getAuthority(), adminUser.getEmail(), adminUser.getAgentId());
            String refreshToken = jwtUtil.generateRefreshToken(requestDto.getUsername(), authentication.getAuthorities().iterator().next().getAuthority());
            // Initialize Zoho CRM after successful authentication

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new LoginResponseDto(token, refreshToken)));
        } catch (BadCredentialsException ex) {
            logger.error("Bad credentials provided for user: {}", requestDto.getUsername());
            return responseObj.render(responseObj.formErrorResponse("Invalid username or password!!"));
        } catch (Exception ex) {
            logger.error("Error in login: {}", ex.getMessage());
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
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            String username = jwtUtil.extractUsername(refreshToken);
            AdminUser adminUser = adminUserRepository.findByUsername(username).orElseThrow();    

            // Refresh Zoho token along with JWT token
            try {
                // zohoAuthService.refreshZohoToken();
                // zohoUtil.refreshZohoAccessToken();
            } catch (Exception e) {
                // Log the error but don't fail the token refresh
                logger.error("Failed to refresh Zoho token", e);
            }

            String newAccessToken = jwtUtil.generateToken(username, adminUser.getRole(), adminUser.getEmail(), adminUser.getAgentId());

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new RefreshTokenResponseDto(newAccessToken)));

        } catch (Exception ex) {
            return responseObj.render(responseObj.formErrorResponse(ex.getMessage()));
        }
    }

    public boolean verifyToken(String token) {
        final String projectId = "vimaadmin";
        final String expectedAction = "login"; // match frontend action
    
        String url = "https://recaptchaenterprise.googleapis.com/v1/projects/" 
                   + projectId + "/assessments?key=" + secret;
    
        try {
            // Prepare request body
            Map<String, Object> event = new HashMap<>();
            event.put("token", token);
            event.put("siteKey", site);
    
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("event", event);
    
            HttpHeaders headers = new HttpHeaders();
            // headers.setContentType(MediaType.APPLICATION_JSON);
    
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
    
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
    
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) return false;
    
            Map<String, Object> tokenProps = (Map<String, Object>) responseBody.get("tokenProperties");
    
            return tokenProps != null &&
                   Boolean.TRUE.equals(tokenProps.get("valid"));
    
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
            String hashedPassword = requestDto.getHashedPassword(); // Temporarily use recaptchaToken field for hashedPassword
            String recaptchaToken = requestDto.getRecaptchaToken(); // We'll need to add a new field later
            
            logger.info("[correlationId:{}] Received username: {}", MDC.get("correlationId"), username);
            // logger.info("[correlationId:{}] Received client proof: {}", MDC.get("correlationId"), clientProof);
            logger.info("[correlationId:{}] Received hashed password: {}", MDC.get("correlationId"), hashedPassword);
            
            if (username == null || username.trim().isEmpty() ||
                hashedPassword == null || hashedPassword.trim().isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Username, client proof, and hashed password are required"));
            }
            
            // Verify reCAPTCHA token
            if (recaptchaToken == null || !verifyToken(recaptchaToken)) {
                return responseObj.render(responseObj.formErrorResponse("Invalid Captcha!!"));
            }
            
            // Find user by username
            AdminUser adminUser = adminUserRepository.findByUsername(username).orElse(null);
            if (adminUser == null) {
                logger.warn("[correlationId:{}] User not found: {}", MDC.get("correlationId"), username);
                return responseObj.render(responseObj.formErrorResponse("Invalid username"));
            }
            
            // Retrieve and validate nonce from database
            String nonce = adminUser.getNonce();
            LocalDateTime nonceExpiresAt = adminUser.getNonceExpiresAt();
            
            if (nonce == null || nonceExpiresAt == null) {
                logger.warn("[correlationId:{}] No nonce found for user: {}", MDC.get("correlationId"), username);
                return responseObj.render(responseObj.formErrorResponse("Invalid Credentials"));
            }
            
            // Check if nonce has expired
            if (LocalDateTime.now().isAfter(nonceExpiresAt)) {
                logger.warn("[correlationId:{}] Expired nonce for user: {}", MDC.get("correlationId"), username);
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
                return responseObj.render(responseObj.formErrorResponse("Invalid credentials"));
            }

            
            String token = jwtUtil.generateToken(requestDto.getUsername(), adminUser.getRole(), adminUser.getEmail(), adminUser.getAgentId());
            String refreshToken = jwtUtil.generateRefreshToken(requestDto.getUsername(), adminUser.getRole());
            // Initialize Zoho CRM after successful authentication

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new LoginResponseDto(token, refreshToken)));
        } catch (BadCredentialsException ex) {
            logger.error("Bad credentials provided for user");
            return responseObj.render(responseObj.formErrorResponse("Invalid username or password!!"));
        } catch (Exception ex) {
            logger.error("Error in challengeLogin: {}", ex.getMessage());
            return responseObj.render(responseObj.formErrorResponse("Internal Server Error"));
        }
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
