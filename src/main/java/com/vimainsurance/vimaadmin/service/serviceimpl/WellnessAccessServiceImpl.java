package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessAccessLogResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessDashboardDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.WellnessAccessLog;
import com.vimainsurance.vimaadmin.entity.WellnessPartner;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IWellnessAccessLogRepository;
import com.vimainsurance.vimaadmin.repository.IWellnessPartnerRepository;
import com.vimainsurance.vimaadmin.service.IWellnessAccessService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WellnessAccessServiceImpl implements IWellnessAccessService {

    private static final Logger logger = LoggerFactory.getLogger(WellnessAccessServiceImpl.class);

    private final IWellnessAccessLogRepository wellnessAccessLogRepository;
    private final IWellnessPartnerRepository wellnessPartnerRepository;
    private final IDealsRepository dealsRepository;

    @Override
    public void logAccess(UUID orgId, UUID partnerId, UUID employeeId, String userIdentifier, String accessType, String status,
            String errorMessage) {
        try {
            if (orgId == null) {
                logger.warn("[correlationId:{}] Skipping wellness access logging due to null organizationId", MDC.get("correlationId"));
                return;
            }

            WellnessAccessLog log = WellnessAccessLog.builder()
                    .organizationId(orgId)
                    .partnerId(partnerId)
                    .employeeId(employeeId)
                    .userIdentifier(userIdentifier)
                    .accessType(accessType == null ? "LOGIN" : accessType)
                    .status(status == null ? "FAILED" : status)
                    .errorMessage(errorMessage)
                    .accessedAt(LocalDateTime.now())
                    .build();
            wellnessAccessLogRepository.save(Objects.requireNonNull(log));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Failed to persist wellness access log: {}", MDC.get("correlationId"), e.getMessage(), e);
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Page<WellnessAccessLogResponseDto>>> getAccessLogs(UUID orgId, int page, int size) {
        BaseResponse<Page<WellnessAccessLogResponseDto>> responseObj = new BaseResponse<>();
        try {
            if (orgId == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "organizationId is required"));
            }

            int resolvedPage = Math.max(page, 0);
            int resolvedSize = size <= 0 ? 20 : Math.min(size, 100);
            Page<WellnessAccessLog> logsPage = wellnessAccessLogRepository.findByOrganizationIdOrderByAccessedAtDesc(
                    orgId,
                    PageRequest.of(resolvedPage, resolvedSize));

            Page<WellnessAccessLogResponseDto> payload = logsPage.map(this::toLogResponseDto);
            return responseObj.render(responseObj.formSuccessResponse(
                    "Wellness access logs fetched successfully",
                    payload,
                    logsPage.getTotalElements()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] getAccessLogs failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to fetch wellness access logs"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<WellnessDashboardDto>> getDashboardStats(UUID orgId) {
        BaseResponse<WellnessDashboardDto> responseObj = new BaseResponse<>();
        try {
            if (orgId == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "organizationId is required"));
            }

            Map<UUID, Long> totalByPartner = new HashMap<>();
            for (Object[] row : wellnessAccessLogRepository.getTotalAccessesByOrg(orgId)) {
                UUID partnerId = castToUuid(row[0]);
                Long total = castToLong(row[1]);
                if (partnerId != null) {
                    totalByPartner.put(partnerId, total);
                }
            }

            Map<UUID, Long> successByPartner = new HashMap<>();
            Map<UUID, Long> uniqueByPartner = new HashMap<>();
            for (Object[] row : wellnessAccessLogRepository.getAccessStatsByOrg(orgId)) {
                UUID partnerId = castToUuid(row[0]);
                if (partnerId == null) {
                    continue;
                }
                successByPartner.put(partnerId, castToLong(row[1]));
                uniqueByPartner.put(partnerId, castToLong(row[2]));
            }

            List<WellnessDashboardDto.PartnerAccessStat> partnerStats = totalByPartner.entrySet().stream()
                    .map(entry -> buildPartnerStat(entry.getKey(), entry.getValue(), successByPartner, uniqueByPartner))
                    .sorted((a, b) -> Long.compare(
                            b.getTotalAccesses() == null ? 0L : b.getTotalAccesses(),
                            a.getTotalAccesses() == null ? 0L : a.getTotalAccesses()))
                    .toList();

            WellnessDashboardDto payload = WellnessDashboardDto.builder()
                    .partners(partnerStats)
                    .build();
            return responseObj.render(responseObj.formSuccessResponse("Wellness dashboard stats fetched successfully", payload));
        } catch (Exception e) {
            logger.error("[correlationId:{}] getDashboardStats failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to fetch wellness dashboard stats"));
        }
    }

    private WellnessAccessLogResponseDto toLogResponseDto(WellnessAccessLog log) {
        WellnessPartner partner = null;
        Deals employee = null;
        UUID partnerId = log.getPartnerId();
        if (partnerId != null) {
            partner = wellnessPartnerRepository.findById(partnerId).orElse(null);
        }
        if (log.getEmployeeId() != null && log.getOrganizationId() != null) {
            employee = dealsRepository.findByIndividualIdAndOrganizationId(log.getEmployeeId(), log.getOrganizationId()).orElse(null);
        }
        return WellnessAccessLogResponseDto.builder()
                .id(log.getId())
                .partnerName(partner != null ? partner.getName() : null)
                .partnerSlug(partner != null ? partner.getSlug() : null)
                .employeeId(log.getEmployeeId())
                .employeeNumber(employee != null ? employee.getEmployeeNumber() : null)
                .userIdentifier(log.getUserIdentifier())
                .accessType(log.getAccessType())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .accessedAt(log.getAccessedAt())
                .build();
    }

    private WellnessDashboardDto.PartnerAccessStat buildPartnerStat(
            UUID partnerId,
            Long totalAccesses,
            Map<UUID, Long> successByPartner,
            Map<UUID, Long> uniqueByPartner) {
        UUID safePartnerId = Objects.requireNonNull(partnerId);
        Optional<WellnessPartner> partnerOpt = wellnessPartnerRepository.findById(safePartnerId);
        long total = totalAccesses == null ? 0L : totalAccesses;
        long successCount = successByPartner.getOrDefault(safePartnerId, 0L);
        double successRate = total == 0 ? 0.0 : (successCount * 100.0) / total;
        return WellnessDashboardDto.PartnerAccessStat.builder()
                .partnerId(safePartnerId)
                .partnerName(partnerOpt.map(WellnessPartner::getName).orElse(null))
                .partnerSlug(partnerOpt.map(WellnessPartner::getSlug).orElse(null))
                .totalAccesses(total)
                .uniqueEmployees(uniqueByPartner.getOrDefault(safePartnerId, 0L))
                .successRate(successRate)
                .build();
    }

    private UUID castToUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long castToLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }
}
