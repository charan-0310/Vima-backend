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
        AdminUser me = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow();
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
                .content(p.map(this::toDto).toList())
                .totalElements(p.getTotalElements())
                .page(p.getNumber())
                .size(p.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        AdminUser me = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow();
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
        return notificationRepository.countByRecipient_IdAndReadAtIsNull(me.getId());
    }

    @Transactional
    public boolean markRead(UUID id) {
        UUID recipientId = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow().getId();
        int updated = notificationRepository.markReadIfOwned(id, recipientId, LocalDateTime.now());
        return updated > 0;
    }

    @Transactional
    public int markAllRead(UUID companyIdFilter) {
        AdminUser me = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow();
        UUID effectiveCompanyFilter = resolveCompanyFilter(me, companyIdFilter);
        return notificationRepository.markAllReadForRecipient(me.getId(), effectiveCompanyFilter, LocalDateTime.now());
    }

    @Transactional
    public boolean markStarred(UUID id, boolean starred) {
        UUID recipientId = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow().getId();
        int updated = notificationRepository.markStarredIfOwned(id, recipientId, starred, LocalDateTime.now());
        return updated > 0;
    }

    private UUID resolveCompanyFilter(AdminUser me, UUID companyIdFilter) {
        if (isVimaAudienceRole(me.getRole())) {
            // Shared Vima audience inbox should not be narrowed by company selector.
            // Otherwise users can see partial notifications (e.g., only 2 instead of full shared pool).
            return null;
        }
        if (isHrAdminRole(me.getRole())) {
            // Do not filter HR inbox by company: rows are already scoped to recipient_admin_user_id.
            // Filtering by me.organization_id hid enrollment (and other) notifications when the HR user's
            // linked org in admin_users did not match notifications.company_id (common with legacy/null org linkage).
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

    private AdminNotificationResponseDto toDto(AdminNotification n) {
        return AdminNotificationResponseDto.builder()
                .id(n.getId())
                .createdAt(n.getCreatedAt())
                .eventType(n.getEventType())
                .category(n.getCategory())
                .title(n.getTitle())
                .body(n.getBody())
                .deepLinkUrl(n.getDeepLinkUrl())
                .readAt(n.getReadAt())
                .companyId(n.getCompany() != null ? n.getCompany().getOrganizationId() : null)
                .starred(Boolean.TRUE.equals(n.getIsStarred()))
                .actorName(resolveActorName(n))
                .organizationName(resolveOrganizationName(n))
                .build();
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
