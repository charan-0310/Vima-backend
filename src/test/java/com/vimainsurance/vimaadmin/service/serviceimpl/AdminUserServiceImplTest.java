package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.vimainsurance.vimaadmin.dto.AdminUserRequestDto;
import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.AuthentikPaginatedResponse;
import com.vimainsurance.vimaadmin.dto.PasswordChangeRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.util.AuthentikUtil;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.IdGenerator;
import com.vimainsurance.vimaadmin.util.KeyCloakUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUserServiceImplTest {

    @Mock
    private IAdminUserRepository adminUserRepository;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private IEmailService emailService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private AuthentikUtil authentikUtil;

    @Mock
    private KeyCloakUtil keyCloakUtil;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private AdminUserRequestDto requestDto;
    private AdminUser adminUser;
    private UUID userId;
    private String username;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        username = "admin1";
        requestDto = new AdminUserRequestDto();
        requestDto.setUsername(username);
        requestDto.setEmail("admin1@example.com");
        requestDto.setFullName("Admin One");
        requestDto.setRole("ADMIN");
        requestDto.setOrganizations(Collections.emptyList());
        requestDto.setIsActive(true);
        requestDto.setOauthProvider("local");
        requestDto.setOauthProviderId("localid");
        requestDto.setZohoCrmId("zohoid");
        requestDto.setLastLogin(LocalDateTime.now());

        adminUser = new AdminUser();
        adminUser.setId(userId);
        adminUser.setUsername(username);
        adminUser.setEmail("admin1@example.com");
        adminUser.setFullName("Admin One");
        adminUser.setRole("ADMIN");
        adminUser.setIsActive(true);
        adminUser.setPasswordHash("hash");
        adminUser.setOauthProvider("local");
        adminUser.setOauthProviderId("localid");
        adminUser.setZohoCrmId("zohoid");
        adminUser.setLastLogin(LocalDateTime.now());
        adminUser.setCreatedAt(LocalDateTime.now());
    }

    // ========== CREATE ADMIN USER TESTS ==========

    @Test
    void testCreateAdminUser_Success() {
        when(adminUserRepository.findByUsername(requestDto.getUsername())).thenReturn(Optional.empty());
        when(adminUserRepository.findByEmail(requestDto.getEmail())).thenReturn(Optional.empty());
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(adminUser);
        when(keyCloakUtil.createUser(anyString(), anyString(), anyString(), anyString(), any(), any(Boolean.class), nullable(String.class), anyString())).thenReturn("TempPassword123");
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.createAdminUser(requestDto);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertNotNull(response.getBody().getPayload());
        assertEquals("User created successfully", response.getBody().getPayload());
        verify(keyCloakUtil, times(1)).createUser(anyString(), anyString(), anyString(), anyString(), any(), any(Boolean.class), nullable(String.class), anyString());
    }

    @Test
    void testCreateAdminUser_UsernameExists() {
        when(adminUserRepository.findByUsername(requestDto.getUsername())).thenReturn(Optional.of(adminUser));
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.createAdminUser(requestDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Username already exists", response.getBody().getMessage());
        verify(adminUserRepository, never()).save(any());
    }

    @Test
    void testCreateAdminUser_EmailExists() {
        when(adminUserRepository.findByUsername(requestDto.getUsername())).thenReturn(Optional.empty());
        when(adminUserRepository.findByEmail(requestDto.getEmail())).thenReturn(Optional.of(adminUser));

        ResponseEntity<ResponseDto<String>> response = adminUserService.createAdminUser(requestDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email already exists", response.getBody().getMessage());
        verify(adminUserRepository, never()).save(any());
    }

    @Test
    void testCreateAdminUser_Exception() {
        when(adminUserRepository.findByUsername(requestDto.getUsername())).thenReturn(Optional.empty());
        when(adminUserRepository.findByEmail(requestDto.getEmail())).thenReturn(Optional.empty());
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(adminUser);
        doThrow(new RuntimeException("Database error")).when(keyCloakUtil).createUser(anyString(), anyString(), anyString(), anyString(), any(), any(Boolean.class), nullable(String.class), anyString());

        ResponseEntity<ResponseDto<String>> response = adminUserService.createAdminUser(requestDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to create user. Please try again.", response.getBody().getMessage());
    }

    @Test
    void testCreateAdminUser_AuthentikUsernameExists() {
        when(adminUserRepository.findByUsername(requestDto.getUsername())).thenReturn(Optional.empty());
        when(adminUserRepository.findByEmail(requestDto.getEmail())).thenReturn(Optional.empty());
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(adminUser);
        String responseBody = "{\"username\":[\"This field must be unique.\"]}";
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request", responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.charset.StandardCharsets.UTF_8))
                .when(keyCloakUtil).createUser(anyString(), anyString(), anyString(), anyString(), any(), any(Boolean.class), nullable(String.class), anyString());

        ResponseEntity<ResponseDto<String>> response = adminUserService.createAdminUser(requestDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Username must be unique", response.getBody().getMessage());
    }

    // ========== UPDATE ADMIN USER TESTS ==========

    @Test
    void testUpdateAdminUser_Success() {
        requestDto.setRole("SALES_AGENT"); // Use valid role to avoid reportingTo check
        requestDto.setEmail(adminUser.getEmail()); // Use same email to avoid email check
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(adminUserRepository.save(any())).thenReturn(adminUser);
        
        ResponseEntity<ResponseDto<AdminUserResponseDto>> response = adminUserService.updateAdminUser(username, requestDto);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(username, response.getBody().getPayload().getUsername());
    }

    @Test
    void testUpdateAdminUser_NotFound() {
        when(adminUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        
        ResponseEntity<ResponseDto<AdminUserResponseDto>> response = adminUserService.updateAdminUser(username, requestDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Admin user not found", response.getBody().getMessage());
    }

    @Test
    void testUpdateAdminUser_EmailExists() {
        requestDto.setRole("SALES_AGENT"); // Use valid role to avoid reportingTo check
        requestDto.setEmail("different@email.com"); // Set different email to trigger email check
        AdminUser existingUser = new AdminUser();
        existingUser.setEmail("different@email.com");
        existingUser.setUsername("differentuser"); // Set username to avoid null pointer
        when(adminUserRepository.findByUsername(username)).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.findByEmail("different@email.com")).thenReturn(Optional.of(existingUser));
        
        ResponseEntity<ResponseDto<AdminUserResponseDto>> response = adminUserService.updateAdminUser(username, requestDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email already exists", response.getBody().getMessage());
    }

    @Test
    void testUpdateAdminUser_UsernameExists() {
        requestDto.setRole("SALES_AGENT"); // Use valid role to avoid reportingTo check
        AdminUser existingUser = new AdminUser();
        existingUser.setUsername("differentuser");
        existingUser.setEmail("test@email.com"); // Set email to avoid null pointer
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.findByEmail(requestDto.getEmail())).thenReturn(Optional.empty());
        when(adminUserRepository.findByUsername(requestDto.getUsername())).thenReturn(Optional.of(existingUser));
        
        ResponseEntity<ResponseDto<AdminUserResponseDto>> response = adminUserService.updateAdminUser(username, requestDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Username already exists", response.getBody().getMessage());
    }

    @Test
    void testUpdateAdminUser_ReportingToNotFound() {
        requestDto.setRole("ADMIN");
        requestDto.setReportingTo("nonexistentuser");
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(adminUserRepository.findByUsername(requestDto.getReportingTo())).thenReturn(Optional.empty());
        
        ResponseEntity<ResponseDto<AdminUserResponseDto>> response = adminUserService.updateAdminUser(username, requestDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Reporting to user not found", response.getBody().getMessage());
    }

    @Test
    void testUpdateAdminUser_Exception() {
        requestDto.setRole("SALES_AGENT"); // Use valid role to avoid reportingTo check
        requestDto.setEmail(adminUser.getEmail()); // Use same email to avoid email check
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(adminUserRepository.save(any())).thenThrow(new RuntimeException("Database error"));
        
        ResponseEntity<ResponseDto<AdminUserResponseDto>> response = adminUserService.updateAdminUser(username, requestDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error Occured while updating admin user", response.getBody().getMessage());
    }

    // ========== DELETE ADMIN USER TESTS ==========

    @Test
    void testDeleteAdminUser_Success() {
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any())).thenReturn(adminUser);
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.deleteAdminUser(username);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals("Deleted successfully", response.getBody().getPayload());
    }

    @Test
    void testDeleteAdminUser_NotFound() {
        when(adminUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.deleteAdminUser(username);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Admin user not found", response.getBody().getMessage());
    }

    @Test
    void testDeleteAdminUser_Exception() {
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any())).thenThrow(new RuntimeException("Database error"));
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.deleteAdminUser(username);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error Occured while deleting admin user", response.getBody().getMessage());
    }

    // ========== GET ADMIN USER BY ID TESTS ==========

    @Test
    void testGetAdminUserById_Success() {
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        
        ResponseEntity<ResponseDto<AdminUserResponseDto>> response = adminUserService.getAdminUserById(username);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(username, response.getBody().getPayload().getUsername());
    }

    @Test
    void testGetAdminUserById_NotFound() {
        when(adminUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        
        ResponseEntity<ResponseDto<AdminUserResponseDto>> response = adminUserService.getAdminUserById(username);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Admin user not found", response.getBody().getMessage());
    }

    @Test
    void testGetAdminUserById_Exception() {
        when(adminUserRepository.findByUsername(username)).thenThrow(new RuntimeException("Database error"));
        
        ResponseEntity<ResponseDto<AdminUserResponseDto>> response = adminUserService.getAdminUserById(username);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Database error", response.getBody().getMessage());
    }

    // ========== GET ALL ADMIN USERS TESTS ==========

    @Test
    void testGetAllAdminUsers_GetAll_Success() {
        AdminUserResponseDto dto = new AdminUserResponseDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        List<AdminUserResponseDto> users = Collections.singletonList(dto);
        
        when(keyCloakUtil.getAllUsers()).thenReturn(users);
        
        ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> response = adminUserService.getAllAdminUsers(-1, -1);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(1, response.getBody().getPayload().size());
        assertEquals(1L, response.getBody().getTotalRecords());
        verify(keyCloakUtil, times(1)).getAllUsers();
    }

    @Test
    void testGetAllAdminUsers_GetAll_EmptyList() {
        when(keyCloakUtil.getAllUsers()).thenReturn(Collections.emptyList());
        
        ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> response = adminUserService.getAllAdminUsers(-1, -1);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(0, response.getBody().getPayload().size());
        assertEquals(0L, response.getBody().getTotalRecords());
        verify(keyCloakUtil, times(1)).getAllUsers();
    }

    @Test
    void testGetAllAdminUsers_Paginated_Success() {
        AdminUserResponseDto dto = new AdminUserResponseDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        List<AdminUserResponseDto> users = Collections.singletonList(dto);
        
        AuthentikPaginatedResponse<AdminUserResponseDto> paginatedResponse = new AuthentikPaginatedResponse<>();
        paginatedResponse.setResults(users);
        AuthentikPaginatedResponse.PaginationInfo paginationInfo = new AuthentikPaginatedResponse.PaginationInfo();
        paginationInfo.setCount(10);
        paginationInfo.setCurrent(1);
        paginationInfo.setTotalPages(1);
        paginatedResponse.setPagination(paginationInfo);
        
        when(keyCloakUtil.getUsers(1, 10)).thenReturn(paginatedResponse);
        
        ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> response = adminUserService.getAllAdminUsers(0, 10);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(1, response.getBody().getPayload().size());
        assertEquals(10L, response.getBody().getTotalRecords());
        verify(keyCloakUtil, times(1)).getUsers(1, 10);
    }

    @Test
    void testGetAllAdminUsers_Paginated_EmptyList() {
        AuthentikPaginatedResponse<AdminUserResponseDto> paginatedResponse = new AuthentikPaginatedResponse<>();
        paginatedResponse.setResults(Collections.emptyList());
        AuthentikPaginatedResponse.PaginationInfo paginationInfo = new AuthentikPaginatedResponse.PaginationInfo();
        paginationInfo.setCount(0);
        paginatedResponse.setPagination(paginationInfo);
        
        when(keyCloakUtil.getUsers(1, 10)).thenReturn(paginatedResponse);
        
        ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> response = adminUserService.getAllAdminUsers(0, 10);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(0, response.getBody().getPayload().size());
        assertEquals(0L, response.getBody().getTotalRecords());
    }

    @Test
    void testGetAllAdminUsers_Exception() {
        when(keyCloakUtil.getAllUsers()).thenThrow(new RuntimeException("Authentik error"));
        
        ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> response = adminUserService.getAllAdminUsers(-1, -1);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Authentik error", response.getBody().getMessage());
    }

    // ========== CHANGE PASSWORD TESTS ==========

    @Test
    void testChangePassword_Success() {
        PasswordChangeRequestDto passwordDto = new PasswordChangeRequestDto();
        passwordDto.setCurrentPassword("oldPassword");
        passwordDto.setNewPassword("newPassword123!");
        
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(adminUserRepository.save(any())).thenReturn(adminUser);
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.changePassword(username, passwordDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Current password is incorrect", response.getBody().getMessage());
    }

    @Test
    void testChangePassword_UserNotFound() {
        PasswordChangeRequestDto passwordDto = new PasswordChangeRequestDto();
        passwordDto.setCurrentPassword("oldPassword");
        passwordDto.setNewPassword("newPassword123!");
        
        when(adminUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.changePassword(username, passwordDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Admin user not found", response.getBody().getMessage());
    }

    @Test
    void testChangePassword_InvalidCurrentPassword() {
        PasswordChangeRequestDto passwordDto = new PasswordChangeRequestDto();
        passwordDto.setCurrentPassword("wrongPassword");
        passwordDto.setNewPassword("newPassword123!");
        
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.changePassword(username, passwordDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Current password is incorrect", response.getBody().getMessage());
        verify(adminUserRepository, never()).save(any());
    }

    @Test
    void testChangePassword_Exception() {
        PasswordChangeRequestDto passwordDto = new PasswordChangeRequestDto();
        passwordDto.setCurrentPassword("oldPassword");
        passwordDto.setNewPassword("newPassword123!");
        
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(adminUserRepository.save(any())).thenThrow(new RuntimeException("Database error"));
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.changePassword(username, passwordDto);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Current password is incorrect", response.getBody().getMessage());
    }

    // ========== ADMIN CHANGE USER PASSWORD TESTS ==========

    @Test
    void testAdminChangeUserPassword_Success() {
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any())).thenReturn(adminUser);
        when(emailService.sendPasswordResetEmail(anyString(), anyString(), anyString())).thenReturn(null);
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.adminChangeUserPassword(username);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Process completed successfully", response.getBody().getMessage());
        verify(adminUserRepository, times(1)).save(any());
        verify(emailService, times(1)).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testAdminChangeUserPassword_UserNotFound() {
        when(adminUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.adminChangeUserPassword(username);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Admin user not found", response.getBody().getMessage());
        verify(adminUserRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testAdminChangeUserPassword_Exception() {
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any())).thenThrow(new RuntimeException("Database error"));
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.adminChangeUserPassword(username);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Database error", response.getBody().getMessage());
    }

    @Test
    void testAdminChangeUserPassword_EmailException() {
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any())).thenReturn(adminUser);
        when(emailService.sendPasswordResetEmail(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Email service error"));
        
        ResponseEntity<ResponseDto<String>> response = adminUserService.adminChangeUserPassword(username);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Process completed successfully", response.getBody().getMessage());
        verify(adminUserRepository, times(1)).save(any());
    }

    // ========== VERIFICATION TESTS ==========

    @Test
    void testCreateAdminUser_VerifyRepositoryCalls() {
        when(adminUserRepository.findByUsername(requestDto.getUsername())).thenReturn(Optional.empty());
        when(adminUserRepository.findByEmail(requestDto.getEmail())).thenReturn(Optional.empty());
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(adminUser);
        when(keyCloakUtil.createUser(anyString(), anyString(), anyString(), anyString(), any(), any(Boolean.class), nullable(String.class), anyString())).thenReturn("TempPassword123");
        
        adminUserService.createAdminUser(requestDto);
        
        // findByUsername called twice: duplicate check (line 168) and welcome-email check (line 227)
        verify(adminUserRepository, times(2)).findByUsername(requestDto.getUsername());
        verify(adminUserRepository, times(1)).findByEmail(requestDto.getEmail());
        verify(adminUserRepository, times(1)).save(any(AdminUser.class));
        verify(keyCloakUtil, times(1)).createUser(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testUpdateAdminUser_VerifyRepositoryCalls() {
        requestDto.setRole("SALES_AGENT"); // Use valid role to avoid reportingTo check
        requestDto.setEmail(adminUser.getEmail()); // Use same email to avoid email check
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(adminUserRepository.save(any())).thenReturn(adminUser);
        
        adminUserService.updateAdminUser(username, requestDto);
        
        verify(adminUserRepository, times(1)).findByUsername(anyString());
        verify(adminUserRepository, times(1)).save(any());
    }

    @Test
    void testDeleteAdminUser_VerifyRepositoryCalls() {
        when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any())).thenReturn(adminUser);
        
        adminUserService.deleteAdminUser(username);
        
        verify(adminUserRepository, times(1)).findByUsername(username);
        verify(adminUserRepository, times(1)).save(any());
    }
} 