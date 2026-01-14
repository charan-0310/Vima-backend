package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.AdminUserRequestDto;
import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.AdminUsersFilteredResponseDto;
import com.vimainsurance.vimaadmin.dto.AuthentikGroupsResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationDto;
import com.vimainsurance.vimaadmin.dto.PasswordChangeRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.RoleDto;
import org.springframework.web.multipart.MultipartFile;

public interface IAdminUserService {
    ResponseEntity<ResponseDto<String>> createAdminUser(AdminUserRequestDto requestDto);
    ResponseEntity<ResponseDto<AdminUserResponseDto>> updateAdminUser(String username, AdminUserRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> deleteAdminUser(String username);
    ResponseEntity<ResponseDto<AdminUserResponseDto>> getAdminUserById(String username);
    ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> getAllAdminUsers(int page, int rec);
    ResponseEntity<ResponseDto<AdminUsersFilteredResponseDto>> getAllAdminUsersWithFilters(String search, String role, String organization, Boolean isActive, int page, int rec, String sortBy, String sortDirection);
    ResponseEntity<ResponseDto<AuthentikGroupsResponseDto>> getRolesAndOrganizations();
    ResponseEntity<ResponseDto<List<RoleDto>>> getRoles();
    ResponseEntity<ResponseDto<List<OrganizationDto>>> getOrganizations();
    ResponseEntity<ResponseDto<String>> changePassword(String username, PasswordChangeRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> adminChangeUserPassword(String username);
    // ResponseEntity<ResponseDto<String>> uploadDocument(String username, MultipartFile file);
} 