package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.DemoSetupRequestDto;
import com.vimainsurance.vimaadmin.dto.DemoSetupResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.ClaimSettlement;
import com.vimainsurance.vimaadmin.entity.CostSharingRule;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EmployeePolicyMap;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.InsuranceProvider;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.entity.PremiumRateTable;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;
import com.vimainsurance.vimaadmin.enums.ClaimCategory;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.ClaimType;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.enums.CoverageCategory;
import com.vimainsurance.vimaadmin.enums.EmployerShareType;
import com.vimainsurance.vimaadmin.enums.Industry;
import com.vimainsurance.vimaadmin.enums.MemberType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.PricingModel;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.enums.Relationship;
import com.vimainsurance.vimaadmin.enums.SubmissionSource;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.repository.IClaimSettlementRepository;
import com.vimainsurance.vimaadmin.repository.ICostSharingRuleRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEmployeePolicyMapRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.repository.IPremiumRateTableRepository;
import com.vimainsurance.vimaadmin.service.IDemoSetupService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.IdGenerator;
import com.vimainsurance.vimaadmin.util.KeyCloakUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class DemoSetupServiceImpl implements IDemoSetupService {

    private static final Logger logger = LoggerFactory.getLogger(DemoSetupServiceImpl.class);
    private static final int DEFAULT_EXPIRY_DAYS = 7;
    private static final String DEMO_ACCOUNT_TYPE = "demo";

    @Autowired
    private IOrganizationRepository organizationRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private KeyCloakUtil keyCloakUtil;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private IEnrollmentWindowsRepository enrollmentWindowsRepository;

    @Autowired
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;

    @Autowired
    private IDealsRepository dealsRepository;

    @Autowired
    private IPolicyRepository policyRepository;

    @Autowired
    private IPremiumRateTableRepository premiumRateTableRepository;

    @Autowired
    private IInsuranceProviderRepository insuranceProviderRepository;

    @Autowired
    private DemoSetupEmailNotifier demoSetupEmailNotifier;

    @Autowired
    private ICostSharingRuleRepository costSharingRuleRepository;

    @Autowired
    private IEmployeePolicyMapRepository employeePolicyMapRepository;

    @Autowired
    private IClaimRepository claimRepository;

    @Autowired
    private IClaimSettlementRepository claimSettlementRepository;

    @Autowired
    private Environment environment;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "organizations", entityType = "DEMO_SETUP", action = "PROVISION")
    public ResponseEntity<ResponseDto<DemoSetupResponseDto>> provisionDemo(DemoSetupRequestDto requestDto) {
        BaseResponse<DemoSetupResponseDto> responseObj = new BaseResponse<>();
        try {
            String normalizedEmail = normalizeEmail(requestDto.getClientEmail());
            String orgName = requestDto.getOrgName().trim();
            logDemoSetupPhase("START", null, orgName, normalizedEmail, requestDto.isForceNew());
            List<Organization> sameNameOrgs = organizationRepository.findAllByOrganizationName(orgName);
            if (!sameNameOrgs.isEmpty()) {
                boolean nameConflict = sameNameOrgs.stream().anyMatch(o ->
                        !Boolean.TRUE.equals(o.getIsDemoOrg()) || requestDto.isForceNew());
                if (nameConflict) {
                    return responseObj.render(responseObj.formErrorResponse(
                            "An organization with this name already exists. If you are reusing demo orgs, uncheck \"Create fresh org\" or pick a unique name."));
                }
            }

            Organization organization;
            boolean reused = false;
            if (!requestDto.isForceNew()) {
                try {
                    organization = findReusableDemoOrg().orElse(null);
                } catch (Exception ex) {
                    throw new RuntimeException(
                            "Could not evaluate reusable demo org. Please enable 'Create fresh org' and try again.",
                            ex);
                }
                if (organization != null) {
                    reused = true;
                    reuseDemoOrg(organization, orgName);
                } else {
                    organization = createNewDemoOrg(orgName, normalizedEmail);
                }
            } else {
                organization = createNewDemoOrg(orgName, normalizedEmail);
            }

            logDemoSetupPhase("AFTER_RESOLVE_ORG", organization.getOrganizationId(), orgName, normalizedEmail, requestDto.isForceNew());
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS);
            deactivateExistingDemoUsers(organization);
            AdminUser demoAdminUser = upsertDemoAdminUser(organization, normalizedEmail, expiresAt);
            logDemoSetupPhase("AFTER_UPSERT_DEMO_ADMIN", organization.getOrganizationId(), orgName, normalizedEmail, requestDto.isForceNew());
            if (!Boolean.TRUE.equals(reused)) {
                seedDemoPoliciesAndRateCards(organization);
                logDemoSetupPhase("AFTER_SEED_POLICIES", organization.getOrganizationId(), orgName, normalizedEmail, requestDto.isForceNew());
                seedDemoEmployeesAndEnrollment(organization, demoAdminUser, normalizedEmail);
                logDemoSetupPhase("AFTER_SEED_EMPLOYEES", organization.getOrganizationId(), orgName, normalizedEmail, requestDto.isForceNew());
                organization.setDemoSeedCompletedAt(LocalDateTime.now());
                organizationRepository.save(organization);
            } else {
                updatePrimarySeedEmployeeEmail(organization.getOrganizationId(), normalizedEmail);
                ensureEmployeePolicyMapping(organization.getOrganizationId());
                logDemoSetupPhase("AFTER_REUSE_SEED_TOUCHUP", organization.getOrganizationId(), orgName, normalizedEmail, requestDto.isForceNew());
            }
            ensureDemoHealthIds(organization.getOrganizationId());
            seedDemoClaimsIfAbsent(organization, demoAdminUser);
            UUID portalEmployeeIndividualId = resolveDemoPortalIndividualId(organization.getOrganizationId());
            logDemoSetupPhase("BEFORE_KEYCLOAK", organization.getOrganizationId(), orgName, normalizedEmail, requestDto.isForceNew());
            String tempPassword = createDemoKeycloakUser(organization, normalizedEmail, expiresAt, portalEmployeeIndividualId);
            logDemoSetupPhase("AFTER_KEYCLOAK", organization.getOrganizationId(), orgName, normalizedEmail, requestDto.isForceNew());

            organization.setDemoLastAssignedEmail(normalizedEmail);
            organization.setDemoExpiresAt(expiresAt);
            organizationRepository.save(organization);

            demoSetupEmailNotifier.sendDemoWelcomeEmailAsync(
                    normalizedEmail,
                    organization.getOrganizationName(),
                    tempPassword,
                    expiresAt);

            DemoSetupResponseDto response = DemoSetupResponseDto.builder()
                    .organizationId(organization.getOrganizationId())
                    .organizationName(organization.getOrganizationName())
                    .clientEmail(normalizedEmail)
                    .status("ACTIVE")
                    .expiresAt(expiresAt)
                    .createdAt(organization.getCreatedAt())
                    .reused(reused)
                    .build();
            logDemoSetupPhase("SUCCESS", organization.getOrganizationId(), orgName, normalizedEmail, requestDto.isForceNew());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, response));
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            logger.error(
                    "[correlationId:{}] DEMO_SETUP failed rootType={} rootMessage={} outerMessage={}",
                    MDC.get("correlationId"),
                    root.getClass().getName(),
                    root.getMessage(),
                    e.getMessage(),
                    e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    private void logDemoSetupPhase(
            String phase, UUID organizationId, String orgName, String normalizedClientEmail, boolean forceNew) {
        String emailTag = normalizedClientEmail == null || !normalizedClientEmail.contains("@")
                ? "(none)"
                : "***@" + normalizedClientEmail.substring(normalizedClientEmail.indexOf('@') + 1);
        logger.info(
                "[correlationId:{}] DEMO_SETUP phase={} orgName={} orgId={} clientEmail={} forceNew={}",
                MDC.get("correlationId"),
                phase,
                orgName,
                organizationId,
                emailTag,
                forceNew);
    }

    @Override
    public ResponseEntity<ResponseDto<List<DemoSetupResponseDto>>> listDemoOrgs() {
        BaseResponse<List<DemoSetupResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<DemoSetupResponseDto> payload = organizationRepository.findByIsDemoOrgTrueOrderByDemoCreatedAtDesc().stream()
                    .map(this::toResponseDto)
                    .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, payload));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Failed to list demo orgs: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "organizations", entityType = "DEMO_SETUP", action = "REVOKE_ACCESS")
    public ResponseEntity<ResponseDto<String>> revokeDemoAccess(UUID organizationId) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Organization organization = organizationRepository.findByOrganizationId(organizationId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            List<AdminUser> activeDemoUsers = adminUserRepository.findByOrganizationAndIsDemoUserTrueAndIsActiveTrue(organization);
            for (AdminUser demoUser : activeDemoUsers) {
                keyCloakUtil.disableUserByEmail(demoUser.getEmail());
                demoUser.setIsActive(false);
                adminUserRepository.save(demoUser);
            }
            organization.setDemoExpiresAt(LocalDateTime.now());
            organizationRepository.save(organization);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Demo access revoked"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Failed to revoke demo access: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "organizations", entityType = "DEMO_SETUP", action = "EXTEND_ACCESS")
    public ResponseEntity<ResponseDto<String>> extendDemoAccess(UUID organizationId, int days) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            int extensionDays = days > 0 ? days : DEFAULT_EXPIRY_DAYS;
            Organization organization = organizationRepository.findByOrganizationId(organizationId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            LocalDateTime newExpiry = LocalDateTime.now().plusDays(extensionDays);
            List<AdminUser> demoUsers = adminUserRepository.findByOrganizationAndIsDemoUserTrue(organization);
            for (AdminUser demoUser : demoUsers) {
                keyCloakUtil.enableUserByEmail(demoUser.getEmail());
                keyCloakUtil.setUserAttribute(demoUser.getEmail(), "demo_expires_at", newExpiry.toString());
                demoUser.setIsActive(true);
                demoUser.setDemoExpiresAt(newExpiry);
                adminUserRepository.save(demoUser);
            }
            organization.setDemoExpiresAt(newExpiry);
            organizationRepository.save(organization);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Demo access extended"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Failed to extend demo access: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "organizations", entityType = "DEMO_SETUP", action = "DELETE_ORG")
    public ResponseEntity<ResponseDto<String>> deleteDemoOrganization(UUID organizationId) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Organization organization = organizationRepository.findByOrganizationId(organizationId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            if (!Boolean.TRUE.equals(organization.getIsDemoOrg())) {
                return responseObj.render(responseObj.formErrorResponse("Only demo organizations can be deleted with this action."));
            }
            List<AdminUser> orgAdmins = adminUserRepository.findByOrganization_OrganizationId(organizationId);
            for (AdminUser admin : orgAdmins) {
                if (!Boolean.TRUE.equals(admin.getIsDemoUser())) {
                    return responseObj.render(responseObj.formErrorResponse(
                            "This organization has non-demo admin users; refusing delete. Remove them first or use a different cleanup path."));
                }
            }
            purgeDemoOrganizationData(organizationId);
            List<UUID> demoAdminIds = orgAdmins.stream().map(AdminUser::getId).collect(Collectors.toList());
            if (!demoAdminIds.isEmpty()) {
                detachDemoAdminsFromEnrollmentReferences(demoAdminIds);
            }
            for (AdminUser demoUser : orgAdmins) {
                keyCloakUtil.disableUserByEmail(demoUser.getEmail());
            }
            adminUserRepository.deleteAll(orgAdmins);
            organizationRepository.delete(organization);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Demo organization deleted"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Failed to delete demo org: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    /**
     * Best-effort hard delete of CPC (and related) rows for a single organization. Order respects common FKs.
     */
    private void purgeDemoOrganizationData(UUID organizationId) {
        executeNativeUpdate("DELETE FROM cpc.employee_policy_map WHERE organization_id = :org", organizationId);

        executeNativeUpdate("""
                DELETE FROM cpc.cd_transaction_documents
                WHERE transaction_id IN (
                    SELECT transaction_id FROM cpc.cd_balance_transactions WHERE organization_id = :org)
                """, organizationId);
        executeNativeUpdate("DELETE FROM cpc.cd_balance_transactions WHERE organization_id = :org", organizationId);

        try {
            executeNativeUpdate("DELETE FROM claims.claims WHERE organization_id = :org", organizationId);
        } catch (Exception ex) {
            logger.debug("[correlationId:{}] claims purge skipped: {}", MDC.get("correlationId"), ex.getMessage());
        }

        executeNativeUpdate("""
                UPDATE cpc.customers
                SET enrollment_submission_id = NULL, enrollment_window_id = NULL
                WHERE organization_id = :org
                """, organizationId);

        executeNativeUpdate("""
                DELETE FROM cpc.nominees n
                USING cpc.enrollment_submissions s
                WHERE n.submission_id = s.id
                  AND (
                      s.enrollment_window_id IN (SELECT id FROM cpc.enrollment_windows WHERE organization_id = :org)
                      OR s.employee_id IN (SELECT individual_id FROM cpc.customers WHERE organization_id = :org)
                  )
                """, organizationId);

        executeNativeUpdate("""
                UPDATE cpc.enrollment_submissions SET reviewed_by = NULL
                WHERE enrollment_window_id IN (SELECT id FROM cpc.enrollment_windows WHERE organization_id = :org)
                   OR employee_id IN (SELECT individual_id FROM cpc.customers WHERE organization_id = :org)
                """, organizationId);

        executeNativeUpdate("""
                DELETE FROM cpc.enrollment_submissions
                WHERE enrollment_window_id IN (SELECT id FROM cpc.enrollment_windows WHERE organization_id = :org)
                   OR employee_id IN (SELECT individual_id FROM cpc.customers WHERE organization_id = :org)
                """, organizationId);

        executeNativeUpdate("""
                DELETE FROM cpc.enrollment_invitations
                WHERE enrollment_window_id IN (SELECT id FROM cpc.enrollment_windows WHERE organization_id = :org)
                   OR employee_id IN (SELECT individual_id FROM cpc.customers WHERE organization_id = :org)
                """, organizationId);

        executeNativeUpdate("""
                DELETE FROM cpc.deal_endorsements de
                USING cpc.endorsements e
                WHERE de.endorsement_id = e.endorsement_id AND e.organization_id = :org
                """, organizationId);

        executeNativeUpdate("DELETE FROM cpc.endorsements WHERE organization_id = :org", organizationId);

        executeNativeUpdate("DELETE FROM cpc.enrollment_windows WHERE organization_id = :org", organizationId);

        executeNativeUpdate("""
                DELETE FROM cpc.nominees
                WHERE policy_id IN (SELECT policy_id FROM cpc.policies WHERE organization_id = :org)
                """, organizationId);

        executeNativeUpdate("DELETE FROM cpc.premium_rate_tables WHERE organization_id = :org", organizationId);

        executeNativeUpdate("""
                UPDATE cpc.policies SET document_id = NULL, cd_account_id = NULL WHERE organization_id = :org
                """, organizationId);
        executeNativeUpdate("DELETE FROM cpc.policies WHERE organization_id = :org", organizationId);

        executeNativeUpdate("""
                DELETE FROM cpc.customers c
                USING cpc.customers p
                WHERE c.primary_individual_id = p.individual_id
                  AND p.organization_id = :org
                """, organizationId);
        executeNativeUpdate("DELETE FROM cpc.customers WHERE organization_id = :org", organizationId);

        executeNativeUpdate("DELETE FROM admin.feature_flag_companies WHERE organization_id = :org", organizationId);

        executeNativeUpdate("DELETE FROM cpc.cost_sharing_rules WHERE company_id = :org", organizationId);

        executeNativeUpdate("DELETE FROM cpc.cd_accounts WHERE organization_id = :org", organizationId);

        entityManager.flush();
    }

    private void executeNativeUpdate(String sql, UUID organizationId) {
        entityManager.createNativeQuery(sql).setParameter("org", organizationId).executeUpdate();
    }

    /**
     * Demo HR users may appear as {@code enrollment_windows.created_by} on other organizations' windows
     * (e.g. after org switches or manual QA). Clear FKs before removing {@code admin_users} rows.
     */
    private void detachDemoAdminsFromEnrollmentReferences(List<UUID> demoAdminIds) {
        entityManager.createNativeQuery(
                        "UPDATE cpc.enrollment_submissions SET reviewed_by = NULL WHERE reviewed_by IN (:ids)")
                .setParameter("ids", demoAdminIds)
                .executeUpdate();
        UUID fallbackCreator = adminUserRepository.findFirstPlatformAdminIdExcluding(demoAdminIds)
                .orElseThrow(() -> new RuntimeException(
                        "Cannot delete demo admins: enrollment windows still need a creator, and no SUPER_ADMIN/ADMIN/VIMA_ADMIN "
                                + "user exists outside this delete set to reassign. Add or keep a platform admin user."));
        entityManager.createNativeQuery(
                        "UPDATE cpc.enrollment_windows SET created_by = :fallback WHERE created_by IN (:ids)")
                .setParameter("fallback", fallbackCreator)
                .setParameter("ids", demoAdminIds)
                .executeUpdate();
    }

    private Optional<Organization> findReusableDemoOrg() {
        Optional<Organization> candidate = organizationRepository.findReusableDemoOrgForUpdate(LocalDateTime.now());
        if (candidate.isEmpty()) {
            return Optional.empty();
        }
        List<AdminUser> activeUsers = adminUserRepository.findByOrganizationAndIsDemoUserTrueAndIsActiveTrue(candidate.get());
        return activeUsers.isEmpty() ? candidate : Optional.empty();
    }

    private void reuseDemoOrg(Organization organization, String newOrgName) {
        organization.setOrganizationName(newOrgName);
        organization.setUpdatedAt(LocalDateTime.now());
        LocalDateTime cutoff = organization.getDemoSeedCompletedAt() != null
                ? organization.getDemoSeedCompletedAt()
                : organization.getDemoCreatedAt();
        if (cutoff != null) {
            resetClientMutations(organization.getOrganizationId(), cutoff);
            ensureSeedWindowClosed(organization.getOrganizationId(), cutoff);
        }
        organizationRepository.save(organization);
    }

    private Organization createNewDemoOrg(String orgName, String clientEmail) {
        Organization org = new Organization();
        org.setOrganizationName(orgName);
        org.setIndustry(Industry.IT_SERVICES);
        org.setPanNumber("DEMOP1234H");
        org.setGstin("27DEMOP1234H1Z5");
        org.setStatus("ACTIVE");
        org.setRegisteredAddress("Demo Office, Bangalore 560001");
        org.setPrimaryContactName("Demo Contact");
        org.setPrimaryContactEmail(clientEmail);
        org.setPrimaryContactPhone("9000000000");
        org.setIsDemoOrg(true);
        org.setDemoCreatedAt(LocalDateTime.now());
        Organization savedOrg = organizationRepository.save(org);
        seedDefaultCostSharingRules(savedOrg.getOrganizationId());

        String orgGroupName = "ORG_" + sanitizeName(orgName);
        keyCloakUtil.createGroup(orgGroupName, java.util.Map.of("organization_id", List.of(savedOrg.getOrganizationId().toString())));
        return savedOrg;
    }

    private void deactivateExistingDemoUsers(Organization organization) {
        List<AdminUser> activeUsers = adminUserRepository.findByOrganizationAndIsDemoUserTrueAndIsActiveTrue(organization);
        for (AdminUser activeUser : activeUsers) {
            keyCloakUtil.disableUserByEmail(activeUser.getEmail());
            activeUser.setIsActive(false);
            adminUserRepository.save(activeUser);
        }
    }

    /**
     * Individual id of the seeded primary employee (DEMO-EMP-001) for Keycloak {@code user_id} / employee portal.
     */
    private UUID resolveDemoPortalIndividualId(UUID organizationId) {
        return dealsRepository
                .findFirstByEmployeeNumberAndOrganization_OrganizationIdOrderByCreatedAtAsc("DEMO-EMP-001", organizationId)
                .map(Deals::getIndividualId)
                .orElseThrow(() -> new RuntimeException(
                        "Demo seed employee DEMO-EMP-001 not found for organization. Re-provision with 'Create fresh org'."));
    }

    /**
     * @param portalEmployeeIndividualId Primary seed employee {@code cpc.customers.individual_id} (DEMO-EMP-001);
     *                                     stored in Keycloak as {@code user_id} for employee-portal APIs such as {@code GET /api/v1/employees/{id}}.
     */
    private String createDemoKeycloakUser(Organization organization, String email, LocalDateTime expiresAt, UUID portalEmployeeIndividualId) {
        String fullName = "Demo User";
        String orgGroupName = "ORG_" + sanitizeName(organization.getOrganizationName());
        Optional<AdminUser> existingUser = adminUserRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtAsc(email);
        if (existingUser.isPresent() && !Boolean.TRUE.equals(existingUser.get().getIsDemoUser())) {
            throw new RuntimeException("This email belongs to an existing non-demo user account. Use a different email.");
        }
        boolean keycloakEmailExists = keyCloakUtil.emailExistsInRealm(email);
        if (keycloakEmailExists && !keyCloakUtil.isDemoAccountUserByEmail(email)) {
            throw new RuntimeException("This email is already used by an existing non-demo Keycloak user. Use a different email.");
        }
        String tempPassword = "Demo@" + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMM"));
        try {
            String createdPassword = keyCloakUtil.createUser(
                    fullName,
                    email,
                    email,
                    "ROLE_HR_ADMIN",
                    List.of(orgGroupName),
                    true,
                    tempPassword,
                    portalEmployeeIndividualId != null ? portalEmployeeIndividualId.toString() : null);
            boolean hrRoleAssigned = keyCloakUtil.addRoleToExistingUserByEmail(email, "ROLE_HR_ADMIN", List.of(orgGroupName))
                    || keyCloakUtil.addRoleToExistingUserByEmail(email, "HR_ADMIN", List.of(orgGroupName));
            boolean employeeRoleAssigned = keyCloakUtil.addRoleToExistingUserByEmail(email, "ROLE_EMPLOYEE", List.of(orgGroupName))
                    || keyCloakUtil.addRoleToExistingUserByEmail(email, "EMPLOYEE", List.of(orgGroupName));
            if (!hrRoleAssigned || !employeeRoleAssigned) {
                throw new RuntimeException("Failed to assign required demo roles (ROLE_HR_ADMIN and ROLE_EMPLOYEE) in Keycloak.");
            }
            keyCloakUtil.setUserAttribute(email, "demo_expires_at", expiresAt.toString());
            keyCloakUtil.setUserAttribute(email, "account_type", DEMO_ACCOUNT_TYPE);
            if (portalEmployeeIndividualId != null) {
                keyCloakUtil.setUserAttribute(email, "user_id", portalEmployeeIndividualId.toString());
            }
            return createdPassword;
        } catch (Exception ex) {
            if (isLocalLikeProfile()) {
                logger.warn("Keycloak unavailable in local-like profile. Continuing demo provisioning without Keycloak sync: {}", ex.getMessage());
                return tempPassword;
            }
            throw new RuntimeException("Failed to create demo Keycloak user: " + ex.getMessage(), ex);
        }
    }

    private AdminUser upsertDemoAdminUser(Organization organization, String email, LocalDateTime expiresAt) {
        Optional<AdminUser> existingUser = adminUserRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtAsc(email);
        AdminUser user = existingUser.orElseGet(AdminUser::new);
        if (existingUser.isEmpty()) {
            user.setAgentId(idGenerator.generateVimaId());
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setUsername(email);
        user.setEmail(email);
        user.setFullName("Demo User");
        user.setRole("HR_ADMIN");
        user.setIsActive(true);
        user.setIsDemoUser(true);
        user.setDemoExpiresAt(expiresAt);
        user.setOrganization(organization);
        return adminUserRepository.save(user);
    }

    private DemoSetupResponseDto toResponseDto(Organization org) {
        LocalDateTime expiresAt = org.getDemoExpiresAt();
        String status;
        if (expiresAt == null) {
            status = "AVAILABLE";
        } else if (expiresAt.isAfter(LocalDateTime.now())) {
            status = "ACTIVE";
        } else {
            status = "EXPIRED";
        }
        return DemoSetupResponseDto.builder()
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .clientEmail(org.getDemoLastAssignedEmail())
                .status(status)
                .expiresAt(expiresAt)
                .createdAt(org.getCreatedAt())
                .reused(false)
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeName(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
    }

    private void seedBaselineEnrollmentWindow(Organization organization, AdminUser createdBy) {
        List<EnrollmentWindows> windows = enrollmentWindowsRepository
                .findAllByOrganization_OrganizationId(organization.getOrganizationId());
        if (!windows.isEmpty()) {
            return;
        }
        EnrollmentWindows window = new EnrollmentWindows();
        window.setOrganization(organization);
        window.setName("Demo Enrollment");
        window.setDescription("Seeded demo enrollment cycle");
        window.setStartDate(java.time.LocalDate.now().minusDays(14));
        window.setEndDate(java.time.LocalDate.now().minusDays(1));
        window.setStatus(EnrollementStatus.CLOSED);
        window.setClosedAt(LocalDateTime.now());
        window.setConfig("{}");
        window.setCreatedBy(createdBy);
        enrollmentWindowsRepository.save(window);
    }

    private void ensureSeedWindowClosed(UUID organizationId, LocalDateTime cutoff) {
        List<EnrollmentWindows> windows = enrollmentWindowsRepository.findAllByOrganization_OrganizationId(organizationId);
        Optional<EnrollmentWindows> seedWindow = windows.stream()
                .filter(w -> w.getCreatedAt() != null && !w.getCreatedAt().isAfter(cutoff))
                .min(Comparator.comparing(EnrollmentWindows::getCreatedAt));
        seedWindow.ifPresent(window -> {
            window.setStatus(EnrollementStatus.CLOSED);
            window.setClosedAt(LocalDateTime.now());
            enrollmentWindowsRepository.save(window);
        });
    }

    private void resetClientMutations(UUID organizationId, LocalDateTime cutoff) {
        entityManager.createNativeQuery("""
                DELETE FROM cpc.deal_endorsements de
                USING cpc.endorsements e
                WHERE de.endorsement_id = e.endorsement_id
                  AND e.organization_id = :organizationId
                  AND e.created_at > :cutoff
                """)
                .setParameter("organizationId", organizationId)
                .setParameter("cutoff", cutoff)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM cpc.endorsements
                WHERE organization_id = :organizationId
                  AND created_at > :cutoff
                """)
                .setParameter("organizationId", organizationId)
                .setParameter("cutoff", cutoff)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM cpc.enrollment_submissions
                WHERE employee_id IN (
                    SELECT individual_id FROM cpc.customers
                    WHERE organization_id = :organizationId
                )
                  AND created_at > :cutoff
                """)
                .setParameter("organizationId", organizationId)
                .setParameter("cutoff", cutoff)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM cpc.customers
                WHERE organization_id = :organizationId
                  AND created_at > :cutoff
                """)
                .setParameter("organizationId", organizationId)
                .setParameter("cutoff", cutoff)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM cpc.enrollment_windows
                WHERE organization_id = :organizationId
                  AND created_at > :cutoff
                """)
                .setParameter("organizationId", organizationId)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }

    private void seedDemoPoliciesAndRateCards(Organization organization) {
        UUID orgId = organization.getOrganizationId();
        if (!policyRepository.findByOrganizationId(orgId).isEmpty()) {
            return;
        }
        InsuranceProvider provider = insuranceProviderRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active insurance provider found for demo seeding"));

        Policy gmc = new Policy();
        gmc.setPolicyNumber("DEMO-GMC-" + System.currentTimeMillis());
        gmc.setOrganizationId(orgId);
        gmc.setInsuranceProviderId(provider.getProviderId());
        gmc.setPolicyCategory(ProductType.EMPLOYEE);
        gmc.setProductType(ProductType.GMC);
        gmc.setCoverageType(CoverageType.ESCP);
        gmc.setStatus(PolicyStatus.ACTIVE);
        gmc.setPremiumAmount(new BigDecimal("17700"));
        gmc.setNetAmount(new BigDecimal("15000"));
        gmc.setGst(new BigDecimal("2700"));
        gmc.setCdBalance(BigDecimal.ZERO);
        gmc.setSumInsured(new BigDecimal("1000000"));
        gmc.setStartDate(LocalDate.now().minusMonths(6));
        gmc.setEndDate(LocalDate.now().plusMonths(6));
        gmc.setRenewalDate(LocalDate.now().plusMonths(6));
        gmc.setIsDeleted(false);
        gmc.setAppliesToEmployees(true);
        gmc.setMaxChildrenAllowed(4);
        gmc.setInsurerName(provider.getProviderName());
        gmc.setTpaOrganizationName("Demo TPA");
        gmc.setTpaContactInfo("9000000001");
        gmc = policyRepository.save(gmc);

        Policy gpa = new Policy();
        gpa.setPolicyNumber("DEMO-GPA-" + (System.currentTimeMillis() + 1));
        gpa.setOrganizationId(orgId);
        gpa.setInsuranceProviderId(provider.getProviderId());
        gpa.setPolicyCategory(ProductType.EMPLOYEE);
        gpa.setProductType(ProductType.GPA);
        gpa.setCoverageType(CoverageType.E);
        gpa.setStatus(PolicyStatus.ACTIVE);
        gpa.setPremiumAmount(new BigDecimal("11800"));
        gpa.setNetAmount(new BigDecimal("10000"));
        gpa.setGst(new BigDecimal("1800"));
        gpa.setCdBalance(BigDecimal.ZERO);
        gpa.setSumInsured(new BigDecimal("500000"));
        gpa.setStartDate(LocalDate.now().minusMonths(6));
        gpa.setEndDate(LocalDate.now().plusMonths(6));
        gpa.setRenewalDate(LocalDate.now().plusMonths(6));
        gpa.setIsDeleted(false);
        gpa.setAppliesToEmployees(true);
        gpa.setSumInsuredMultiplier(4);
        gpa.setInsurerName(provider.getProviderName());
        gpa = policyRepository.save(gpa);

        premiumRateTableRepository.save(PremiumRateTable.builder()
                .organizationId(orgId)
                .policyId(gmc.getPolicyId())
                .productType(ProductType.GMC.getValue())
                .memberType("FAMILY")
                .rate(new BigDecimal("11500"))
                .effectiveFrom(LocalDate.now().minusMonths(6))
                .pricingModel(PricingModel.FLAT)
                .isDeleted(false)
                .gstInclusive(false)
                .build());

        premiumRateTableRepository.save(PremiumRateTable.builder()
                .organizationId(orgId)
                .policyId(gpa.getPolicyId())
                .productType(ProductType.GPA.getValue())
                .memberType("EMPLOYEE")
                .rate(new BigDecimal("850"))
                .effectiveFrom(LocalDate.now().minusMonths(6))
                .pricingModel(PricingModel.FLAT)
                .isDeleted(false)
                .gstInclusive(false)
                .build());
    }

    private void seedDemoEmployeesAndEnrollment(Organization organization, AdminUser createdBy, String clientEmail) {
        seedBaselineEnrollmentWindow(organization, createdBy);
        EnrollmentWindows window = enrollmentWindowsRepository
                .findAllByOrganization_OrganizationId(organization.getOrganizationId())
                .stream()
                .min(Comparator.comparing(EnrollmentWindows::getCreatedAt))
                .orElseThrow(() -> new RuntimeException("Failed to create demo enrollment window"));

        List<Deals> existingDeals = dealsRepository.findByOrganizationId(organization.getOrganizationId());
        if (existingDeals.size() >= 3) {
            return;
        }

        String orgSuffix = organization.getOrganizationId().toString().substring(0, 8);
        Deals d1 = createDemoEmployee(organization, "DEMO-EMP-001", "Demo HR User", clientEmail, "9000000101");
        Deals d2 = createDemoEmployee(organization, "DEMO-EMP-002", "Priya Sharma", "demo-emp-2+" + orgSuffix + "@vimainsurance.com", "9000000102");
        Deals d3 = createDemoEmployee(organization, "DEMO-EMP-003", "Raj Patel", "demo-emp-3+" + orgSuffix + "@vimainsurance.com", "9000000103");
        seedDemoDependents(organization, d1, d2, d3);

        d1.setEnrollmentWindow(window);
        d2.setEnrollmentWindow(window);
        d3.setEnrollmentWindow(window);
        d1.setEnrollmentStatus(EnrollementStatus.APPROVED);
        d2.setEnrollmentStatus(EnrollementStatus.APPROVED);
        d3.setEnrollmentStatus(EnrollementStatus.APPROVED);
        d1 = dealsRepository.save(d1);
        d2 = dealsRepository.save(d2);
        d3 = dealsRepository.save(d3);

        createApprovedSubmission(window, d1, createdBy, "DEMO-SUB-001");
        createApprovedSubmission(window, d2, createdBy, "DEMO-SUB-002");
        createApprovedSubmission(window, d3, createdBy, "DEMO-SUB-003");
        ensureEmployeePolicyMapping(organization.getOrganizationId());
    }

    private Deals createDemoEmployee(Organization organization, String employeeNumber, String fullName, String email, String phone) {
        Deals deal = new Deals();
        String finalEmail = resolveUniqueDealEmail(organization, email);
        deal.setOrganization(organization);
        deal.setEmployeeNumber(employeeNumber);
        deal.setFirstName(fullName.split(" ")[0]);
        deal.setLastName(fullName.contains(" ") ? fullName.substring(fullName.indexOf(' ') + 1) : "");
        deal.setFullName(fullName + " " + organization.getOrganizationId().toString().substring(0, 6));
        deal.setEmail(finalEmail);
        deal.setUsername(finalEmail);
        deal.setPhone(phone);
        deal.setDateOfBirth(LocalDate.now().minusYears(30));
        deal.setDateOfJoining(LocalDate.now().minusYears(2));
        deal.setGender("MALE");
        deal.setRelationship("EMPLOYEE");
        deal.setActualRelationship("SELF");
        deal.setIsPrimaryMember(true);
        deal.setStatus(AccountStatus.ACTIVE);
        deal.setAccountType(AccountType.CORPORATE_EMPLOYEE);
        deal.setEnrollmentStatus(EnrollementStatus.APPROVED);
        deal.setHealthId(demoHealthIdForPrimary(employeeNumber));
        return dealsRepository.save(deal);
    }

    private static String demoHealthIdForPrimary(String employeeNumber) {
        return "DEMO-HID-" + (employeeNumber != null ? employeeNumber : "EMP");
    }

    private static String demoHealthIdForDependent(String employeeNumber, String relationship) {
        String rel = relationship != null ? relationship.replaceAll("[^A-Za-z0-9]", "-") : "DEP";
        return "DEMO-HID-" + (employeeNumber != null ? employeeNumber : "EMP") + "-" + rel;
    }

    /**
     * Ensures stable demo Health IDs on seeded members (primary + dependents). Idempotent.
     */
    private void ensureDemoHealthIds(UUID organizationId) {
        List<Deals> members = dealsRepository.findByOrganizationId(organizationId);
        for (Deals d : members) {
            String en = d.getEmployeeNumber();
            if (en == null || !en.startsWith("DEMO-EMP-")) {
                continue;
            }
            if (d.getHealthId() != null && !d.getHealthId().isBlank()) {
                continue;
            }
            if (Boolean.TRUE.equals(d.getIsPrimaryMember())) {
                d.setHealthId(demoHealthIdForPrimary(en));
            } else {
                d.setHealthId(demoHealthIdForDependent(en, d.getRelationship()));
            }
            dealsRepository.save(d);
        }
    }

    /**
     * Two sample claims (cashless in review + settled reimbursement for a dependent) when the org has no claims yet.
     * Skipped if any claim already exists for the org (idempotent for reuse).
     */
    private void seedDemoClaimsIfAbsent(Organization organization, AdminUser demoAdminUser) {
        UUID orgId = organization.getOrganizationId();
        if (!claimRepository.findByOrganization_OrganizationId(orgId).isEmpty()) {
            return;
        }
        Optional<Policy> gmcOpt = policyRepository.findByOrganizationId(orgId).stream()
                .filter(p -> p.getProductType() == ProductType.GMC
                        && p.getStatus() == PolicyStatus.ACTIVE
                        && !Boolean.TRUE.equals(p.getIsDeleted()))
                .findFirst();
        if (gmcOpt.isEmpty()) {
            logger.warn("[correlationId:{}] Demo claims seed skipped: no active GMC policy for org {}",
                    MDC.get("correlationId"), orgId);
            return;
        }
        Optional<Deals> primaryOpt = dealsRepository.findFirstByEmployeeNumberAndOrganization_OrganizationIdOrderByCreatedAtAsc(
                "DEMO-EMP-001", orgId);
        if (primaryOpt.isEmpty()) {
            logger.warn("[correlationId:{}] Demo claims seed skipped: DEMO-EMP-001 not found for org {}",
                    MDC.get("correlationId"), orgId);
            return;
        }
        Deals primary = primaryOpt.get();
        Optional<Deals> spouseOpt = dealsRepository
                .findFirstByEmployeeNumberAndOrganization_OrganizationIdAndRelationshipOrderByCreatedAtAsc(
                        "DEMO-EMP-001", orgId, "SPOUSE");

        Policy gmc = gmcOpt.get();
        String slug = orgId.toString().replace("-", "");

        Claim cashless = buildDemoClaimShell(organization, gmc, primary, demoAdminUser,
                "DEMO-SEED-" + slug + "-01", ClaimType.CASHLESS, ClaimStatus.PENDING_REVIEW);
        cashless.setMemberType(MemberType.EMPLOYEE);
        cashless.setRelationship(Relationship.SELF);
        cashless.setMemberName(trimName(primary.getFullName()));
        cashless.setMemberDob(primary.getDateOfBirth());
        String hid = primary.getHealthId() != null ? primary.getHealthId() : demoHealthIdForPrimary("DEMO-EMP-001");
        cashless.setMemberId(hid);
        cashless.setMemberUhid(hid);
        cashless.setReasonForAdmission("Elective daycare procedure (demo)");
        cashless.setDiagnosis("Minor procedure — illustrative demo case");
        cashless.setClaimAmount(new BigDecimal("85000"));
        cashless.setHospitalName("Demo Network Hospital");
        cashless.setHospitalCity("Bengaluru");
        cashless.setHospitalState("Karnataka");
        cashless.setHospitalPincode("560001");
        cashless.setIsNetworkHospital(true);
        cashless.setDateOfAdmission(LocalDate.now().minusDays(20));
        cashless.setDateOfDischarge(LocalDate.now().minusDays(18));
        cashless.setDateOfSubmission(LocalDate.now().minusDays(16));
        claimRepository.save(cashless);

        Claim reimbursement = buildDemoClaimShell(organization, gmc, primary, demoAdminUser,
                "DEMO-SEED-" + slug + "-02", ClaimType.REIMBURSEMENT, ClaimStatus.SETTLED);
        Deals memberForSettled = spouseOpt.orElse(null);
        if (memberForSettled != null) {
            reimbursement.setMemberType(MemberType.DEPENDENT);
            reimbursement.setRelationship(Relationship.SPOUSE);
            String spouseHid = memberForSettled.getHealthId() != null ? memberForSettled.getHealthId()
                    : demoHealthIdForDependent("DEMO-EMP-001", "SPOUSE");
            reimbursement.setMemberName(trimName(memberForSettled.getFullName()));
            reimbursement.setMemberDob(memberForSettled.getDateOfBirth());
            reimbursement.setMemberId(spouseHid);
            reimbursement.setMemberUhid(spouseHid);
        } else {
            reimbursement.setMemberType(MemberType.EMPLOYEE);
            reimbursement.setRelationship(Relationship.SELF);
            String hid2 = primary.getHealthId() != null ? primary.getHealthId() : demoHealthIdForPrimary("DEMO-EMP-001");
            reimbursement.setMemberName(trimName(primary.getFullName()));
            reimbursement.setMemberDob(primary.getDateOfBirth());
            reimbursement.setMemberId(hid2);
            reimbursement.setMemberUhid(hid2);
        }
        reimbursement.setReasonForAdmission("Outpatient treatment (demo)");
        reimbursement.setDiagnosis("Seasonal illness — illustrative demo case");
        reimbursement.setClaimAmount(new BigDecimal("18500"));
        reimbursement.setHospitalName("City Clinic — Demo");
        reimbursement.setHospitalCity("Bengaluru");
        reimbursement.setHospitalState("Karnataka");
        reimbursement.setHospitalPincode("560076");
        reimbursement.setIsNetworkHospital(false);
        reimbursement.setDateOfAdmission(LocalDate.now().minusMonths(2));
        reimbursement.setDateOfDischarge(LocalDate.now().minusMonths(2));
        reimbursement.setDateOfSubmission(LocalDate.now().minusMonths(2).plusDays(3));
        reimbursement.setBankAccountNumber("XXXXXXXX1234");
        reimbursement.setAccountHolderName(trimName(memberForSettled != null ? memberForSettled.getFullName() : primary.getFullName()));
        reimbursement.setIfscCode("HDFC0001234");
        reimbursement.setReviewedBy(demoAdminUser);
        reimbursement.setReviewedAt(LocalDateTime.now().minusMonths(2).plusDays(5));
        reimbursement.setApprovedBy(demoAdminUser);
        reimbursement.setApprovedAt(LocalDateTime.now().minusMonths(2).plusDays(6));
        reimbursement = claimRepository.save(reimbursement);

        ClaimSettlement settlement = new ClaimSettlement();
        settlement.setClaim(reimbursement);
        settlement.setClaimedAmount(new BigDecimal("18500"));
        settlement.setGrossSanctionedAmount(new BigDecimal("17500"));
        settlement.setNetSanctionedAmount(new BigDecimal("17200"));
        settlement.setAmountPaid(new BigDecimal("17200"));
        settlement.setPaymentMode("NEFT");
        settlement.setPaymentDate(LocalDate.now().minusMonths(2).plusDays(10));
        settlement.setSettlementDate(LocalDate.now().minusMonths(2).plusDays(10));
        claimSettlementRepository.save(settlement);

        logger.info("[correlationId:{}] Seeded demo claims {} and {} for org {}",
                MDC.get("correlationId"), cashless.getClaimNumber(), reimbursement.getClaimNumber(), orgId);
    }

    private static String trimName(String fullName) {
        if (fullName == null) {
            return "Demo Member";
        }
        String t = fullName.trim();
        return t.length() > 200 ? t.substring(0, 200) : t;
    }

    private Claim buildDemoClaimShell(
            Organization organization,
            Policy gmc,
            Deals employeePrimary,
            AdminUser demoAdminUser,
            String claimNumber,
            ClaimType claimType,
            ClaimStatus status) {
        Claim c = new Claim();
        c.setClaimNumber(claimNumber);
        c.setOrganization(organization);
        c.setPolicyId(gmc.getPolicyId());
        c.setPolicyNumber(gmc.getPolicyNumber());
        c.setValidUntil(gmc.getEndDate());
        c.setInsuranceProviderLogo(null);
        c.setInsurerId(gmc.getInsuranceProviderId());
        c.setEmployee(employeePrimary);
        c.setClaimType(claimType);
        c.setClaimCategory(ClaimCategory.FRESH_CLAIM);
        c.setProductType(ProductType.GMC);
        c.setInternalStatus(status);
        c.setSubmissionSource(SubmissionSource.EMPLOYEE_PORTAL);
        c.setSubmittedBy(demoAdminUser);
        c.setIsDeleted(false);
        return c;
    }

    private void seedDemoDependents(Organization organization, Deals d1, Deals d2, Deals d3) {
        createDemoDependent(organization, d1, "DEMO-EMP-001", "SPOUSE", "SELF", "Anita User", "9000000201");
        createDemoDependent(organization, d1, "DEMO-EMP-001", "CHILD1", "Son", "Aarav User", "9000000202");
        createDemoDependent(organization, d2, "DEMO-EMP-002", "SPOUSE", "SELF", "Karan Sharma", "9000000203");
        createDemoDependent(organization, d2, "DEMO-EMP-002", "PARENT", "Father", "Mohan Sharma", "9000000204");
        createDemoDependent(organization, d3, "DEMO-EMP-003", "CHILD1", "Daughter", "Ira Patel", "9000000205");
    }

    private void createDemoDependent(
            Organization organization,
            Deals primaryEmployee,
            String employeeNumber,
            String relationship,
            String actualRelationship,
            String fullName,
            String phone) {
        Optional<Deals> existing = dealsRepository.findFirstByEmployeeNumberAndOrganization_OrganizationIdAndRelationshipOrderByCreatedAtAsc(
                employeeNumber,
                organization.getOrganizationId(),
                relationship);
        if (existing.isPresent()) {
            return;
        }

        Deals dependent = new Deals();
        dependent.setOrganization(organization);
        dependent.setPrimaryIndividual(primaryEmployee);
        dependent.setEmployeeNumber(employeeNumber);
        dependent.setFirstName(fullName.split(" ")[0]);
        dependent.setLastName(fullName.contains(" ") ? fullName.substring(fullName.indexOf(' ') + 1) : "");
        dependent.setFullName(fullName + " " + organization.getOrganizationId().toString().substring(0, 6));
        dependent.setPhone(phone);
        dependent.setDateOfBirth(LocalDate.now().minusYears(relationship.startsWith("CHILD") ? 10 : 32));
        dependent.setGender("FEMALE");
        dependent.setRelationship(relationship);
        dependent.setActualRelationship(actualRelationship);
        dependent.setIsPrimaryMember(false);
        dependent.setStatus(AccountStatus.ACTIVE);
        dependent.setAccountType(AccountType.CORPORATE_EMPLOYEE);
        dependent.setEnrollmentStatus(EnrollementStatus.APPROVED);
        dependent.setDateOfJoining(primaryEmployee.getDateOfJoining());
        dependent.setEnrollmentWindow(primaryEmployee.getEnrollmentWindow());
        dependent.setHealthId(demoHealthIdForDependent(employeeNumber, relationship));
        dealsRepository.save(dependent);
    }

    private void createApprovedSubmission(EnrollmentWindows window, Deals employee, AdminUser reviewedBy, String ref) {
        if (enrollmentSubmissionRepository
                .findByEmployee_IndividualIdAndEnrollmentWindow_Id(employee.getIndividualId(), window.getId())
                .isPresent()) {
            return;
        }
        EnrollmentSubmission submission = new EnrollmentSubmission();
        submission.setEmployee(employee);
        submission.setEnrollmentWindow(window);
        submission.setReferenceNumber(ref + "-" + System.currentTimeMillis());
        submission.setStatus(EnrollementStatus.APPROVED);
        submission.setStage("REVIEW");
        submission.setPlanSelections("[]");
        submission.setNomineeData("{}");
        submission.setDependents("{}");
        submission.setPersonalDetails("{}");
        submission.setPremiumBreakdown("{}");
        submission.setSubmittedAt(LocalDateTime.now().minusDays(2));
        submission.setReviewedAt(LocalDateTime.now().minusDays(1));
        submission.setReviewedBy(reviewedBy);
        submission.setDeclarationAccepted(true);
        submission.setDeclarationTimestamp(LocalDateTime.now().minusDays(2));
        submission.setIdempotencyKey(UUID.randomUUID().toString().replace("-", ""));
        enrollmentSubmissionRepository.save(submission);
    }

    private void updatePrimarySeedEmployeeEmail(UUID organizationId, String newEmail) {
        dealsRepository
                .findFirstByEmployeeNumberAndOrganization_OrganizationIdOrderByCreatedAtAsc("DEMO-EMP-001", organizationId)
                .ifPresent(employee -> {
            employee.setEmail(resolveUniqueDealEmail(employee.getOrganization(), newEmail));
            employee.setUsername(employee.getEmail());
            dealsRepository.save(employee);
        });
    }

    private String resolveUniqueDealEmail(Organization organization, String preferredEmail) {
        if (preferredEmail == null || preferredEmail.isBlank()) {
            return preferredEmail;
        }
        String normalized = preferredEmail.trim().toLowerCase(Locale.ROOT);
        List<Deals> matches = dealsRepository.findAllByEmailIgnoreCaseOrderByCreatedAtAsc(normalized);
        boolean usedOutsideThisOrg = matches.stream().anyMatch(d ->
                d.getOrganization() == null
                        || organization == null
                        || !organization.getOrganizationId().equals(d.getOrganization().getOrganizationId()));
        if (!usedOutsideThisOrg) {
            return normalized;
        }
        String[] parts = normalized.split("@");
        String local = parts.length > 0 ? parts[0] : "demo";
        String domain = parts.length > 1 ? parts[1] : "vimainsurance.com";
        return local + "+" + organization.getOrganizationId().toString().substring(0, 8) + "@" + domain;
    }

    private boolean isLocalLikeProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles == null || profiles.length == 0) {
            return false;
        }
        for (String profile : profiles) {
            String p = profile == null ? "" : profile.toLowerCase(Locale.ROOT);
            if (p.contains("local") || p.contains("dev") || p.contains("test")) {
                return true;
            }
        }
        return false;
    }

    private void seedDefaultCostSharingRules(UUID organizationId) {
        LocalDate effectiveFrom = LocalDate.now();
        List<CostSharingRule> defaults = List.of(
                defaultRule(organizationId, "GMC", CoverageCategory.SELF, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "GMC", CoverageCategory.SPOUSE, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "GMC", CoverageCategory.CHILD, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "GMC", CoverageCategory.PARENT, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "GPA", CoverageCategory.ALL_DEPENDENTS, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "GTL", CoverageCategory.ALL_DEPENDENTS, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "TOP_UP", CoverageCategory.SELF, BigDecimal.ZERO, effectiveFrom),
                defaultRule(organizationId, "SUPER_TOP_UP", CoverageCategory.SELF, BigDecimal.ZERO, effectiveFrom));
        for (CostSharingRule rule : defaults) {
            boolean exists = costSharingRuleRepository.existsByCompanyIdAndPlanTypeAndCoverageCategoryAndEffectiveFrom(
                    rule.getCompanyId(), rule.getPlanType(), rule.getCoverageCategory(), rule.getEffectiveFrom());
            if (exists) {
                continue;
            }
            try {
                costSharingRuleRepository.save(rule);
            } catch (DataIntegrityViolationException ignored) {
                // idempotent by design
            }
        }
    }

    private CostSharingRule defaultRule(UUID organizationId, String planType, CoverageCategory category, BigDecimal employerShare, LocalDate effectiveFrom) {
        return CostSharingRule.builder()
                .companyId(organizationId)
                .planType(planType)
                .coverageCategory(category)
                .employerShareType(EmployerShareType.PERCENTAGE)
                .employerShareValue(employerShare)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .isDeleted(false)
                .build();
    }

    private void ensureEmployeePolicyMapping(UUID organizationId) {
        List<Deals> allMembers = dealsRepository.findByOrganizationId(organizationId);
        List<Policy> policies = policyRepository.findByOrganizationId(organizationId).stream()
                .filter(p -> p.getStatus() == PolicyStatus.ACTIVE)
                .collect(Collectors.toList());

        for (Deals member : allMembers) {
            String relationship = member.getRelationship() != null ? member.getRelationship() : "SELF";
            UUID primaryEmployeeId = Boolean.TRUE.equals(member.getIsPrimaryMember())
                    ? null
                    : (member.getPrimaryIndividual() != null ? member.getPrimaryIndividual().getIndividualId() : null);
            for (Policy policy : policies) {
                if (!appliesPolicyToRelationship(policy, relationship)) {
                    continue;
                }
                if (employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(member.getIndividualId(), policy.getPolicyId(), "ACTIVE")) {
                    continue;
                }
                EmployeePolicyMap mapping = EmployeePolicyMap.builder()
                        .individualId(member.getIndividualId())
                        .primaryEmployeeId(primaryEmployeeId)
                        .relationship(relationship)
                        .policyId(policy.getPolicyId())
                        .organizationId(organizationId)
                        .sumInsured(policy.getSumInsured())
                        .coverageTier(policy.getCoverageType() != null ? policy.getCoverageType().getValue() : "E")
                        .isVoluntary(false)
                        .status("ACTIVE")
                        .effectiveFrom(policy.getStartDate() != null ? policy.getStartDate() : LocalDate.now())
                        .source("DEMO_SEED")
                        .enrollmentWindowId(member.getEnrollmentWindow() != null ? member.getEnrollmentWindow().getId() : null)
                        .build();
                employeePolicyMapRepository.save(mapping);
            }
        }
    }

    private boolean appliesPolicyToRelationship(Policy policy, String relationship) {
        if (policy == null || policy.getProductType() == null) {
            return true;
        }
        String rel = relationship != null ? relationship.trim() : "";
        boolean isSelf = "SELF".equalsIgnoreCase(rel) || "EMPLOYEE".equalsIgnoreCase(rel);
        ProductType pt = policy.getProductType();
        if (pt == ProductType.GPA || pt == ProductType.GTL || pt == ProductType.TOP_UP || pt == ProductType.SUPER_TOP_UP) {
            return isSelf;
        }
        if (pt == ProductType.PARENT_GMC) {
            return "PARENT".equalsIgnoreCase(rel) || "PARENT_IN_LAW".equalsIgnoreCase(rel);
        }
        return true;
    }
}
