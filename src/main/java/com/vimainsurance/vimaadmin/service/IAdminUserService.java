package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.AdminUserRequestDto;
import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.PasswordChangeRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IAdminUserService {
    ResponseEntity<ResponseDto<AdminUserResponseDto>> createAdminUser(AdminUserRequestDto requestDto);
    ResponseEntity<ResponseDto<AdminUserResponseDto>> updateAdminUser(String username, AdminUserRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> deleteAdminUser(String username);
    ResponseEntity<ResponseDto<AdminUserResponseDto>> getAdminUserById(String username);
    ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> getAllAdminUsers();
    ResponseEntity<ResponseDto<String>> changePassword(String username, PasswordChangeRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> adminChangeUserPassword(String username);
} 