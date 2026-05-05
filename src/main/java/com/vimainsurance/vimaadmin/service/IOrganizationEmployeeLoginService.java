package com.vimainsurance.vimaadmin.service;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.EmployeeOnboardingResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationCreateLoginsRequestDto;
import com.vimainsurance.vimaadmin.dto.OrganizationEmployeeLoginPreviewDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IOrganizationEmployeeLoginService {

    ResponseEntity<ResponseDto<OrganizationEmployeeLoginPreviewDto>> previewEmployeeLogins(UUID organizationId);

    ResponseEntity<ResponseDto<EmployeeOnboardingResponseDto>> createEmployeeLogins(
            UUID organizationId,
            OrganizationCreateLoginsRequestDto request);

    ResponseEntity<ResponseDto<String>> resendWelcomeEmail(UUID organizationId, UUID individualId);

    ResponseEntity<ResponseDto<String>> sendPasswordResetEmail(UUID organizationId, UUID individualId);
}
