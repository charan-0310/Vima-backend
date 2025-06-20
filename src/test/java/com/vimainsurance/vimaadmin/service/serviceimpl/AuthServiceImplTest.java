package com.vimainsurance.vimaadmin.service.serviceimpl;

import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.vimainsurance.vimaadmin.dto.LoginRequestDto;
import com.vimainsurance.vimaadmin.dto.LoginResponseDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenRequestDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.JwtUtil;
import com.vimainsurance.vimaadmin.util.ZohoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ZohoUtil zohoUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private IAdminUserRepository adminUserRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequestDto loginRequestDto;
    private RefreshTokenRequestDto refreshTokenRequestDto;
    private AdminUser adminUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Setup login request
        loginRequestDto = new LoginRequestDto();
        loginRequestDto.setUsername("test");
        loginRequestDto.setPassword("password123");

        // Setup refresh token request
        refreshTokenRequestDto = new RefreshTokenRequestDto();
        refreshTokenRequestDto.setRefreshtoken("test-refresh-token");

        // Setup admin user
        adminUser = new AdminUser();
        adminUser.setUsername("test");
         adminUser.setEmail("test@gmail.com");
        adminUser.setRole("SALES_AGENT");

        // Setup authentication
        authentication = new UsernamePasswordAuthenticationToken(
            "testuser",
            "password",
            java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    void testLogin_Success() {
        // Mock authentication
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
            when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));

        // Mock JWT token generation
        when(jwtUtil.generateToken(anyString(), anyString(), anyString()))
            .thenReturn("test-jwt-token");
        
        // Perform login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.login(loginRequestDto);

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals("test-jwt-token", response.getBody().getPayload().getAccessToken());
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Mock authentication failure
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new RuntimeException("Invalid credentials"));

        // Perform login
        ResponseEntity<ResponseDto<LoginResponseDto>> response = authService.login(loginRequestDto);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testRefreshToken_Success() {
        // Mock token validation
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.extractUsername(anyString())).thenReturn("testuser");
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("new-access-token");

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
} 