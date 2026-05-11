package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
import com.vimainsurance.vimaadmin.security.LoginAttemptService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private IAdminUserRepository adminUserRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequestDto loginRequestDto;
    private RefreshTokenRequestDto refreshTokenRequestDto;
    private ChallengeRequestDto challengeRequestDto;
    private ChallengeLoginRequestDto challengeLoginRequestDto;
    private AdminUser adminUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Setup login request
        loginRequestDto = new LoginRequestDto();
        loginRequestDto.setUsername("test");
        loginRequestDto.setPassword("password123");
        loginRequestDto.setRecaptchaToken("valid-token");

        // Setup refresh token request
        refreshTokenRequestDto = new RefreshTokenRequestDto();
        refreshTokenRequestDto.setRefreshToken("test-refresh-token");

        // Setup challenge request
        challengeRequestDto = new ChallengeRequestDto();
        challengeRequestDto.setHashedUsername("test");

        // Setup challenge login request
        challengeLoginRequestDto = new ChallengeLoginRequestDto();
        challengeLoginRequestDto.setUsername("test");
        challengeLoginRequestDto.setClientproof("valid-proof");
        challengeLoginRequestDto.setHashedPassword("hashed-password");
        challengeLoginRequestDto.setRecaptchaToken("valid-token");

        // Setup admin user
        adminUser = new AdminUser();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("test");
        adminUser.setEmail("test@gmail.com");
        adminUser.setRole("SALES_AGENT");
        adminUser.setAgentId("AGENT001");
        adminUser.setPasswordHash("$2a$10$encodedPasswordHash");
        adminUser.setNonce("test-nonce");
        adminUser.setNonceTimestamp(LocalDateTime.now());
        adminUser.setNonceExpiresAt(LocalDateTime.now().plusMinutes(2));

        // Setup authentication
        authentication = new UsernamePasswordAuthenticationToken(
            "testuser",
            "password",
            java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    // ========== LOGIN TESTS ==========
    
    @Test
    void testLogin_Success() {
        // Mock authentication
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));

        // Mock JWT token generation
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn("test-jwt-token");
        when(jwtUtil.generateRefreshToken(anyString(), anyString()))
            .thenReturn("test-refresh-token");
        
        // Perform login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.login(loginRequestDto);

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals("test-jwt-token", response.getBody().getPayload().getAccessToken());
        assertEquals("test-refresh-token", response.getBody().getPayload().getRefreshToken());
    }

    @Test
    void testLogin_InvalidRecaptcha() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Perform login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.login(loginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid username or password!!", response.getBody().getMessage());
    }

    @Test
    void testLogin_NullRecaptcha() {
        // Set null reCAPTCHA token
        loginRequestDto.setRecaptchaToken(null);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Perform login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.login(loginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid username or password!!", response.getBody().getMessage());
    }

    @Test
    void testLogin_BadCredentials() {
        // Mock authentication failure
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Perform login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.login(loginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid username or password!!", response.getBody().getMessage());
    }

    @Test
    void testLogin_GeneralException() {
        // Mock general exception
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Perform login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.login(loginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error!!", response.getBody().getMessage());
    }

    // ========== REFRESH TOKEN TESTS ==========

    @Test
    void testRefreshToken_Success() {
        // Mock token validation
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.extractUsername(anyString())).thenReturn("testuser");
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("new-access-token");

        // Perform refresh token
        ResponseEntity<ResponseDto<RefreshTokenResponseDto>> response = authService.getRefreshToken(refreshTokenRequestDto);

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals("new-access-token", response.getBody().getPayload().getAccessToken());
    }

    @Test
    void testRefreshToken_InvalidToken() {
        // Mock invalid token
        when(jwtUtil.validateToken(anyString())).thenReturn(false);

        // Perform refresh token
        ResponseEntity<ResponseDto<RefreshTokenResponseDto>> response = authService.getRefreshToken(refreshTokenRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.RECORD_NOT_FOUND_MESSAGE, response.getBody().getMessage());
    }

    @Test
    void testRefreshToken_UserNotFound() {
        // Mock token validation but user not found
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.extractUsername(anyString())).thenReturn("testuser");
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Perform refresh token
        ResponseEntity<ResponseDto<RefreshTokenResponseDto>> response = authService.getRefreshToken(refreshTokenRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testRefreshToken_Exception() {
        // Mock exception during token validation
        when(jwtUtil.validateToken(anyString())).thenThrow(new RuntimeException("Token validation error"));

        // Perform refresh token
        ResponseEntity<ResponseDto<RefreshTokenResponseDto>> response = authService.getRefreshToken(refreshTokenRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Token validation error", response.getBody().getMessage());
    }

    // ========== CHALLENGE TESTS ==========

    @Test
    void testGetChallenge_Success() {
        // Mock user repository
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(adminUser);

        // Perform get challenge
        ResponseEntity<ResponseDto<ChallengeResponseDto>> response = authService.getChallenge(challengeRequestDto);

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertNotNull(response.getBody().getPayload().getChallenge());
        
        // Verify user was saved with nonce
        verify(adminUserRepository, times(1)).save(any(AdminUser.class));
    }

    @Test
    void testGetChallenge_EmptyUsername() {
        // Set empty username
        challengeRequestDto.setHashedUsername("");

        // Perform get challenge
        ResponseEntity<ResponseDto<ChallengeResponseDto>> response = authService.getChallenge(challengeRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("username is required", response.getBody().getMessage());
    }

    @Test
    void testGetChallenge_NullUsername() {
        // Set null username
        challengeRequestDto.setHashedUsername(null);

        // Perform get challenge
        ResponseEntity<ResponseDto<ChallengeResponseDto>> response = authService.getChallenge(challengeRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("username is required", response.getBody().getMessage());
    }

    @Test
    void testGetChallenge_UserNotFound() {
        // Mock user not found
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Perform get challenge
        ResponseEntity<ResponseDto<ChallengeResponseDto>> response = authService.getChallenge(challengeRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    void testGetChallenge_Exception() {
        // Mock repository exception
        when(adminUserRepository.findByUsername(anyString())).thenThrow(new RuntimeException("Database error"));

        // Perform get challenge
        ResponseEntity<ResponseDto<ChallengeResponseDto>> response = authService.getChallenge(challengeRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().getMessage());
    }

    // ========== CHALLENGE LOGIN TESTS ==========

    @Test
    void testChallengeLogin_Success() {
        // Mock user repository
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(adminUser);
        
        // Mock password verification
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        
        // Mock JWT token generation
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn("test-jwt-token");
        when(jwtUtil.generateRefreshToken(anyString(), anyString()))
            .thenReturn("test-refresh-token");

        challengeLoginRequestDto.setClientproof(computeHmacSha256("hashed-password", "test-nonce"));

        // Perform challenge login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.challengeLogin(challengeLoginRequestDto);

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
    }

    @Test
    void testChallengeLogin_InvalidRecaptcha() {
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(adminUser);

        // Perform challenge login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.challengeLogin(challengeLoginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }

    @Test
    void testChallengeLogin_MissingFields() {
        // Set empty fields
        challengeLoginRequestDto.setUsername("");
        challengeLoginRequestDto.setHashedPassword("");

        // Perform challenge login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.challengeLogin(challengeLoginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Username, client proof, and hashed password are required", response.getBody().getMessage());
    }

    @Test
    void testChallengeLogin_UserNotFound() {
        // Mock user not found
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Perform challenge login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.challengeLogin(challengeLoginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid username", response.getBody().getMessage());
    }

    @Test
    void testChallengeLogin_NoNonce() {
        // Mock user with no nonce
        adminUser.setNonce(null);
        adminUser.setNonceExpiresAt(null);
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));

        // Perform challenge login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.challengeLogin(challengeLoginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid Credentials", response.getBody().getMessage());
    }

    @Test
    void testChallengeLogin_ExpiredNonce() {
        // Mock user with expired nonce
        adminUser.setNonceExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));

        // Perform challenge login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.challengeLogin(challengeLoginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid Credentials", response.getBody().getMessage());
    }

    @Test
    void testChallengeLogin_InvalidPassword() {
        // Mock user repository
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(adminUser);
        
        // Mock invalid password
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Perform challenge login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.challengeLogin(challengeLoginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }

    @Test
    void testChallengeLogin_InvalidClientProof() {
        // Mock user repository
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(adminUser);
        
        // Mock password verification
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        
        // Set invalid client proof
        challengeLoginRequestDto.setClientproof("invalid-proof");

        // Perform challenge login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.challengeLogin(challengeLoginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }

    @Test
    void testChallengeLogin_Exception() {
        // Mock repository exception
        when(adminUserRepository.findByUsername(anyString())).thenThrow(new RuntimeException("Database error"));

        // Perform challenge login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.challengeLogin(challengeLoginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().getMessage());
    }

    private String computeHmacSha256(String data, String salt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(data.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(salt.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute HMAC for test setup", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

} 