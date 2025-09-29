package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.AgentTargetRequestDto;
import com.vimainsurance.vimaadmin.dto.AgentTargetResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.UsernameDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.service.AgentTargetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-targets")
@RequiredArgsConstructor
public class AgentTargetController {
    private final AgentTargetService agentTargetService;

    @PostMapping
    public ResponseEntity<String> create(@Valid @RequestBody AgentTargetRequestDto dto) {
        log.info("Create AgentTarget: {}", dto);
        return agentTargetService.create(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto<AgentTargetResponseDto>> update(@PathVariable Long id, @Valid @RequestBody AgentTargetRequestDto dto) {
        log.info("Update AgentTarget id={}", id);
        return agentTargetService.update(id, dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<AgentTargetResponseDto>> getById(@PathVariable Long id) {
        log.info("Get AgentTarget by id={}", id);
        return agentTargetService.getById(id);
    }

    @GetMapping
    public ResponseEntity<ResponseDto<List<AgentTargetResponseDto>>> getAll() {
        log.info("Get all AgentTargets");
        return agentTargetService.getAll();
    }

    @GetMapping("/agents")
    public ResponseEntity<ResponseDto<List<AdminUser>>> getAllAgentsByRole(@RequestParam(defaultValue = "SALES_AGENT") String role) {
        log.info("Get all agents by role={}", role);
        return agentTargetService.getAllAgentsByRole(role);
    }

    @GetMapping("/agents/usernames")
    public ResponseEntity<ResponseDto<List<UsernameDto>>> getAllAgentUsernamesByRole(@RequestParam(defaultValue = "SALES_AGENT") String role) {
        log.info("Get all agent usernames by role={}", role);
        return agentTargetService.getAllAgentUsernamesByRole(role);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<String>> delete(@PathVariable Long id) {
        log.info("Delete AgentTarget id={}", id);
        return agentTargetService.delete(id);
    }
} 