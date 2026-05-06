package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.config.AsyncConfig;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EmailAttachment;
import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.EmailResponse;
import com.vimainsurance.vimaadmin.dto.EmployeeOnboardingResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationCreateLoginsRequestDto;
import com.vimainsurance.vimaadmin.dto.OrganizationEmployeeLoginItemDto;
import com.vimainsurance.vimaadmin.dto.OrganizationEmployeeLoginPreviewDto;
import com.vimainsurance.vimaadmin.dto.OrganizationEmployeeLoginSummaryDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.service.IOrganizationEmployeeLoginService;
import com.vimainsurance.vimaadmin.service.onboarding.EmployeeOnboardingPipeline;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.KeyCloakUtil;
import com.vimainsurance.vimaadmin.util.PrimaryEmployeeRelationshipUtil;

@Service
public class OrganizationEmployeeLoginServiceImpl implements IOrganizationEmployeeLoginService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationEmployeeLoginServiceImpl.class);

    /** Hard caps to protect SES quota and avoid abuse — kept conservative. */
    private static final int HEALTH_CARD_MAX_ATTACHMENTS = 12;
    private static final long HEALTH_CARD_MAX_BYTES_PER_FILE = 1_500_000L; // 1.5 MB
    private static final long HEALTH_CARD_MAX_BYTES_TOTAL = 5_000_000L;    // 5 MB
    private static final String HEALTH_CARD_TEMPLATE = "health-card-email";

    @Autowired
    private IDealsRepository dealsRepository;

    @Autowired
    private IOrganizationRepository organizationRepository;

    @Autowired
    private KeyCloakUtil keycloakUtil;

    @Autowired
    private EmployeeOnboardingPipeline employeeOnboardingPipeline;

    @Autowired
    private IEmailService emailService;

    @Autowired
    @Qualifier(AsyncConfig.ENROLLMENT_BULK_EXECUTOR)
    private Executor enrollmentBulkExecutor;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${keycloak.portal.client-id:}")
    private String keycloakPortalClientId;

    private record PreviewBundle(
            OrganizationEmployeeLoginPreviewDto dto,
            Map<UUID, Deals> dealByIndividualId,
            List<Deals> newUserDeals,
            String orgGroupName) {
    }

    @Override
    public ResponseEntity<ResponseDto<OrganizationEmployeeLoginPreviewDto>> previewEmployeeLogins(UUID organizationId) {
        logger.info("[correlationId:{}] previewEmployeeLogins org {}", MDC.get("correlationId"), organizationId);
        BaseResponse<OrganizationEmployeeLoginPreviewDto> responseObj = new BaseResponse<>();
        try {
            PreviewBundle bundle = buildPreviewBundle(organizationId);
            if (bundle == null) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, bundle.dto()));
        } catch (IllegalStateException e) {
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] previewEmployeeLogins failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to load employee login preview"));
        }
    }

    @Override
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "ORG_EMPLOYEE_LOGIN", action = "SUBMIT")
    public ResponseEntity<ResponseDto<EmployeeOnboardingResponseDto>> createEmployeeLogins(
            UUID organizationId,
            OrganizationCreateLoginsRequestDto request) {
        logger.info("[correlationId:{}] createEmployeeLogins org {}", MDC.get("correlationId"), organizationId);
        BaseResponse<EmployeeOnboardingResponseDto> responseObj = new BaseResponse<>();
        try {
            if (request == null) {
                return responseObj.render(responseObj.formErrorResponse("Request body is required"));
            }
            boolean allEl = Boolean.TRUE.equals(request.getAllEligible());
            boolean hasIds = request.getIndividualIds() != null && !request.getIndividualIds().isEmpty();
            if (!allEl && !hasIds) {
                return responseObj.render(responseObj.formErrorResponse("Provide individualIds or set allEligible to true"));
            }

            PreviewBundle bundle = buildPreviewBundle(organizationId);
            if (bundle == null) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            List<Deals> toProcess;
            if (allEl) {
                toProcess = new ArrayList<>(bundle.newUserDeals());
            } else {
                Set<UUID> wanted = new HashSet<>(request.getIndividualIds());
                toProcess = bundle.newUserDeals().stream()
                        .filter(d -> wanted.contains(d.getIndividualId()))
                        .collect(Collectors.toList());
                if (toProcess.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse(200,
                            "No eligible employees (NEW_USER with valid email) in the selection"));
                }
            }

            if (toProcess.isEmpty()) {
                EmployeeOnboardingResponseDto emptyDto = new EmployeeOnboardingResponseDto();
                emptyDto.setSuccessUsers(List.of());
                emptyDto.setFailedUsers(List.of());
                emptyDto.setSuccessCount(0);
                emptyDto.setFailedCount(0);
                emptyDto.setExistingKeycloakUserEmails(List.of());
                emptyDto.setBackgroundProcessing(false);
                emptyDto.setTotalEmployees(0);
                return responseObj.render(responseObj.formSuccessResponse(
                        "No primary employees need new logins.", emptyDto));
            }

            if (allEl) {
                String correlationId = MDC.get("correlationId");
                String orgGroup = bundle.orgGroupName();
                List<UUID> queuedIds = toProcess.stream()
                        .map(Deals::getIndividualId)
                        .collect(Collectors.toList());
                CompletableFuture.runAsync(
                        () -> runEmployeeLoginBackground(organizationId, queuedIds, orgGroup, correlationId),
                        enrollmentBulkExecutor);
                EmployeeOnboardingResponseDto queuedDto = new EmployeeOnboardingResponseDto();
                queuedDto.setSuccessUsers(List.of());
                queuedDto.setFailedUsers(List.of());
                queuedDto.setSuccessCount(0);
                queuedDto.setFailedCount(0);
                queuedDto.setExistingKeycloakUserEmails(List.of());
                queuedDto.setBackgroundProcessing(true);
                queuedDto.setTotalEmployees(queuedIds.size());
                return responseObj.render(responseObj.formSuccessResponse(
                        "Creating employee logins for " + queuedIds.size() + " primary employee(s) in the background.",
                        queuedDto));
            }

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);
            List<String> successUsers = new ArrayList<>();
            List<String> failedUsers = new ArrayList<>();
            List<String> existingKeycloakSuccessUsers = new ArrayList<>();

            employeeOnboardingPipeline.processDealsForOnboarding(
                    toProcess,
                    bundle.orgGroupName(),
                    successCount,
                    failedCount,
                    successUsers,
                    failedUsers,
                    existingKeycloakSuccessUsers);

            if (failedCount.get() > 0) {
                return responseObj.render(responseObj.formErrorResponse(
                        "Employee onboarding failed for some users. Failed: " + failedCount.get()
                                + ", Failed users: " + failedUsers));
            }
            EmployeeOnboardingResponseDto dto = new EmployeeOnboardingResponseDto();
            dto.setSuccessUsers(successUsers);
            dto.setFailedUsers(failedUsers);
            dto.setSuccessCount(successCount.get());
            dto.setFailedCount(failedCount.get());
            dto.setExistingKeycloakUserEmails(existingKeycloakSuccessUsers);
            dto.setBackgroundProcessing(false);
            dto.setTotalEmployees(0);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dto));
        } catch (IllegalStateException e) {
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] createEmployeeLogins failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to complete employee onboarding"));
        }
    }

    /**
     * Reload {@link Deals} inside a new transaction so Hibernate entities are not used detached across threads
     * (same pattern as {@link com.vimainsurance.vimaadmin.service.serviceimpl.EndorsementServiceImpl#runEmployeeOnboardingBackground}).
     */
    private void runEmployeeLoginBackground(
            UUID organizationId,
            List<UUID> individualIds,
            String orgGroupName,
            String correlationId) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        try {
            List<Deals> deals = transactionTemplate.execute(status -> {
                if (individualIds == null || individualIds.isEmpty()) {
                    return List.of();
                }
                List<Deals> found = dealsRepository.findByIndividualIdIn(individualIds);
                return found.stream()
                        .filter(d -> d.getOrganization() != null
                                && organizationId.equals(d.getOrganization().getOrganizationId()))
                        .filter(d -> Boolean.TRUE.equals(d.getIsPrimaryMember()))
                        .filter(d -> d.getStatus() == AccountStatus.ACTIVE)
                        .filter(PrimaryEmployeeRelationshipUtil::isPrimarySelfEmployee)
                        .collect(Collectors.toList());
            });
            if (deals == null || deals.isEmpty()) {
                logger.warn("[correlationId:{}] Background org employee onboarding: no active primary deals after reload for org {}",
                        MDC.get("correlationId"), organizationId);
                return;
            }
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);
            List<String> successUsers = new ArrayList<>();
            List<String> failedUsers = new ArrayList<>();
            List<String> existingKeycloakSuccessUsers = new ArrayList<>();
            employeeOnboardingPipeline.processDealsForOnboarding(
                    deals,
                    orgGroupName,
                    successCount,
                    failedCount,
                    successUsers,
                    failedUsers,
                    existingKeycloakSuccessUsers);
            logger.info("[correlationId:{}] Background org employee onboarding finished — success={}, failed={}",
                    MDC.get("correlationId"), successCount.get(), failedCount.get());
        } catch (Exception e) {
            logger.error("[correlationId:{}] Background org employee onboarding failed", MDC.get("correlationId"), e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "ORG_EMPLOYEE_LOGIN", action = "SUBMIT")
    public ResponseEntity<ResponseDto<String>> resendWelcomeEmail(UUID organizationId, UUID individualId) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Deals> dealOpt = dealsRepository.findByIndividualIdAndOrganizationId(individualId, organizationId);
            if (dealOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Employee not found for this organization"));
            }
            Deals deal = dealOpt.get();
            if (deal.getStatus() != AccountStatus.ACTIVE) {
                return responseObj.render(responseObj.formErrorResponse("Employee is not ACTIVE"));
            }
            if (!Boolean.TRUE.equals(deal.getIsPrimaryMember())
                    || !PrimaryEmployeeRelationshipUtil.isPrimarySelfEmployee(deal)) {
                return responseObj.render(responseObj.formErrorResponse(
                        "Login actions apply to primary employees only (SELF or EMPLOYEE with actual SELF)"));
            }
            String email = deal.getEmail() != null ? deal.getEmail().trim() : "";
            if (email.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Employee email is missing"));
            }
            if (!keycloakUtil.emailExistsInRealm(email)) {
                return responseObj.render(responseObj.formErrorResponse(400,
                        "No Keycloak user for this email — use Create login instead"));
            }
            String password = keycloakUtil.regenerateTemporaryPasswordForEmail(email);
            emailService.sendWelcomeEmail(email, deal.getFullName(), email, password);
            return responseObj.render(responseObj.formSuccessResponse("Welcome email sent", "OK"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] resendWelcomeEmail failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to resend welcome email"));
        }
    }

    @Override
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "ORG_EMPLOYEE_LOGIN", action = "SUBMIT")
    public ResponseEntity<ResponseDto<String>> sendPasswordResetEmail(UUID organizationId, UUID individualId) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Deals> dealOpt = dealsRepository.findByIndividualIdAndOrganizationId(individualId, organizationId);
            if (dealOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Employee not found for this organization"));
            }
            Deals deal = dealOpt.get();
            if (deal.getStatus() != AccountStatus.ACTIVE) {
                return responseObj.render(responseObj.formErrorResponse("Employee is not ACTIVE"));
            }
            if (!Boolean.TRUE.equals(deal.getIsPrimaryMember())
                    || !PrimaryEmployeeRelationshipUtil.isPrimarySelfEmployee(deal)) {
                return responseObj.render(responseObj.formErrorResponse(
                        "Login actions apply to primary employees only (SELF or EMPLOYEE with actual SELF)"));
            }
            String email = deal.getEmail() != null ? deal.getEmail().trim() : "";
            if (email.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Employee email is missing"));
            }
            if (!keycloakUtil.emailExistsInRealm(email)) {
                return responseObj.render(responseObj.formErrorResponse(400,
                        "No Keycloak user for this email — use Create login instead"));
            }
            String redirect = buildPortalLoginRedirectUri();
            String cid = keycloakPortalClientId != null && !keycloakPortalClientId.isBlank()
                    ? keycloakPortalClientId.trim()
                    : null;
            keycloakUtil.sendUpdatePasswordActionEmail(email, cid, redirect, 86400);
            return responseObj.render(responseObj.formSuccessResponse("Password reset email sent", "OK"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] sendPasswordResetEmail failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to send password reset email"));
        }
    }

    private String buildPortalLoginRedirectUri() {
        String base = appBaseUrl != null ? appBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/login";
    }

    @Override
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "ORG_EMPLOYEE_HEALTH_CARD_EMAIL", action = "SUBMIT")
    public ResponseEntity<ResponseDto<String>> sendHealthCardsByEmail(
            UUID organizationId,
            UUID individualId,
            MultipartFile[] attachments,
            List<String> memberNames) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (attachments == null || attachments.length == 0) {
                return responseObj.render(responseObj.formErrorResponse("At least one health card attachment is required"));
            }
            if (attachments.length > HEALTH_CARD_MAX_ATTACHMENTS) {
                return responseObj.render(responseObj.formErrorResponse(
                        "Too many attachments (" + attachments.length + ") — limit is "
                                + HEALTH_CARD_MAX_ATTACHMENTS));
            }
            long totalBytes = 0L;
            for (MultipartFile file : attachments) {
                if (file == null || file.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("One of the attachments is empty"));
                }
                long size = file.getSize();
                if (size > HEALTH_CARD_MAX_BYTES_PER_FILE) {
                    return responseObj.render(responseObj.formErrorResponse(
                            "Attachment '" + safeFileName(file.getOriginalFilename())
                                    + "' is too large (max 1.5 MB per file)"));
                }
                totalBytes += size;
                if (totalBytes > HEALTH_CARD_MAX_BYTES_TOTAL) {
                    return responseObj.render(responseObj.formErrorResponse(
                            "Total attachment size exceeds 5 MB"));
                }
                String contentType = file.getContentType();
                if (contentType != null && !contentType.toLowerCase().startsWith("image/jpeg")
                        && !contentType.toLowerCase().startsWith("image/jpg")) {
                    return responseObj.render(responseObj.formErrorResponse(
                            "Only JPEG attachments are accepted (got '" + contentType + "')"));
                }
            }

            Optional<Deals> dealOpt = dealsRepository.findByIndividualIdAndOrganizationId(individualId, organizationId);
            if (dealOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Employee not found for this organization"));
            }
            Deals deal = dealOpt.get();
            if (deal.getStatus() != AccountStatus.ACTIVE) {
                return responseObj.render(responseObj.formErrorResponse("Employee is not ACTIVE"));
            }
            if (!Boolean.TRUE.equals(deal.getIsPrimaryMember())
                    || !PrimaryEmployeeRelationshipUtil.isPrimarySelfEmployee(deal)) {
                return responseObj.render(responseObj.formErrorResponse(
                        "Health card emails are only sent to primary employees (SELF or EMPLOYEE with actual SELF)"));
            }
            String recipientEmail = deal.getEmail() != null ? deal.getEmail().trim() : "";
            if (recipientEmail.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Employee email is missing"));
            }

            List<EmailAttachment> emailAttachments = new ArrayList<>(attachments.length);
            for (int i = 0; i < attachments.length; i++) {
                MultipartFile file = attachments[i];
                String fileName = safeFileName(file.getOriginalFilename());
                if (fileName == null || fileName.isBlank()) {
                    fileName = "health-card-" + (i + 1) + ".jpg";
                }
                byte[] bytes;
                try {
                    bytes = file.getBytes();
                } catch (java.io.IOException ioe) {
                    logger.warn("[correlationId:{}] Failed to read attachment '{}': {}",
                            MDC.get("correlationId"), fileName, ioe.getMessage());
                    return responseObj.render(responseObj.formErrorResponse(
                            "Could not read attachment '" + fileName + "'"));
                }
                emailAttachments.add(EmailAttachment.builder()
                        .fileName(fileName)
                        .contentType("image/jpeg")
                        .content(bytes)
                        .build());
            }

            String orgName = deal.getOrganization() != null ? deal.getOrganization().getOrganizationName() : null;
            int attachmentCount = emailAttachments.size();
            String displayName = deal.getFullName() != null && !deal.getFullName().isBlank()
                    ? deal.getFullName().trim()
                    : "there";
            List<String> safeMemberNames = sanitizeMemberNames(memberNames, attachmentCount);

            Map<String, Object> templateVars = new HashMap<>();
            templateVars.put("userFullName", displayName);
            templateVars.put("organizationName", orgName != null ? orgName : "");
            templateVars.put("memberNames", safeMemberNames);
            templateVars.put("attachmentCount", attachmentCount);
            templateVars.put("baseUrl", appBaseUrl != null ? appBaseUrl : "");

            String subject = attachmentCount == 1
                    ? "Your Vima e-Health ID card"
                    : "Your Vima e-Health ID cards (" + attachmentCount + ")";

            EmailRequest req = EmailRequest.builder()
                    .to(recipientEmail)
                    .subject(subject)
                    .templateName(HEALTH_CARD_TEMPLATE)
                    .templateVariables(templateVars)
                    .isHtml(true)
                    .attachments(emailAttachments)
                    .build();

            EmailResponse emailResponse = emailService.sendEmailWithAttachments(req);
            if (emailResponse == null || !emailResponse.isSuccess()) {
                String detail = emailResponse != null && emailResponse.getError() != null
                        ? emailResponse.getError()
                        : "Email provider rejected the message";
                logger.warn("[correlationId:{}] Health card email failed for individual {} ({} attachments): {}",
                        MDC.get("correlationId"), individualId, attachmentCount, detail);
                return responseObj.render(responseObj.formErrorResponse(
                        "Failed to send health card email: " + detail));
            }
            logger.info("[correlationId:{}] Health card email sent for individual {} -> {} ({} attachments)",
                    MDC.get("correlationId"), individualId, redactEmail(recipientEmail), attachmentCount);
            return responseObj.render(responseObj.formSuccessResponse(
                    "Health card email sent to " + recipientEmail, "OK"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] sendHealthCardsByEmail failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to send health card email"));
        }
    }

    private static List<String> sanitizeMemberNames(List<String> raw, int attachmentCount) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(Math.min(raw.size(), attachmentCount));
        for (String name : raw) {
            if (name == null) {
                continue;
            }
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // Cap individual names to a reasonable length to avoid template abuse.
            out.add(trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed);
            if (out.size() >= attachmentCount) {
                break;
            }
        }
        return out;
    }

    private static String safeFileName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Strip path separators and any control characters; keep it short.
        String cleaned = trimmed.replaceAll("[\\\\/\\x00-\\x1F]", "_");
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }

    private static String redactEmail(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String masked = local.length() <= 2
                ? "*".repeat(local.length())
                : local.charAt(0) + "*".repeat(Math.max(1, local.length() - 2)) + local.charAt(local.length() - 1);
        return masked + domain;
    }

    /**
     * @return null when organization not found; throws IllegalStateException when Keycloak group missing
     */
    private PreviewBundle buildPreviewBundle(UUID organizationId) {
        Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
        if (orgOpt.isEmpty()) {
            return null;
        }
        Organization organization = orgOpt.get();
        String orgGroupName = "ORG_" + organization.getOrganizationName().trim().toUpperCase().replaceAll("[^A-Z0-9]", "_");
        String groupId = keycloakUtil.getGroupIdByName(orgGroupName);
        if (groupId == null || groupId.isEmpty()) {
            throw new IllegalStateException("No Keycloak organization group found for this company");
        }

        List<Deals> selfActive = listActivePrimarySelfDeals(organizationId);
        Map<UUID, Integer> healthIdCountByPrimary = countHealthIdsByPrimary(selfActive);

        List<OrganizationEmployeeLoginItemDto> items = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        List<String> uniqueForKeycloak = new ArrayList<>();
        Map<UUID, Deals> dealByIndividualId = new HashMap<>();

        for (Deals deal : selfActive) {
            OrganizationEmployeeLoginItemDto row = new OrganizationEmployeeLoginItemDto();
            row.setIndividualId(deal.getIndividualId());
            row.setFullName(deal.getFullName());
            row.setEmployeeNumber(deal.getEmployeeNumber());
            row.setHealthIdCount(healthIdCountByPrimary.getOrDefault(deal.getIndividualId(), 0));
            String primaryHid = deal.getHealthId();
            row.setPrimaryHealthId(
                    primaryHid != null && !primaryHid.isBlank() ? primaryHid.trim() : null);
            String rawEmail = deal.getEmail();
            if (rawEmail == null || rawEmail.isBlank()) {
                row.setStatus("INVALID_EMAIL");
                row.setDetail("Email is required to create a login");
                items.add(row);
                continue;
            }
            String norm = rawEmail.trim().toLowerCase();
            row.setEmail(norm);
            if (seenEmails.contains(norm)) {
                row.setStatus("DUPLICATE_IN_BATCH");
                row.setDetail("This email appears more than once for primary employees in this organization");
                items.add(row);
                continue;
            }
            seenEmails.add(norm);
            uniqueForKeycloak.add(norm);
            dealByIndividualId.put(deal.getIndividualId(), deal);
            items.add(row);
        }

        Map<String, Boolean> existsMap;
        try {
            existsMap = keycloakUtil.checkEmailsExistInRealmBatched(uniqueForKeycloak);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Keycloak is not configured; cannot preview logins");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Keycloak email preview failed", e);
            throw new IllegalStateException("Failed to check emails in Keycloak: " + e.getMessage());
        }

        int newUser = 0;
        int existing = 0;
        int invalid = 0;
        int dup = 0;
        for (OrganizationEmployeeLoginItemDto row : items) {
            if ("INVALID_EMAIL".equals(row.getStatus())) {
                invalid++;
                continue;
            }
            if ("DUPLICATE_IN_BATCH".equals(row.getStatus())) {
                dup++;
                continue;
            }
            if (row.getEmail() == null) {
                continue;
            }
            boolean inKc = Boolean.TRUE.equals(existsMap.get(row.getEmail()));
            if (inKc) {
                row.setStatus("EXISTING_KEYCLOAK_USER");
                row.setDetail(
                        "Account already exists in Keycloak; onboarding adds ROLE_EMPLOYEE and org group membership when applicable.");
                existing++;
            } else {
                row.setStatus("NEW_USER");
                row.setDetail("A new Keycloak user will be created for this email.");
                newUser++;
            }
        }

        boolean eventsOk = keycloakUtil.isRealmLoginEventsEnabled();
        if (eventsOk) {
            List<String> existingEmails = items.stream()
                    .filter(r -> "EXISTING_KEYCLOAK_USER".equals(r.getStatus()))
                    .map(OrganizationEmployeeLoginItemDto::getEmail)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!existingEmails.isEmpty()) {
                Map<String, Instant> lastLogin = keycloakUtil.getLastLoginForEmails(existingEmails);
                for (OrganizationEmployeeLoginItemDto row : items) {
                    if (row.getEmail() != null && lastLogin.containsKey(row.getEmail())) {
                        row.setLastLoginAt(lastLogin.get(row.getEmail()));
                    }
                }
            }
        }

        OrganizationEmployeeLoginSummaryDto summary = new OrganizationEmployeeLoginSummaryDto();
        summary.setTotalActive(items.size());
        summary.setWithLogin(existing);
        summary.setWithoutLogin(newUser);
        summary.setInvalid(invalid + dup);

        OrganizationEmployeeLoginPreviewDto dto = new OrganizationEmployeeLoginPreviewDto();
        dto.setEventsTrackingEnabled(eventsOk);
        dto.setSummary(summary);
        dto.setItems(items);

        List<Deals> newUserDeals = new ArrayList<>();
        for (OrganizationEmployeeLoginItemDto row : items) {
            if (!"NEW_USER".equals(row.getStatus()) || row.getIndividualId() == null) {
                continue;
            }
            Deals d = dealByIndividualId.get(row.getIndividualId());
            if (d != null) {
                newUserDeals.add(d);
            }
        }

        return new PreviewBundle(dto, dealByIndividualId, newUserDeals, orgGroupName);
    }

    private List<Deals> listActivePrimarySelfDeals(UUID organizationId) {
        List<Deals> all = dealsRepository.findByOrganizationId(organizationId);
        return all.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsPrimaryMember()))
                .filter(d -> d.getStatus() == AccountStatus.ACTIVE)
                .filter(PrimaryEmployeeRelationshipUtil::isPrimarySelfEmployee)
                .collect(Collectors.toList());
    }

    /**
     * For each primary employee, count non-blank Health IDs across self + active dependents.
     * One batched query for all dependents — keeps the preview O(1) extra DB round-trips per org.
     */
    private Map<UUID, Integer> countHealthIdsByPrimary(List<Deals> primaries) {
        Map<UUID, Integer> counts = new HashMap<>();
        if (primaries == null || primaries.isEmpty()) {
            return counts;
        }
        List<UUID> primaryIds = new ArrayList<>(primaries.size());
        for (Deals primary : primaries) {
            UUID id = primary.getIndividualId();
            if (id == null) {
                continue;
            }
            primaryIds.add(id);
            counts.put(id, hasHealthId(primary) ? 1 : 0);
        }
        if (primaryIds.isEmpty()) {
            return counts;
        }
        List<Deals> dependents = dealsRepository.findByPrimaryIndividualIdIn(primaryIds);
        for (Deals dep : dependents) {
            if (dep.getStatus() != AccountStatus.ACTIVE) {
                continue;
            }
            if (!hasHealthId(dep)) {
                continue;
            }
            Deals primary = dep.getPrimaryIndividual();
            if (primary == null || primary.getIndividualId() == null) {
                continue;
            }
            UUID primaryId = primary.getIndividualId();
            counts.merge(primaryId, 1, Integer::sum);
        }
        return counts;
    }

    private static boolean hasHealthId(Deals deal) {
        String h = deal != null ? deal.getHealthId() : null;
        return h != null && !h.isBlank();
    }
}
