package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.DemoSetupRequestDto;
import com.vimainsurance.vimaadmin.dto.DemoSetupResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IDemoSetupService {
    ResponseEntity<ResponseDto<DemoSetupResponseDto>> provisionDemo(DemoSetupRequestDto requestDto);
    ResponseEntity<ResponseDto<List<DemoSetupResponseDto>>> listDemoOrgs();
    ResponseEntity<ResponseDto<String>> revokeDemoAccess(UUID organizationId);
    ResponseEntity<ResponseDto<String>> extendDemoAccess(UUID organizationId, int days);

    /** Permanently removes a demo organization and related seeded data. Only when {@code is_demo_org} is true. */
    ResponseEntity<ResponseDto<String>> deleteDemoOrganization(UUID organizationId);
}
