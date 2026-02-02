package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.vimainsurance.vimaadmin.dto.AdminUserRequestDto;
import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.AdminUsersFilteredResponseDto;
import com.vimainsurance.vimaadmin.dto.AuthentikGroupsResponseDto;
import com.vimainsurance.vimaadmin.dto.AuthentikPaginatedResponse;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.OrganizationDto;
import com.vimainsurance.vimaadmin.dto.PasswordChangeRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.RoleDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.service.IAdminUserService;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.util.AuthentikUtil;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.IdGenerator;
import com.vimainsurance.vimaadmin.util.PasswordEncoder;
import com.vimainsurance.vimaadmin.util.PasswordGenerator;

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

    @Autowired
    private AuthentikUtil authentikUtil;

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
        user.setRole(UserRole.fromValue(dto.getRole().replace("ROLE_", "")).getValue());
        user.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        user.setOauthProvider(dto.getOauthProvider());
        user.setOauthProviderId(dto.getOauthProviderId());
        user.setZohoCrmId(dto.getZohoCrmId());
        user.setLastLogin(dto.getLastLogin());
        user.setReportingTo(dto.getReportingTo() != null ? adminUserRepository.findByUsername(dto.getReportingTo()).orElse(null) : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<String>> createAdminUser(AdminUserRequestDto requestDto) {
        logger.info("[correlationId:{}] createAdminUser called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (adminUserRepository.findByUsername(requestDto.getUsername()).isPresent()) {
                return responseObj.render(responseObj.formErrorResponse("Username already exists"));
            }
            if (adminUserRepository.findByEmail(requestDto.getEmail()).isPresent()) {
                return responseObj.render(responseObj.formErrorResponse("Email already exists"));
            }
            if(requestDto.getRole() == null || requestDto.getRole().trim().isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Role is required"));
            }
            if(requestDto.getRole().contains("HR_ADMIN") && (requestDto.getOrganizations() == null || requestDto.getOrganizations().isEmpty())) {
                return responseObj.render(responseObj.formErrorResponse("Organizations are required"));
            }
            if(requestDto.getRole().contains("HR_ADMIN") && requestDto.getOrganizations() != null && requestDto.getOrganizations().size() > 1) {
                return responseObj.render(responseObj.formErrorResponse("HR_ADMIN can only be assigned to one organization"));
            }

            // create user in DB
            AdminUser user = new AdminUser();
            mapRequestToEntity(requestDto, user);
            user.setCreatedAt(LocalDateTime.now());
            AdminUser saved = adminUserRepository.save(user);
            String dateStr = saved.getCreatedAt() != null 
            ? saved.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("ddMM"))
            : "0101";
            String password = saved.getFullName().trim().toLowerCase() + "@" + dateStr;
            

            // Create user in Authentik first
            try {
                authentikUtil.createUser(
                    requestDto.getFullName(),
                    requestDto.getUsername(),
                    requestDto.getEmail(),
                    requestDto.getRole(),
                    requestDto.getOrganizations(),
                    requestDto.getIsActive() != null ? requestDto.getIsActive() : true,
                    password,
                    saved.getId().toString()
                );
                logger.info("[correlationId:{}] User created successfully in Authentik: {}", MDC.get("correlationId"), requestDto.getUsername());
            } catch (Exception e) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                logger.error("[correlationId:{}] Error creating user in Authentik: {}", MDC.get("correlationId"), e.getMessage(), e);
                return responseObj.render(responseObj.formErrorResponse("Failed to create user in Authentik: " + e.getMessage()));
            }
            
            // // Create user in local database
            // AdminUser user = new AdminUser();
            // mapRequestToEntity(requestDto, user);
            // user.setAgentId(idGenerator.generateVimaId());
            // String randomPassword = PasswordGenerator.generateRandomPassword();
            // user.setPasswordHash(PasswordEncoder.encodePassword(randomPassword));
            // user.setCreatedAt(LocalDateTime.now());
            // AdminUser saved = adminUserRepository.save(user);
            // if(adminUserRepository.findByUsername(requestDto.getUsername()).isPresent()){
            //     emailService.sendWelcomeEmail(requestDto.getEmail(), requestDto.getUsername(), randomPassword);
            // }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "User created successfully"));
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
            return responseObj.render(responseObj.formErrorResponse("Error Occured while updating admin user"));
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
            return responseObj.render(responseObj.formErrorResponse("Error Occured while deleting admin user"));
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
    public ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> getAllAdminUsers(int page, int rec) {
        logger.info("getAllAdminUsers called with page={}, rec={}", page, rec);
        BaseResponse<List<AdminUserResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<AdminUserResponseDto> dtos;
            long totalRecords;
            
            if (page == -1 && rec == -1) {
                // Get all users
                dtos = authentikUtil.getAllUsers();
                totalRecords = dtos.size();
            } else {
                // Get paginated users (convert 0-based page to 1-based for Authentik API)
                AuthentikPaginatedResponse<AdminUserResponseDto> paginatedResponse = authentikUtil.getUsers(page + 1, rec);
                dtos = paginatedResponse.getResults();
                if (paginatedResponse.getPagination() != null && paginatedResponse.getPagination().getCount() != null) {
                    totalRecords = paginatedResponse.getPagination().getCount();
                } else {
                    totalRecords = dtos != null ? dtos.size() : 0;
                }
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dtos, totalRecords));
        } catch (Exception e) {
            logger.error("Error fetching all admin users", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<AdminUsersFilteredResponseDto>> getAllAdminUsersWithFilters(
            String search, String role, String organization, Boolean isActive, int page, int rec, String sortBy, String sortDirection) {
        logger.info("getAllAdminUsersWithFilters called with search={}, role={}, organization={}, isActive={}, page={}, rec={}, sortBy={}, sortDirection={}", 
                search, role, organization, isActive, page, rec, sortBy, sortDirection);
        BaseResponse<AdminUsersFilteredResponseDto> responseObj = new BaseResponse<>();
        try {
            // Map sortBy to Authentik ordering field name
            String ordering = mapSortByToAuthentikOrdering(sortBy, sortDirection);
            
            // Prepare groups_by_name list for Authentik (supports both role and organization)
            List<String> groupsByName = new ArrayList<>();
            if (role != null && !role.trim().isEmpty()) {
                String roleName = role.trim();
                // If role doesn't start with ROLE_, add it
                if (!roleName.startsWith("ROLE_")) {
                    groupsByName.add("ROLE_" + roleName);
                } else {
                    groupsByName.add(roleName);
                }
            }
            if (organization != null && !organization.trim().isEmpty()) {
                String orgName = organization.trim();
                // If organization doesn't start with ORG_, add it
                if (!orgName.startsWith("ORG_")) {
                    groupsByName.add("ORG_" + orgName);
                } else {
                    groupsByName.add(orgName);
                }
            }
            
            // Use Authentik search for username/email/name
            // Organization names will be searched in-memory since Authentik's search doesn't include groups
            String authenticSearch = search;
            
            List<AdminUserResponseDto> dtos;
            long totalRecords;
            
            if (page == -1 && rec == -1) {
                // Get all users with filters from Authentik (fetch all pages)
                dtos = getAllUsersWithFiltersFromAuthentik(authenticSearch, isActive, ordering, groupsByName, search);
                totalRecords = dtos.size();
            } else {
                // Get paginated users with filters from Authentik (convert 0-based page to 1-based)
                AuthentikPaginatedResponse<AdminUserResponseDto> paginatedResponse = 
                    authentikUtil.getUsersWithFilters(authenticSearch, isActive, ordering, groupsByName, page + 1, rec);
                dtos = paginatedResponse.getResults();
                
                // Apply organization search filter in-memory (for partial matches)
                // This ensures we catch users whose organization names contain the search term
                if (search != null && !search.trim().isEmpty()) {
                    dtos = dtos.stream()
                            .filter(user -> matchesSearchInAllFields(user, search))
                            .collect(Collectors.toList());
                }
                
                if (paginatedResponse.getPagination() != null && paginatedResponse.getPagination().getCount() != null) {
                    totalRecords = paginatedResponse.getPagination().getCount();
                } else {
                    totalRecords = dtos != null ? dtos.size() : 0;
                }
            }
            
            // Get all filtered users for statistics calculation (without pagination)
            List<AdminUserResponseDto> allFilteredUsers = getAllUsersWithFiltersFromAuthentik(authenticSearch, isActive, ordering, groupsByName, search);
            
            // Calculate statistics based on filtered results
            Long totalUsers = (long) allFilteredUsers.size();
            Long totalVimaAdmins = calculateTotalVimaAdmins(allFilteredUsers);
            Long totalHRAdmins = calculateTotalHRAdmins(allFilteredUsers);
            Long totalOrganizations = calculateTotalOrganizations(allFilteredUsers);
            
            // Create response DTO with users and statistics
            AdminUsersFilteredResponseDto responseDto = new AdminUsersFilteredResponseDto();
            responseDto.setUsers(dtos);
            responseDto.setTotalUsers(totalUsers);
            responseDto.setTotalVimaAdmins(totalVimaAdmins);
            responseDto.setTotalOrganizations(totalOrganizations);
            responseDto.setTotalHRAdmins(totalHRAdmins);
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto, totalRecords));
        } catch (Exception e) {
            logger.error("Error fetching admin users with filters", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    /**
     * Calculate total VIMA_ADMIN users count from filtered users
     */
    private Long calculateTotalVimaAdmins(List<AdminUserResponseDto> users) {
        try {
            return users.stream()
                    .filter(user -> user.getRoles() != null && 
                            user.getRoles().stream().anyMatch(role -> role.equals("ROLE_VIMA_ADMIN")))
                    .count();
        } catch (Exception e) {
            logger.error("Error calculating total VIMA_ADMIN users", e);
            return 0L;
        }
    }

    /**
     * Calculate total HR_ADMIN users count from filtered users
     */
    private Long calculateTotalHRAdmins(List<AdminUserResponseDto> users) {
        try {
            return users.stream()
                    .filter(user -> user.getRoles() != null && 
                            user.getRoles().stream().anyMatch(role -> role.equals("ROLE_HR_ADMIN")))
                    .count();
        } catch (Exception e) {
            logger.error("Error calculating total HR_ADMIN users", e);
            return 0L;
        }
    }

    /**
     * Calculate total unique organizations count from filtered users
     */
    private Long calculateTotalOrganizations(List<AdminUserResponseDto> users) {
        try {
            return users.stream()
                    .filter(user -> user.getOrganizations() != null && !user.getOrganizations().isEmpty())
                    .flatMap(user -> user.getOrganizations().stream())
                    .distinct()
                    .count();
        } catch (Exception e) {
            logger.error("Error calculating total organizations", e);
            return 0L;
        }
    }

    /**
     * Check if user matches search term in all searchable fields including organization names
     */
    private boolean matchesSearchInAllFields(AdminUserResponseDto user, String search) {
        if (search == null || search.trim().isEmpty()) {
            return true;
        }
        
        String searchLower = search.toLowerCase();
        
        // Check standard fields (username, email, fullName, agentId) - these are already searched by Authentik
        // But we also check here to ensure consistency
        boolean matchesStandardFields = 
            (user.getUsername() != null && user.getUsername().toLowerCase().contains(searchLower)) ||
            (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower)) ||
            (user.getFullName() != null && user.getFullName().toLowerCase().contains(searchLower)) ||
            (user.getAgentId() != null && user.getAgentId().toLowerCase().contains(searchLower));
        
        // Check organization names (not searched by Authentik, so we do it here)
        boolean matchesOrganization = false;
        if (user.getOrganizations() != null && !user.getOrganizations().isEmpty()) {
            matchesOrganization = user.getOrganizations().stream()
                    .anyMatch(org -> org != null && org.toLowerCase().contains(searchLower));
        }
        
        // Return true if matches any field (standard fields OR organization)
        return matchesStandardFields || matchesOrganization;
    }

    /**
     * Get all users with filters from Authentik by fetching all pages
     */
    private List<AdminUserResponseDto> getAllUsersWithFiltersFromAuthentik(
            String search, Boolean isActive, String ordering, List<String> groupsByName, String originalSearch) {
        List<AdminUserResponseDto> allUsers = new ArrayList<>();
        int currentPage = 1;
        int pageSize = 100; // Use a reasonable page size for fetching all
        
        while (true) {
            AuthentikPaginatedResponse<AdminUserResponseDto> response = 
                authentikUtil.getUsersWithFilters(search, isActive, ordering, groupsByName, currentPage, pageSize);
            if (response.getResults() == null || response.getResults().isEmpty()) {
                break;
            }
            
            // Apply in-memory filters
            List<AdminUserResponseDto> filteredResults = response.getResults();
            
            // Apply organization search filter in-memory (for partial matches)
            // This ensures we catch users whose organization names contain the search term
            if (originalSearch != null && !originalSearch.trim().isEmpty()) {
                filteredResults = filteredResults.stream()
                        .filter(user -> matchesSearchInAllFields(user, originalSearch))
                        .collect(Collectors.toList());
            }
            
            allUsers.addAll(filteredResults);
            
            // Check if there's a next page
            if (response.getPagination() != null && response.getPagination().getNext() != null && response.getPagination().getNext() > 0) {
                currentPage = response.getPagination().getNext();
            } else {
                break;
            }
        }
        
        return allUsers;
    }

    /**
     * Map sortBy field to Authentik ordering parameter
     * Authentik uses field names like "username", "email", "name", "is_active", "date_joined", "last_login"
     * Prefix with "-" for descending order
     */
    private String mapSortByToAuthentikOrdering(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "date_joined"; // Default sort field
        }
        
        String authenticField = switch (sortBy.toLowerCase()) {
            case "username" -> "username";
            case "email" -> "email";
            case "fullname", "full_name", "name" -> "name";
            case "isactive", "is_active" -> "is_active";
            case "lastlogin", "last_login" -> "last_login";
            case "createdat", "created_at", "created" -> "date_joined";
            case "agentid", "agent_id" -> "username"; // Fallback to username if agentId not available in Authentik
            default -> "date_joined"; // Default fallback
        };
        
        // Authentik uses "-" prefix for descending order
        boolean isDesc = "desc".equalsIgnoreCase(sortDirection);
        return isDesc ? "-" + authenticField : authenticField;
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
    public ResponseEntity<ResponseDto<AuthentikGroupsResponseDto>> getRolesAndOrganizations() {
        logger.info("getRolesAndOrganizations called");
        BaseResponse<AuthentikGroupsResponseDto> responseObj = new BaseResponse<>();
        try {
            // Fetch roles and organizations separately and combine them
            List<RoleDto> roles = authentikUtil.getRoles();
            List<OrganizationDto> organizations = authentikUtil.getOrganizations();
            
            AuthentikGroupsResponseDto groups = new AuthentikGroupsResponseDto();
            groups.setRoles(roles);
            groups.setOrganizations(organizations);
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, groups));
        } catch (Exception e) {
            logger.error("Error fetching roles and organizations from Authentik", e);
            return responseObj.render(responseObj.formErrorResponse("Error Occured while fetching roles and organizations from Authentik"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<RoleDto>>> getRoles() {
        logger.info("getRoles called");
        BaseResponse<List<RoleDto>> responseObj = new BaseResponse<>();
        try {
            List<RoleDto> roles = authentikUtil.getRoles();
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, roles));
        } catch (Exception e) {
            logger.error("Error fetching roles from Authentik", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<OrganizationDto>>> getOrganizations() {
        logger.info("getOrganizations called");
        BaseResponse<List<OrganizationDto>> responseObj = new BaseResponse<>();
        try {
            List<OrganizationDto> organizations = authentikUtil.getOrganizations();
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, organizations));
        } catch (Exception e) {
            logger.error("Error fetching organizations from Authentik", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> adminChangeUserPassword(String username) {
        logger.info("[correlationId:{}] adminChangeUserPassword called for username: {}", MDC.get("correlationId"), username);
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
                logger.info("[correlationId:{}] Welcome email sent successfully to: {}", MDC.get("correlationId"), user.getEmail());
            } catch (Exception emailException) {
                logger.error("[correlationId:{}] Failed to send welcome email to: {}", MDC.get("correlationId"), user.getEmail(), emailException);
                // Don't fail user creation if email fails
            }
            
            logger.info("[correlationId:{}] Admin password change successful for user: {}", MDC.get("correlationId"), username);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "User password changed successfully by admin"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error changing user password by admin for user: {}", MDC.get("correlationId"), username, e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
} 