package com.vimainsurance.vimaadmin.notification.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminNotificationPageResponseDto {
    private List<AdminNotificationResponseDto> content;
    private long totalElements;
    private int page;
    private int size;
}
