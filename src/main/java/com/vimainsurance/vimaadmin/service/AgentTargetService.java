package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.AgentTargetRequestDto;
import com.vimainsurance.vimaadmin.dto.AgentTargetResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.UsernameDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;

public interface AgentTargetService {
    ResponseEntity<String> create(AgentTargetRequestDto dto);
    ResponseEntity<ResponseDto<AgentTargetResponseDto>> update(Long id, AgentTargetRequestDto dto);
    ResponseEntity<ResponseDto<AgentTargetResponseDto>> getById(Long id);
    ResponseEntity<ResponseDto<List<AgentTargetResponseDto>>> getAll();
    ResponseEntity<ResponseDto<String>> delete(Long id);
    ResponseEntity<ResponseDto<List<AdminUser>>> getAllAgentsByRole(String role);
    ResponseEntity<ResponseDto<List<UsernameDto>>> getAllAgentUsernamesByRole(String role);
} 