package com.vimainsurance.vimaadmin.notification;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
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
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminNotificationInboxService {

    private final IAdminNotificationRepository notificationRepository;
    private final JwtUserExtractor jwtUserExtractor;

    /**
     * Inbox reads are not gated by {@code notifications.enabled} so recipients can still see persisted rows
     * when the flag is off or misconfigured; emit/dispatch remain gated in {@link NotificationServiceImpl}.
     */
    @Transactional(readOnly = true)
    public AdminNotificationPageResponseDto list(boolean unreadOnly, NotificationCategory category, UUID companyIdFilter,
            int page, int size) {
        AdminUser me = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow();
        UUID recipientId = me.getId();
        UUID effectiveCompanyFilter = resolveCompanyFilter(me, companyIdFilter);
        Page<AdminNotification> p = notificationRepository.findInbox(
                recipientId,
                unreadOnly,
                category,
                effectiveCompanyFilter,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return AdminNotificationPageResponseDto.builder()
                .content(p.map(this::toDto).toList())
                .totalElements(p.getTotalElements())
                .page(p.getNumber())
                .size(p.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        UUID recipientId = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow().getId();
        return notificationRepository.countByRecipient_IdAndReadAtIsNull(recipientId);
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
        if (isHrAdminRole(me.getRole())) {
            return me.getOrganization() != null ? me.getOrganization().getOrganizationId() : null;
        }
        return companyIdFilter;
    }

    private boolean isHrAdminRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return "HR_ADMIN".equals(normalized);
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
