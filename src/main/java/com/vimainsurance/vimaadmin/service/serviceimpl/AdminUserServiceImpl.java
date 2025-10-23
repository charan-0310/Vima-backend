package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.AdminUserRequestDto;
import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.PasswordChangeRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.service.IAdminUserService;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.PasswordEncoder;
import com.vimainsurance.vimaadmin.util.PasswordGenerator;
import com.vimainsurance.vimaadmin.util.IdGenerator;

@Service
public class AdminUserServiceImpl implements IAdminUserService {
    private static final Logger logger = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private IdGenerator idGenerator;

    // @Value("${admin.default.password:ChangeMe123!}")
    // private String defaultPassword;

    @Autowired
    private IEmailService emailService;

    private AdminUserResponseDto mapToResponseDto(AdminUser user) {
        AdminUserResponseDto dto = new AdminUserResponseDto();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        dto.setOauthProvider(user.getOauthProvider());
        dto.setOauthProviderId(user.getOauthProviderId());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setAgentId(user.getAgentId());
        dto.setReportingTo(user.getReportingTo() != null ? user.getReportingTo().getUsername() : null);
        return dto;
    }

    private void mapRequestToEntity(AdminUserRequestDto dto, AdminUser user) {
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setRole(dto.getRole());
        user.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        user.setOauthProvider(dto.getOauthProvider());
        user.setOauthProviderId(dto.getOauthProviderId());
        user.setZohoCrmId(dto.getZohoCrmId());
        user.setLastLogin(dto.getLastLogin());
        user.setReportingTo(dto.getReportingTo() != null ? adminUserRepository.findByUsername(dto.getReportingTo()).orElse(null) : null);
    }

