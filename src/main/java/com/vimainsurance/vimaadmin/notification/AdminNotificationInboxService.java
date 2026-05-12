package com.vimainsurance.vimaadmin.notification;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.notification.dto.AdminNotificationPageResponseDto;
import com.vimainsurance.vimaadmin.notification.dto.AdminNotificationResponseDto;
import com.vimainsurance.vimaadmin.notification.entity.AdminNotification;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.repository.IAdminNotificationRepository;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminNotificationInboxService {

    private static final List<String> VIMA_AUDIENCE_ROLES = List.of(
            "SUPER_ADMIN", "ADMIN", "VIMA_ADMIN", "SALES_ADMIN",
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_VIMA_ADMIN", "ROLE_SALES_ADMIN");
    private static final List<String> SHARED_VIMA_AND_HR_AUDIENCE_ROLES = List.of(
            "SUPER_ADMIN", "ADMIN", "VIMA_ADMIN", "SALES_ADMIN", "HR_ADMIN",
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_VIMA_ADMIN", "ROLE_SALES_ADMIN", "ROLE_HR_ADMIN");

    private final IAdminNotificationRepository notificationRepository;
    private final IAdminUserRepository adminUserRepository;
    private final JwtUserExtractor jwtUserExtractor;

    /**
     * Inbox reads are not gated by {@code notifications.enabled} so recipients can still see persisted rows
     * when the flag is off or misconfigured; emit/dispatch remain gated in {@link NotificationServiceImpl}.
     */
    @Transactional(readOnly = true)
    public AdminNotificationPageResponseDto list(boolean unreadOnly, NotificationCategory category, UUID companyIdFilter,
            int page, int size) {
        AdminUser me = currentAdminForNotificationInbox();
        UUID effectiveCompanyFilter = resolveCompanyFilter(me, companyIdFilter);
        Page<AdminNotification> p;
        if (isVimaAudienceRole(me.getRole())) {
            Set<UUID> sharedRecipientIds = resolveSharedAudienceRecipientIds();
            if (sharedRecipientIds.isEmpty()) {
                return AdminNotificationPageResponseDto.builder()
                        .content(List.of())
                        .totalElements(0)
                        .page(page)
                        .size(size)
                        .build();
            }
            Page<AdminNotification> raw = notificationRepository.findInboxByRecipientIds(
                    sharedRecipientIds,
                    unreadOnly,
                    category,
                    effectiveCompanyFilter,
                    PageRequest.of(0, Math.max(size * 20, 500), Sort.by(Sort.Direction.DESC, "createdAt")));
            p = dedupeSharedAudiencePage(raw, page, size);
        } else if (isHrAdminRole(me.getRole())) {
            UUID hrOrgId = resolveHrAdminOrganizationId(me);
            if (hrOrgId == null) {
                return AdminNotificationPageResponseDto.builder()
                        .content(List.of())
                        .totalElements(0)
                        .page(page)
                        .size(size)
                        .build();
            }
            p = notificationRepository.findInboxForHrAdminByOrganization(
                    me.getId(),
                    unreadOnly,
                    category,
                    hrOrgId,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            UUID recipientId = me.getId();
            p = notificationRepository.findInbox(
                    recipientId,
                    unreadOnly,
                    category,
                    effectiveCompanyFilter,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }
        return AdminNotificationPageResponseDto.builder()
                .content(p.map(n -> toDto(n, me)).toList())
                .totalElements(p.getTotalElements())
                .page(p.getNumber())
                .size(p.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        AdminUser me = currentAdminForNotificationInbox();
        if (isVimaAudienceRole(me.getRole())) {
            Set<UUID> sharedRecipientIds = resolveSharedAudienceRecipientIds();
            if (sharedRecipientIds.isEmpty()) {
                return 0L;
            }
            Page<AdminNotification> raw = notificationRepository.findInboxByRecipientIds(
                    sharedRecipientIds,
                    true,
                    null,
                    null,
                    PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "createdAt")));
            return raw.getContent().stream()
                    .map(this::logicalDedupKey)
                    .distinct()
                    .count();
        }
        if (isHrAdminRole(me.getRole())) {
            UUID hrOrgId = resolveHrAdminOrganizationId(me);
            if (hrOrgId == null) {
                return 0L;
            }
            return notificationRepository.countUnreadForHrAdminByOrganization(me.getId(), hrOrgId);
        }
        return notificationRepository.countByRecipient_IdAndReadAtIsNull(me.getId());
    }

    @Transactional
    public boolean markRead(UUID id) {
        AdminUser me = currentAdminForNotificationInbox();
        LocalDateTime now = LocalDateTime.now();
        if (isVimaAudienceRole(me.getRole())) {
            return markReadSharedLogicalGroup(id, me, now);
        }
        int updated = notificationRepository.markReadIfOwned(id, me.getId(), now);
        return updated > 0;
    }

    /**
     * Vima shared inbox dedupes by logical dedup key across many recipient rows. Marking read must clear
     * all matching fan-out rows so {@link #unreadCount()} (distinct logical keys) and the bell drop correctly.
     */
    private boolean markReadSharedLogicalGroup(UUID notificationId, AdminUser me, LocalDateTime readAt) {
        Set<UUID> sharedRecipientIds = resolveSharedAudienceRecipientIds();
        if (sharedRecipientIds.isEmpty()) {
            return false;
        }
        return notificationRepository.findById(notificationId).map((n) -> {
            if (n.getRecipient() == null || n.getRecipient().getId() == null) {
                return false;
            }
            if (!sharedRecipientIds.contains(n.getRecipient().getId())) {
                return false;
            }
            String dk = n.getDedupKey();
            if (dk == null || dk.isBlank()) {
                int owned = notificationRepository.markReadIfOwned(notificationId, me.getId(), readAt);
                return owned > 0;
            }
            String logical = logicalDedupKey(n);
            if (logical.isBlank()) {
                return false;
            }
            String dedupPrefix = logical + ":";
            int updated = notificationRepository.markReadLogicalGroupForRecipients(
                    sharedRecipientIds, logical, dedupPrefix, readAt);
            return updated > 0;
        }).orElse(false);
    }

    @Transactional
    public int markAllRead(UUID companyIdFilter) {
        AdminUser me = currentAdminForNotificationInbox();
        if (isHrAdminRole(me.getRole())) {
            UUID hrOrgId = resolveHrAdminOrganizationId(me);
            if (hrOrgId == null) {
                return 0;
            }
            return notificationRepository.markAllReadForHrAdminByOrganization(me.getId(), hrOrgId, LocalDateTime.now());
        }
        UUID effectiveCompanyFilter = resolveCompanyFilter(me, companyIdFilter);
        if (isVimaAudienceRole(me.getRole())) {
            Set<UUID> sharedRecipientIds = resolveSharedAudienceRecipientIds();
            if (sharedRecipientIds.isEmpty()) {
                return 0;
            }
            return notificationRepository.markAllReadForRecipientIds(
                    sharedRecipientIds, effectiveCompanyFilter, LocalDateTime.now());
        }
        return notificationRepository.markAllReadForRecipient(me.getId(), effectiveCompanyFilter, LocalDateTime.now());
    }

    @Transactional
    public boolean markStarred(UUID id, boolean starred) {
        UUID recipientId = currentAdminForNotificationInbox().getId();
        int updated = notificationRepository.markStarredIfOwned(id, recipientId, starred, LocalDateTime.now());
        return updated > 0;
    }

    /**
     * HR inbox needs {@code admin_users.organization_id}; re-load with {@code organization} graph when role is HR.
     */
    private AdminUser currentAdminForNotificationInbox() {
        AdminUser me = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow();
        if (isHrAdminRole(me.getRole()) && me.getId() != null) {
            return adminUserRepository.findWithOrganizationById(me.getId()).orElse(me);
        }
        return me;
    }

    private UUID resolveCompanyFilter(AdminUser me, UUID companyIdFilter) {
        if (isVimaAudienceRole(me.getRole())) {
            // Shared Vima audience inbox should not be narrowed by company selector.
            // Otherwise users can see partial notifications (e.g., only 2 instead of full shared pool).
            return null;
        }
        if (isHrAdminRole(me.getRole())) {
            // HR inbox list/mark-all-read use {@link #resolveHrAdminOrganizationId} and dedicated repository queries.
            return null;
        }
        return companyIdFilter;
    }

    private boolean isHrAdminRole(String role) {
        String normalized = normalizeRole(role);
        return "HR_ADMIN".equals(normalized);
    }

    private boolean isVimaAudienceRole(String role) {
        String normalized = normalizeRole(role);
        if (normalized == null) {
            return false;
        }
        return VIMA_AUDIENCE_ROLES.stream()
                .map(this::normalizeRole)
                .anyMatch(normalized::equals);
    }

    private Set<UUID> resolveSharedAudienceRecipientIds() {
        return adminUserRepository.findByIsActiveTrue().stream()
                .filter(u -> isSharedAudienceRole(u.getRole()))
                .map(AdminUser::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    private boolean isSharedAudienceRole(String role) {
        String normalized = normalizeRole(role);
        if (normalized == null) {
            return false;
        }
        return SHARED_VIMA_AND_HR_AUDIENCE_ROLES.stream()
                .map(this::normalizeRole)
                .anyMatch(normalized::equals);
    }

    @SuppressWarnings("null")
    private Page<AdminNotification> dedupeSharedAudiencePage(Page<AdminNotification> raw, int page, int size) {
        LinkedHashMap<String, AdminNotification> unique = new LinkedHashMap<>();
        for (AdminNotification n : raw.getContent()) {
            unique.putIfAbsent(logicalDedupKey(n), n);
        }
        List<AdminNotification> deduped = List.copyOf(unique.values());
        int from = Math.min(page * size, deduped.size());
        int to = Math.min(from + size, deduped.size());
        return new PageImpl<>(deduped.subList(from, to), PageRequest.of(page, size), deduped.size());
    }

    private String logicalDedupKey(AdminNotification n) {
        if (n == null || n.getDedupKey() == null) {
            return "";
        }
        return n.getDedupKey().replaceFirst(":[0-9a-fA-F\\-]{36}$", "");
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        normalized = normalized.replace('-', '_').replace(' ', '_');
        while (normalized.contains("__")) {
            normalized = normalized.replace("__", "_");
        }
        if (normalized.endsWith("_GROUP")) {
            normalized = normalized.substring(0, normalized.length() - "_GROUP".length());
        }
        return normalized;
    }

    /**
     * Organization used to scope HR notification inbox; must align with {@code notifications.company_id} when set.
     * When {@code admin_users.organization_id} is missing, uses the first valid UUID from the JWT {@code organization_ids}
     * claim (Keycloak) so HR users still see tenant-scoped rows after provisioning gaps.
     */
    private UUID resolveHrAdminOrganizationId(AdminUser me) {
        if (me != null && me.getOrganization() != null && me.getOrganization().getOrganizationId() != null) {
            return me.getOrganization().getOrganizationId();
        }
        for (String idStr : jwtUserExtractor.getCurrentOrganizations()) {
            if (idStr == null || idStr.isBlank()) {
                continue;
            }
            try {
                return UUID.fromString(idStr.trim());
            } catch (IllegalArgumentException ignored) {
                // skip malformed claim entries
            }
        }
        return null;
    }

    private AdminNotificationResponseDto toDto(AdminNotification n, AdminUser viewer) {
        return AdminNotificationResponseDto.builder()
                .id(n.getId())
                .createdAt(n.getCreatedAt())
                .eventType(n.getEventType())
                .category(n.getCategory())
                .title(n.getTitle())
                .body(n.getBody())
                .deepLinkUrl(resolveDeepLinkForViewer(n, viewer))
                .readAt(n.getReadAt())
                .companyId(n.getCompany() != null ? n.getCompany().getOrganizationId() : null)
                .starred(Boolean.TRUE.equals(n.getIsStarred()))
                .actorName(resolveActorName(n))
                .organizationName(resolveOrganizationName(n))
                .receiverEmail(n.getReceiverEmail())
                .receiverName(n.getReceiverName())
                .receiverRole(n.getReceiverRole())
                .creatorEmail(n.getCreatorEmail())
                .creatorName(n.getCreatorName())
                .creatorRole(n.getCreatorRole())
                .build();
    }

    private String resolveDeepLinkForViewer(AdminNotification n, AdminUser viewer) {
        String deepLink = n != null ? n.getDeepLinkUrl() : null;
        if (deepLink == null || deepLink.isBlank() || n == null || n.getEventType() == null) {
            return deepLink;
        }
        if (!isClaimEvent(n.getEventType())) {
            return deepLink;
        }
        String normalizedRole = normalizeRole(viewer != null ? viewer.getRole() : null);
        if ("VIMA_ADMIN".equals(normalizedRole)
                || "ADMIN".equals(normalizedRole)
                || "SUPER_ADMIN".equals(normalizedRole)) {
            return deepLink.replace("/hr/claims/", "/admin/claims/");
        }
        if ("HR_ADMIN".equals(normalizedRole)) {
            return deepLink.replace("/admin/claims/", "/hr/claims/");
        }
        return deepLink;
    }

    private boolean isClaimEvent(com.vimainsurance.vimaadmin.notification.enums.NotificationEventType eventType) {
        return switch (eventType) {
            case EMPLOYEE_CLAIM_SUBMITTED,
                    EMPLOYEE_CLAIM_QUERY_RAISED,
                    EMPLOYEE_CLAIM_QUERY_RESPONDED,
                    EMPLOYEE_CLAIM_QUERY_RESPONSE_SUBMITTED,
                    EMPLOYEE_CLAIM_APPROVED,
                    EMPLOYEE_CLAIM_REJECTED,
                    EMPLOYEE_CLAIM_SETTLED -> true;
            default -> false;
        };
    }

    private String resolveActorName(AdminNotification n) {
        Map<String, Object> vars = n.getEmailTemplateVars();
        if (vars == null) {
            return null;
        }
        Object actedBy = vars.get("actedByName");
        if (actedBy instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        Object uploadedBy = vars.get("uploadedByName");
        if (uploadedBy instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        return null;
    }

    private String resolveOrganizationName(AdminNotification n) {
        Map<String, Object> vars = n.getEmailTemplateVars();
        if (vars == null) {
            return null;
        }
        Object displayOrganizationName = vars.get("displayOrganizationName");
        if (displayOrganizationName instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        Object organizationName = vars.get("organizationName");
        if (organizationName instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        return null;
    }
}
