package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.dto.claim.HRClaimsSummaryResponse;
import com.vimainsurance.vimaadmin.dto.claim.MonthlyActivityItem;
import com.vimainsurance.vimaadmin.dto.claim.MonthlyActivityResponse;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.service.IHRClaimsService;
import com.vimainsurance.vimaadmin.service.IClaimsService;
import com.vimainsurance.vimaadmin.specification.ClaimSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HRClaimsServiceImpl implements IHRClaimsService {

    private static final int CSV_PAGE_SIZE = 500;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final IClaimsService claimsService;
    private final IClaimRepository claimRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<Page<ClaimDetailsResponse>>> listCompanyClaims(
            List<UUID> organizationIds, ClaimListFilters filters, Pageable pageable) {
        if (filters == null) {
            filters = new ClaimListFilters();
        }
        filters.setOrganizationIds(organizationIds);
        filters.setIsDeleted(false);
        Page<ClaimDetailsResponse> page = claimsService.listClaims(filters, pageable);
        return ResponseEntity.ok(new ResponseDto<>("Success", page, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> getClaimDetail(UUID claimId, List<UUID> organizationIds) {
        Claim claim = claimRepository.findById(claimId).orElse(null);
        if (claim == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseDto<>(404, "Claim not found"));
        }
        if (claim.getOrganization() == null || organizationIds == null || organizationIds.isEmpty()
                || !organizationIds.contains(claim.getOrganization().getOrganizationId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>("Access denied: claim does not belong to your organization", null));
        }
        ClaimDetailsResponse detail = claimsService.getClaimDetails(claimId);
        return ResponseEntity.ok(new ResponseDto<>("Success", detail));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<HRClaimsSummaryResponse>> getCompanySummary(List<UUID> organizationIds) {
        ClaimListFilters filters = new ClaimListFilters();
        filters.setOrganizationIds(organizationIds);
        filters.setIsDeleted(false);

        Specification<Claim> baseSpec = ClaimSpecification.withFiltersAndStatus(filters, null);
        long totalClaims = claimRepository.count(baseSpec);
        long pendingReview = claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.PENDING_REVIEW));
        long withInsurer = claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.SUBMITTED_TO_INSURER))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.IN_PROGRESS))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.QUERY_RAISED))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.QUERY_RESPONDED))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.APPROVED))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.PAYMENT_PENDING));
        long settled = claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.SETTLED));
        long rejected = claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.REJECTED))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.REJECTED_BY_ADMIN));

        BigDecimal totalClaimAmount = organizationIds != null && !organizationIds.isEmpty()
                ? claimRepository.sumClaimAmountByOrganizationIdIn(organizationIds)
                : BigDecimal.ZERO;
        BigDecimal totalSettledAmount = organizationIds != null && !organizationIds.isEmpty()
                ? claimRepository.sumSettledAmountByOrganizationIdIn(organizationIds)
                : BigDecimal.ZERO;

        List<Claim> claims = claimRepository.findAll(baseSpec, Pageable.unpaged()).getContent();
        int avgTatDays = 0;
        int countWithDates = 0;
        long sumDays = 0;
        for (Claim c : claims) {
            if (c.getDateOfSubmission() != null && c.getUpdatedAt() != null) {
                sumDays += java.time.temporal.ChronoUnit.DAYS.between(
                        c.getDateOfSubmission().atStartOfDay(), c.getUpdatedAt());
                countWithDates++;
            }
        }
        if (countWithDates > 0) {
            avgTatDays = (int) Math.round((double) sumDays / countWithDates);
        }

        HRClaimsSummaryResponse summary = HRClaimsSummaryResponse.builder()
                .totalClaims(totalClaims)
                .pendingReview(pendingReview)
                .withInsurer(withInsurer)
                .settled(settled)
                .rejected(rejected)
                .avgTatDays(avgTatDays)
                .totalClaimAmount(totalClaimAmount != null ? totalClaimAmount : BigDecimal.ZERO)
                .totalSettledAmount(totalSettledAmount != null ? totalSettledAmount : BigDecimal.ZERO)
                .build();
        return ResponseEntity.ok(new ResponseDto<>("Success", summary));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<MonthlyActivityResponse>> getMonthlyActivity(List<UUID> organizationIds, int months) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months).withDayOfMonth(1);
        if (organizationIds == null || organizationIds.isEmpty()) {
            return ResponseEntity.ok(new ResponseDto<>("Success", new MonthlyActivityResponse(List.of())));
        }
        ClaimListFilters filters = new ClaimListFilters();
        filters.setOrganizationIds(organizationIds);
        filters.setDateOfSubmissionFrom(startDate);
        filters.setDateOfSubmissionTo(endDate);
        filters.setIsDeleted(false);
        List<Claim> claims = claimRepository.findAll(ClaimSpecification.withFilters(filters), Pageable.unpaged()).getContent();

        Map<String, MonthlyActivityItem> byMonth = new java.util.LinkedHashMap<>();
        for (int i = 0; i < months; i++) {
            LocalDate monthStart = endDate.minusMonths(months - 1 - i).withDayOfMonth(1);
            String key = monthStart.getYear() + "-" + monthStart.getMonthValue();
            byMonth.put(key, MonthlyActivityItem.builder()
                    .month(monthStart.getMonthValue())
                    .year(monthStart.getYear())
                    .claimCount(0L)
                    .totalAmount(BigDecimal.ZERO)
                    .build());
        }
        for (Claim c : claims) {
            if (c.getDateOfSubmission() == null) continue;
            String key = c.getDateOfSubmission().getYear() + "-" + c.getDateOfSubmission().getMonthValue();
            MonthlyActivityItem item = byMonth.get(key);
            if (item != null) {
                byMonth.put(key, MonthlyActivityItem.builder()
                        .month(item.getMonth())
                        .year(item.getYear())
                        .claimCount(item.getClaimCount() + 1)
                        .totalAmount((item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO)
                                .add(c.getClaimAmount() != null ? c.getClaimAmount() : BigDecimal.ZERO))
                        .build());
            }
        }
        List<MonthlyActivityItem> activity = new ArrayList<>(byMonth.values());
        return ResponseEntity.ok(new ResponseDto<>("Success", new MonthlyActivityResponse(activity)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> exportClaimsCsv(List<UUID> organizationIds, ClaimListFilters filters, String filename) {
        if (filters == null) {
            filters = new ClaimListFilters();
        }
        filters.setOrganizationIds(organizationIds);
        filters.setIsDeleted(false);
        Specification<Claim> spec = ClaimSpecification.withFilters(filters);
        Sort sort = Sort.by(Sort.Direction.DESC, "dateOfSubmission");

        StreamingResponseBody body = out -> {
            try (OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                w.write("\uFEFF"); // BOM for Excel
                w.write("claimNumber,memberName,claimType,hospitalName,claimAmount,status,submittedAt,settledAmount,settledAt\n");
                int page = 0;
                Page<Claim> claimPage;
                do {
                    claimPage = claimRepository.findAll(spec, PageRequest.of(page, CSV_PAGE_SIZE, sort));
                    for (Claim c : claimPage.getContent()) {
                        String row = csvRow(c);
                        w.write(row);
                        w.write("\n");
                    }
                    w.flush();
                    page++;
                } while (claimPage.hasNext());
            } catch (IOException e) {
                log.warn("CSV export write error", e);
                throw new RuntimeException(e);
            }
        };
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    private static String csvRow(Claim c) {
        String claimNumber = escapeCsv(c.getClaimNumber());
        String memberName = escapeCsv(c.getMemberName());
        String claimType = c.getClaimType() != null ? escapeCsv(c.getClaimType().getValue()) : "";
        String hospitalName = escapeCsv(c.getHospitalName());
        String claimAmount = c.getClaimAmount() != null ? c.getClaimAmount().toPlainString() : "";
        String status = c.getInternalStatus() != null ? escapeCsv(c.getInternalStatus().getValue()) : "";
        String submittedAt = c.getDateOfSubmission() != null ? c.getDateOfSubmission().format(DATE_FORMAT) : "";
        String settledAmount = "";
        String settledAt = "";
        if (c.getSettlement() != null) {
            if (c.getSettlement().getAmountPaid() != null) {
                settledAmount = c.getSettlement().getAmountPaid().toPlainString();
            }
            if (c.getSettlement().getSettlementDate() != null) {
                settledAt = c.getSettlement().getSettlementDate().format(DATE_FORMAT);
            } else if (c.getUpdatedAt() != null) {
                settledAt = c.getUpdatedAt().format(DATE_TIME_FORMAT);
            }
        }
        return String.join(",", claimNumber, memberName, claimType, hospitalName, claimAmount, status, submittedAt, settledAmount, settledAt);
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