    @Override
    public ResponseEntity<ResponseDto<AdminUserResponseDto>> createAdminUser(AdminUserRequestDto requestDto) {
        logger.info("createAdminUser called");
        BaseResponse<AdminUserResponseDto> responseObj = new BaseResponse<>();
        try {
            if (adminUserRepository.findByUsername(requestDto.getUsername()).isPresent()) {
                return responseObj.render(responseObj.formErrorResponse("Username already exists"));
            }
            if (adminUserRepository.findByEmail(requestDto.getEmail()).isPresent()) {
                return responseObj.render(responseObj.formErrorResponse("Email already exists"));
            }
            AdminUser user = new AdminUser();
            mapRequestToEntity(requestDto, user);
            user.setAgentId(idGenerator.generateVimaId());
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            // if(requestDto.getPassword() != null){
            //     user.setPasswordHash(encoder.encode(requestDto.getPassword()));
            // }
            // else if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            //     user.setPasswordHash(encoder.encode(defaultPassword));
            // }
            String randomPassword = PasswordGenerator.generateRandomPassword();
            user.setPasswordHash(PasswordEncoder.encodePassword(randomPassword));
            user.setCreatedAt(LocalDateTime.now());
            AdminUser saved = adminUserRepository.save(user);
            if(adminUserRepository.findByUsername(requestDto.getUsername()).isPresent()){
                emailService.sendWelcomeEmail(requestDto.getEmail(), requestDto.getUsername(), randomPassword);
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, mapToResponseDto(saved)));
        } catch (Exception e) {
            logger.error("Error creating admin user", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<AdminUserResponseDto>> updateAdminUser(String username, AdminUserRequestDto requestDto) {
        logger.info("updateAdminUser called for username: {}", username);
        BaseResponse<AdminUserResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<AdminUser> userOpt = adminUserRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Admin user not found"));
            }
            AdminUser user = userOpt.get();
            if (!user.getEmail().equals(requestDto.getEmail()) && adminUserRepository.findByEmail(requestDto.getEmail()).isPresent()) {
                return responseObj.render(responseObj.formErrorResponse("Email already exists"));
            }
            if (!user.getUsername().equals(requestDto.getUsername()) && adminUserRepository.findByUsername(requestDto.getUsername()).isPresent()) {
                return responseObj.render(responseObj.formErrorResponse("Username already exists"));
            }
            if(Objects.equals(requestDto.getRole(),"ADMIN") && !adminUserRepository.findByUsername(requestDto.getReportingTo()).isPresent()) {
                return responseObj.render(responseObj.formErrorResponse("Reporting to user not found"));
            }
            mapRequestToEntity(requestDto, user);
            
            AdminUser saved = adminUserRepository.save(user);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, mapToResponseDto(saved)));
        } catch (Exception e) {
            logger.error("Error updating admin user", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> deleteAdminUser(String username) {
        logger.info("deleteAdminUser called for username: {}", username);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<AdminUser> userOpt = adminUserRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Admin user not found"));
            }
            AdminUser user = userOpt.get();
            user.setIsActive(false);
            adminUserRepository.save(user);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting admin user", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<AdminUserResponseDto>> getAdminUserById(String username) {
        logger.info("getAdminUserById called for username: {}", username);
        BaseResponse<AdminUserResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<AdminUser> userOpt = adminUserRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Admin user not found"));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, mapToResponseDto(userOpt.get())));
        } catch (Exception e) {
            logger.error("Error fetching admin user", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> getAllAdminUsers() {
        logger.info("getAllAdminUsers called");
        BaseResponse<List<AdminUserResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<AdminUser> users = adminUserRepository.findByIsActiveTrue();
            List<AdminUserResponseDto> dtos = new ArrayList<>();
            for (AdminUser user : users) {
                dtos.add(mapToResponseDto(user));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dtos, dtos.size()));
        } catch (Exception e) {
            logger.error("Error fetching all admin users", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> changePassword(String username, PasswordChangeRequestDto requestDto) {
        logger.info("changePassword called for username: {}", username);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<AdminUser> userOpt = adminUserRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Admin user not found"));
            }
            
            AdminUser user = userOpt.get();
            
            // Verify current password using the same method as challenge login
            if (!PasswordEncoder.matches(requestDto.getCurrentPassword(), user.getPasswordHash())) {
                return responseObj.render(responseObj.formErrorResponse("Current password is incorrect"));
            }
            
            // Validate new password
            if (requestDto.getNewPassword() == null || requestDto.getNewPassword().trim().isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("New password cannot be empty"));
            }
            
            if (requestDto.getNewPassword().length() < 8) {
                return responseObj.render(responseObj.formErrorResponse("New password must be at least 8 characters long"));
            }
            
            // Check if new password is same as current password
            if (PasswordEncoder.matches(requestDto.getNewPassword(), user.getPasswordHash())) {
                return responseObj.render(responseObj.formErrorResponse("New password must be different from current password"));
            }
            
            // Hash and save new password using the same method as challenge login
            user.setPasswordHash(PasswordEncoder.encodePassword(requestDto.getNewPassword()));
            adminUserRepository.save(user);
            
            logger.info("Password changed successfully for user: {}", username);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Password changed successfully"));
        } catch (Exception e) {
            logger.error("Error changing password for user: {}", username, e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> adminChangeUserPassword(String username) {
        logger.info("adminChangeUserPassword called for username: {}", username);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<AdminUser> userOpt = adminUserRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Admin user not found"));
            }
            
            AdminUser user = userOpt.get();
            
            String randomPassword = PasswordGenerator.generateRandomPassword();
            user.setPasswordHash(PasswordEncoder.encodePassword(randomPassword));
            user.setCreatedAt(LocalDateTime.now());
            AdminUser saved = adminUserRepository.save(user);
            
            // Send welcome email with the generated password
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), randomPassword, user.getUsername());
                logger.info("Welcome email sent successfully to: {}", user.getEmail());
            } catch (Exception emailException) {
                logger.error("Failed to send welcome email to: {}", user.getEmail(), emailException);
                // Don't fail user creation if email fails
            }
            
            logger.info("Admin password change successful for user: {}", username);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "User password changed successfully by admin"));
        } catch (Exception e) {
            logger.error("Error changing user password by admin for user: {}", username, e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
} 