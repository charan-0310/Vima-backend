package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.AdminUserRequestDto;
import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationCreateHrAdminRequestDto;
import com.vimainsurance.vimaadmin.dto.OrganizationHrAdminSummaryDto;
import com.vimainsurance.vimaadmin.dto.AdminUsersFilteredResponseDto;
import com.vimainsurance.vimaadmin.dto.UserManagementStatsDto;
import com.vimainsurance.vimaadmin.dto.AuthentikGroupsResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.RoleDto;
import org.springframework.web.multipart.MultipartFile;

public interface IAdminUserService {
    ResponseEntity<ResponseDto<String>> createAdminUser(AdminUserRequestDto requestDto);

    /** Provision an HR admin for a company (DB + Keycloak org group + welcome email). */
    ResponseEntity<ResponseDto<String>> createHrAdminForOrganization(UUID organizationId, OrganizationCreateHrAdminRequestDto requestDto);

    ResponseEntity<ResponseDto<List<OrganizationHrAdminSummaryDto>>> listHrAdminsForOrganization(UUID organizationId);

    /** Remove HR admin access; user keeps employee login when present. */
    ResponseEntity<ResponseDto<String>> demoteHrAdminForOrganization(UUID organizationId, OrganizationCreateHrAdminRequestDto requestDto);
    ResponseEntity<ResponseDto<AdminUserResponseDto>> updateAdminUser(String username, AdminUserRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> deleteAdminUser(String username);
    ResponseEntity<ResponseDto<AdminUserResponseDto>> getAdminUserById(String username);
    ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> getAllAdminUsers(int page, int rec);
    ResponseEntity<ResponseDto<AdminUsersFilteredResponseDto>> getAllAdminUsersWithFilters(String search, String role, String organization, Boolean isActive, int page, int rec, String sortBy, String sortDirection);

    /** Dashboard counts for User Management (same filter semantics as {@link #getAllAdminUsersWithFilters} without pagination). */
    ResponseEntity<ResponseDto<UserManagementStatsDto>> getUserManagementStats(String search, String role, String organization, Boolean isActive, String sortBy, String sortDirection);
    ResponseEntity<ResponseDto<AuthentikGroupsResponseDto>> getRolesAndOrganizations();
    ResponseEntity<ResponseDto<List<RoleDto>>> getRoles();
    ResponseEntity<ResponseDto<List<OrganizationDto>>> getOrganizations();
    // ResponseEntity<ResponseDto<String>> uploadDocument(String username, MultipartFile file);
} 