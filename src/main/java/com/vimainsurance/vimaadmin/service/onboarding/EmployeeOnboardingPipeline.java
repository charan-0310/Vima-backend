package com.vimainsurance.vimaadmin.service.onboarding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.util.KeyCloakUtil;
import com.vimainsurance.vimaadmin.util.PasswordGenerator;
import com.vimainsurance.vimaadmin.util.PrimaryEmployeeRelationshipUtil;

/**
 * Shared Keycloak employee onboarding used by endorsement flows and organization-level login creation.
 */
@Component
public class EmployeeOnboardingPipeline {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeOnboardingPipeline.class);

    @Autowired
    private KeyCloakUtil keycloakUtil;

    @Autowired
    private IEmailService emailService;

    /**
     * Creates Keycloak users for SELF deals or, if the email already exists (including HR admins and other roles),
     * adds {@code ROLE_EMPLOYEE} and the org group without creating a duplicate account.
     */
    public void processDealsForOnboarding(List<Deals> deals, String orgName,
            AtomicInteger successCount, AtomicInteger failedCount,
            List<String> successUsers, List<String> failedUsers,
            List<String> existingKeycloakSuccessUsers) {
        List<Deals> selfDeals = deals.stream()
                .filter(PrimaryEmployeeRelationshipUtil::isPrimarySelfEmployee)
                .toList();

        LinkedHashSet<String> uniqueEmails = new LinkedHashSet<>();
        for (Deals d : selfDeals) {
            if (d.getEmail() != null && !d.getEmail().isBlank()) {
                uniqueEmails.add(d.getEmail().toLowerCase().trim());
            }
        }

        Map<String, Boolean> existsMap = new HashMap<>();
        if (!uniqueEmails.isEmpty()) {
            try {
                existsMap = new HashMap<>(keycloakUtil.checkEmailsExistInRealmBatched(new ArrayList<>(uniqueEmails)));
            } catch (Exception e) {
                logger.warn("[correlationId:{}] Batch Keycloak email lookup failed; falling back per email: {}",
                        MDC.get("correlationId"), e.getMessage());
                for (String em : uniqueEmails) {
                    try {
                        existsMap.put(em, keycloakUtil.emailExistsInRealm(em));
                    } catch (Exception ex) {
                        logger.warn("[correlationId:{}] emailExistsInRealm failed for {}: {}", MDC.get("correlationId"), em, ex.getMessage());
                        existsMap.put(em, false);
                    }
                }
            }
        }

        try (Keycloak kc = keycloakUtil.getKeycloakClient()) {
            Map<String, String> groupNameToId = keycloakUtil.loadTopLevelGroupNameToIdMap(kc);
            RoleRepresentation employeeRealmRole = keycloakUtil.getRealmRoleOrNull(kc, "ROLE_EMPLOYEE");
            List<String> orgAsList = Arrays.asList(orgName);

            for (Deals deal : selfDeals) {
                try {
                    if (deal.getEmail() == null || deal.getEmail().isBlank()) {
                        failedCount.incrementAndGet();
                        failedUsers.add(deal.getFullName() != null ? deal.getFullName().trim() : "Unknown");
                        continue;
                    }
                    String emailLower = deal.getEmail().toLowerCase().trim();
                    boolean alreadyInRealm = Boolean.TRUE.equals(existsMap.get(emailLower));

                    if (alreadyInRealm) {
                        boolean ok = keycloakUtil.addRoleToExistingUserByEmail(kc, groupNameToId, employeeRealmRole,
                                emailLower, "ROLE_EMPLOYEE", orgAsList);
                        if (ok) {
                            successCount.incrementAndGet();
                            successUsers.add(emailLower);
                            existingKeycloakSuccessUsers.add(emailLower);
                            logger.info("[correlationId:{}] Employee onboarding: existing Keycloak user {} — ROLE_EMPLOYEE and org group applied (coexists with HR_ADMIN or other realm roles)",
                                    MDC.get("correlationId"), emailLower);
                        } else {
                            failedCount.incrementAndGet();
                            failedUsers.add(emailLower);
                            logger.error("[correlationId:{}] Existing Keycloak user {} but could not assign employee role/group",
                                    MDC.get("correlationId"), emailLower);
                        }
                    } else {
                        String password = PasswordGenerator.generateRandomPassword();
                        keycloakUtil.createUser(kc, groupNameToId, employeeRealmRole, deal.getFullName(), emailLower, emailLower,
                                "ROLE_EMPLOYEE", orgAsList, true, password, deal.getIndividualId().toString());
                        emailService.sendWelcomeEmail(emailLower, deal.getFullName(), emailLower, password);
                        successCount.incrementAndGet();
                        successUsers.add(emailLower);
                    }
                } catch (Exception e) {
                    String email = deal.getEmail() != null ? deal.getEmail().toLowerCase().trim() : null;
                    boolean recovered = keycloakUtil.addRoleToExistingUserByEmail(kc, groupNameToId, employeeRealmRole,
                            email, "ROLE_EMPLOYEE", orgAsList);
                    if (recovered) {
                        logger.info("[correlationId:{}] Employee onboarding recovered via existing-user path for {}", MDC.get("correlationId"), email);
                        successCount.incrementAndGet();
                        if (email != null) {
                            successUsers.add(email);
                            existingKeycloakSuccessUsers.add(email);
                        }
                        continue;
                    }
                    failedCount.incrementAndGet();
                    logger.error("[correlationId:{}] Error creating user for deal: {}", MDC.get("correlationId"), e.getMessage(), e);
                    failedUsers.add(deal.getEmail() != null ? deal.getEmail().toLowerCase().trim()
                            : deal.getFullName() != null ? deal.getFullName() : "Unknown");
                }
            }
        }
    }
}
