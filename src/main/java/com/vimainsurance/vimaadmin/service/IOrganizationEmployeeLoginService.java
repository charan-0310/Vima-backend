package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Email the supplied JPEG health-card attachments to the active primary employee identified by
     * {@code individualId} within {@code organizationId}. Recipient email is always resolved from
     * the database (never trusted from the client).
     *
     * @param organizationId tenant org
     * @param individualId   primary employee whose family the cards belong to
     * @param attachments    JPEG files (1..MAX) rendered client-side from the existing HealthCard component
     * @param memberNames    optional friendly member names to render in the email body, in attachment order
     * @return success response with recipient email + attachment count, or error
     */
    ResponseEntity<ResponseDto<String>> sendHealthCardsByEmail(
            UUID organizationId,
            UUID individualId,
            MultipartFile[] attachments,
            List<String> memberNames);
}
