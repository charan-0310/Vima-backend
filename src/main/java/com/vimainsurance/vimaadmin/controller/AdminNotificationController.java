package com.vimainsurance.vimaadmin.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.notification.AdminNotificationInboxService;
import com.vimainsurance.vimaadmin.notification.dto.AdminNotificationPageResponseDto;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping({"/api/v1/admin/notifications", "/api/notifications"})
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
public class AdminNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(AdminNotificationController.class);

    private final AdminNotificationInboxService inboxService;

    public AdminNotificationController(AdminNotificationInboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping
    public ResponseEntity<ResponseDto<AdminNotificationPageResponseDto>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("[correlationId:{}] GET /api/v1/admin/notifications", MDC.get("correlationId"));
        BaseResponse<AdminNotificationPageResponseDto> responseObj = new BaseResponse<>();
        NotificationCategory cat = null;
        if (category != null && !category.isBlank()) {
            try {
                cat = NotificationCategory.valueOf(category.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return responseObj.render(responseObj.formErrorResponse("Invalid category"));
            }
        }
        AdminNotificationPageResponseDto payload = inboxService.list(unreadOnly, cat, companyId, page, size);
        return responseObj.render(responseObj.formSuccessResponse("Notifications", payload, payload.getTotalElements()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ResponseDto<Long>> unreadCount() {
        logger.info("[correlationId:{}] GET /api/v1/admin/notifications/unread-count", MDC.get("correlationId"));
        BaseResponse<Long> responseObj = new BaseResponse<>();
        long count = inboxService.unreadCount();
        return responseObj.render(responseObj.formSuccessResponse("Unread count", count, count));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ResponseDto<String>> markRead(@PathVariable UUID id) {
        logger.info("[correlationId:{}] POST /api/v1/admin/notifications/{}/read", MDC.get("correlationId"), id);
        BaseResponse<String> responseObj = new BaseResponse<>();
        boolean ok = inboxService.markRead(id);
        if (!ok) {
            return responseObj.render(responseObj.formErrorResponse(404, "Notification not found or already read"));
        }
        return responseObj.render(responseObj.formSuccessResponse("OK", "marked"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ResponseDto<Integer>> markAllRead(@RequestParam(required = false) UUID companyId) {
        logger.info("[correlationId:{}] POST /api/v1/admin/notifications/read-all", MDC.get("correlationId"));
        BaseResponse<Integer> responseObj = new BaseResponse<>();
        int updated = inboxService.markAllRead(companyId);
        return responseObj.render(responseObj.formSuccessResponse("Updated", updated, updated));
    }

    @PostMapping("/{id}/star")
    public ResponseEntity<ResponseDto<String>> markStarred(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean starred) {
        logger.info("[correlationId:{}] POST /api/v1/admin/notifications/{}/star starred={}",
                MDC.get("correlationId"), id, starred);
        BaseResponse<String> responseObj = new BaseResponse<>();
        boolean ok = inboxService.markStarred(id, starred);
        if (!ok) {
            return responseObj.render(responseObj.formErrorResponse(404, "Notification not found"));
        }
        return responseObj.render(responseObj.formSuccessResponse("OK", starred ? "starred" : "unstarred"));
    }
}
