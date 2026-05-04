package com.vimainsurance.vimaadmin.notification;

import java.time.LocalDateTime;
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
    private final NotificationsFeatureGate notificationsFeatureGate;

    @Transactional(readOnly = true)
    public AdminNotificationPageResponseDto list(boolean unreadOnly, NotificationCategory category, UUID companyIdFilter,
            int page, int size) {
        if (!notificationsFeatureGate.isNotificationsEnabled()) {
            return AdminNotificationPageResponseDto.builder()
                    .content(java.util.List.of())
                    .totalElements(0)
                    .page(page)
                    .size(size)
                    .build();
        }
        AdminUser me = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow();
        if ("HR_ADMIN".equals(me.getRole()) && me.getOrganization() == null) {
            return AdminNotificationPageResponseDto.builder()
                    .content(java.util.List.of())
                    .totalElements(0)
                    .page(page)
                    .size(size)
                    .build();
        }
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
        if (!notificationsFeatureGate.isNotificationsEnabled()) {
            return 0L;
        }
        UUID recipientId = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow().getId();
        return notificationRepository.countByRecipient_IdAndReadAtIsNull(recipientId);
    }

    @Transactional
    public boolean markRead(UUID id) {
        if (!notificationsFeatureGate.isNotificationsEnabled()) {
            return false;
        }
        UUID recipientId = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow().getId();
        int updated = notificationRepository.markReadIfOwned(id, recipientId, LocalDateTime.now());
        return updated > 0;
    }

    @Transactional
    public int markAllRead(UUID companyIdFilter) {
        if (!notificationsFeatureGate.isNotificationsEnabled()) {
            return 0;
        }
        AdminUser me = jwtUserExtractor.resolveCurrentAdminUser().orElseThrow();
        if ("HR_ADMIN".equals(me.getRole()) && me.getOrganization() == null) {
            return 0;
        }
        UUID effectiveCompanyFilter = resolveCompanyFilter(me, companyIdFilter);
        return notificationRepository.markAllReadForRecipient(me.getId(), effectiveCompanyFilter, LocalDateTime.now());
    }

    private UUID resolveCompanyFilter(AdminUser me, UUID companyIdFilter) {
        if ("HR_ADMIN".equals(me.getRole())) {
            return me.getOrganization() != null ? me.getOrganization().getOrganizationId() : null;
        }
        return companyIdFilter;
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
                .build();
    }
}
