package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.DemoSetupRequestDto;
import com.vimainsurance.vimaadmin.dto.DemoSetupResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IDemoSetupService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/demo-setup")
public class DemoSetupController {

    @Autowired
    private IDemoSetupService demoSetupService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<DemoSetupResponseDto>> createDemoOrg(
            @RequestBody @Valid DemoSetupRequestDto request) {
        return demoSetupService.provisionDemo(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<List<DemoSetupResponseDto>>> listDemoOrgs() {
        return demoSetupService.listDemoOrgs();
    }

    @PostMapping("/{organizationId}/revoke")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> revokeDemoAccess(@PathVariable UUID organizationId) {
        return demoSetupService.revokeDemoAccess(organizationId);
    }

    @PostMapping("/{organizationId}/extend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> extendDemoAccess(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "7") int days) {
        return demoSetupService.extendDemoAccess(organizationId, days);
    }

    @DeleteMapping("/{organizationId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> deleteDemoOrganization(@PathVariable UUID organizationId) {
        return demoSetupService.deleteDemoOrganization(organizationId);
    }
}
