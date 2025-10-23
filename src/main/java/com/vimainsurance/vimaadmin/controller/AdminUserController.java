package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.AdminUserRequestDto;
import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.PasswordChangeRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IAdminUserService;

@RestController
@RequestMapping("/api/v1/admin-users")
public class AdminUserController {

    @Autowired
    private IAdminUserService adminUserService;

    @PostMapping
    public ResponseEntity<ResponseDto<AdminUserResponseDto>> create(@RequestBody AdminUserRequestDto requestDto) {
        return adminUserService.createAdminUser(requestDto);
    }

    @PutMapping("/{username}")
    public ResponseEntity<ResponseDto<AdminUserResponseDto>> update(@PathVariable String username, @RequestBody AdminUserRequestDto requestDto) {
        return adminUserService.updateAdminUser(username, requestDto);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ResponseDto<String>> delete(@PathVariable String username) {
        return adminUserService.deleteAdminUser(username);
    }

    @GetMapping("/{username}")
    public ResponseEntity<ResponseDto<AdminUserResponseDto>> getById(@PathVariable String username) {
        return adminUserService.getAdminUserById(username);
    }

    @GetMapping
    public ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> getAll() {
        return adminUserService.getAllAdminUsers();
    }

    @PostMapping("/{username}/change-password")
    public ResponseEntity<ResponseDto<String>> changePassword(@PathVariable String username, @RequestBody PasswordChangeRequestDto requestDto) {
        return adminUserService.changePassword(username, requestDto);
    }

    @PostMapping("/{username}/reset-password")
    public ResponseEntity<ResponseDto<String>> adminChangeUserPassword(@PathVariable String username) {
        return adminUserService.adminChangeUserPassword(username);
    }

    
} 