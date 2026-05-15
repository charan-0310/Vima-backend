package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.dto.manager.ManagerClaimsPipelineBucketDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerDashboardAlertsDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerDashboardPortfolioDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerDashboardPortfolioDto.ManagerActivePoliciesDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerDashboardPortfolioDto.ManagerCoveredLivesDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerDashboardSummaryDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerMonthlyEndorsementDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerPolicyExpirySegmentDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerTatSummaryDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerUpcomingRenewalDto;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IManagerDashboardService;
import com.vimainsurance.vimaadmin.specification.ClaimSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagerDashboardServiceImpl implements IManagerDashboardService {

    private static final ZoneId REPORT_TZ = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM ''yy", Locale.ENGLISH);

    private static final List<ClaimStatus> CLAIMS_NEEDING_ACTION = Arrays.asList(
            ClaimStatus.PENDING_REVIEW,
            ClaimStatus.DRAFT,
            ClaimStatus.INFO_REQUESTED,
            ClaimStatus.APPROVED_FOR_SUBMISSION,
            ClaimStatus.QUERY_RAISED,
            ClaimStatus.SUBMISSION_FAILED);

    private static final List<ClaimStatus> PIPELINE_PENDING_REVIEW = Arrays.asList(
            ClaimStatus.PENDING_REVIEW,
            ClaimStatus.DRAFT,
            ClaimStatus.INFO_REQUESTED,
            ClaimStatus.APPROVED_FOR_SUBMISSION,
            ClaimStatus.SUBMISSION_FAILED);

    private static final List<ClaimStatus> PIPELINE_WITH_INSURER = Arrays.asList(
            ClaimStatus.SUBMITTED_TO_INSURER,
            ClaimStatus.IN_PROGRESS,
            ClaimStatus.QUERY_RESPONDED,
            ClaimStatus.APPROVED,
            ClaimStatus.PAYMENT_PENDING);

    private static final List<ClaimStatus> PIPELINE_QUERY_RAISED = List.of(ClaimStatus.QUERY_RAISED);

    private static final List<ClaimStatus> PIPELINE_SETTLED = Arrays.asList(
            ClaimStatus.SETTLED,
            ClaimStatus.CLOSED);

    private static final List<ClaimStatus> PIPELINE_REJECTED = Arrays.asList(
            ClaimStatus.REJECTED,
            ClaimStatus.REJECTED_BY_ADMIN,
            ClaimStatus.INTIMATION_REJECTED);

    private final IOrganizationRepository organizationRepository;
    private final IDealsRepository dealsRepository;
    private final IPolicyRepository policyRepository;
    private final IEndorsementRepository endorsementRepository;
    private final IClaimRepository claimRepository;
    private final IEnrollmentWindowsRepository enrollmentWindowsRepository;

    private LocalDate today() {
        return LocalDate.now(REPORT_TZ);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(REPORT_TZ);
    }

    @Override
    @Transactional(readOnly = true)
    public ManagerDashboardSummaryDto getSummary() {
        LocalDate today = today();
        PolicyStatus active = PolicyStatus.ACTIVE;

        long activeOrgs = organizationRepository.countByStatusIgnoreCase("ACTIVE");
        long employees = dealsRepository.countActiveEmployeesInOrganizations(AccountStatus.ACTIVE);
        long dependents = dealsRepository.countActiveDependentsInOrganizations(AccountStatus.ACTIVE);
        long coveredTotal = employees + dependents;

        long activePolicies = policyRepository.countActivePoliciesForOrganizations(active);
        long exp30 = policyRepository.countActiveOrgPoliciesEndDateBetween(active, today, today.plusDays(30));
        long exp31to60 = policyRepository.countActiveOrgPoliciesEndDateBetween(active, today.plusDays(31), today.plusDays(60));

        BigDecimal premium = policyRepository.sumInceptionPremiumForActiveOrganizationPolicies(active);
        double premiumDouble = premium != null ? premium.doubleValue() : 0.0;

        long renewals30 = policyRepository.countActiveOrgPoliciesEndDateBetween(active, today, today.plusDays(30));
        long pendingEndorsements = Optional.ofNullable(endorsementRepository.getPendingCount()).orElse(0L);

        ClaimListFilters claimFilters = new ClaimListFilters();
        claimFilters.setIsDeleted(false);
        long claimsNeedingAction = claimRepository.count(
                ClaimSpecification.withFiltersAndStatusIn(claimFilters, CLAIMS_NEEDING_ACTION));

        long enrollSoon = enrollmentWindowsRepository.countEnrollmentWindowsExpiringSoon(today, today.plusDays(30));

        ManagerCoveredLivesDto covered = ManagerCoveredLivesDto.builder()
                .employees(employees)
                .dependents(dependents)
                .total(coveredTotal)
                .deltaFromLastMonth(0)
                .build();

        ManagerActivePoliciesDto ap = ManagerActivePoliciesDto.builder()
                .total(activePolicies)
                .expiringIn30Days(exp30)
                .expiringIn31To60Days(exp31to60)
                .build();

        ManagerDashboardPortfolioDto portfolio = ManagerDashboardPortfolioDto.builder()
                .activeClients((int) Math.min(activeOrgs, Integer.MAX_VALUE))
                .clientDeltaFromLastMonth(0)
                .coveredLives(covered)
                .activePolicies(ap)
                .totalAnnualPremium(premiumDouble)
                .premiumDeltaFromLastYear(0.0)
                .build();

        ManagerDashboardAlertsDto alerts = ManagerDashboardAlertsDto.builder()
                .renewalsIn30Days(renewals30)
                .pendingEndorsements(pendingEndorsements)
                .claimsNeedingAction(claimsNeedingAction)
                .enrollmentsExpiringSoon(enrollSoon)
                .build();

        return ManagerDashboardSummaryDto.builder()
                .portfolio(portfolio)
                .alerts(alerts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerPolicyExpirySegmentDto> getPolicyExpiryStatus() {
        LocalDate today = today();
        PolicyStatus active = PolicyStatus.ACTIVE;

        long critical = policyRepository.countActiveOrgPoliciesEndDateBetween(active, today, today.plusDays(30));
        long renewingSoon = policyRepository.countActiveOrgPoliciesEndDateBetween(active, today.plusDays(31), today.plusDays(60));
        long activeGt60 = policyRepository.countActiveOrgPoliciesWithEndAfter(active, today.plusDays(60));
        long lapsed = policyRepository.countLapsedOrExpiredOrganizationPolicies(today);

        return Arrays.asList(
                ManagerPolicyExpirySegmentDto.builder()
                        .segment("active")
                        .label("Active (> 60 days)")
                        .count(activeGt60)
                        .build(),
                ManagerPolicyExpirySegmentDto.builder()
                        .segment("renewing_soon")
                        .label("Renewing Soon (31–60d)")
                        .count(renewingSoon)
                        .build(),
                ManagerPolicyExpirySegmentDto.builder()
                        .segment("critical")
                        .label("Critical (≤ 30 days)")
                        .count(critical)
                        .build(),
                ManagerPolicyExpirySegmentDto.builder()
                        .segment("lapsed")
                        .label("Lapsed / Expired")
                        .count(lapsed)
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerClaimsPipelineBucketDto> getClaimsPipeline() {
        ClaimListFilters f = new ClaimListFilters();
        f.setIsDeleted(false);
        LocalDateTime agedBefore = now().minusDays(7);

        return Arrays.asList(
                bucket("Pending Review", PIPELINE_PENDING_REVIEW, f, agedBefore),
                bucket("With Insurer", PIPELINE_WITH_INSURER, f, agedBefore),
                bucket("Query Raised", PIPELINE_QUERY_RAISED, f, agedBefore),
                bucket("Settled", PIPELINE_SETTLED, f, agedBefore),
                bucket("Rejected", PIPELINE_REJECTED, f, agedBefore));
    }

    private ManagerClaimsPipelineBucketDto bucket(String label, List<ClaimStatus> statuses, ClaimListFilters f,
            LocalDateTime agedBefore) {
        long count = claimRepository.count(ClaimSpecification.withFiltersAndStatusIn(f, statuses));
        long aged = claimRepository.count(ClaimSpecification.withFiltersAndStatusInAndUpdatedAtBefore(f, statuses, agedBefore));
        return ManagerClaimsPipelineBucketDto.builder()
                .status(label)
                .count(count)
                .agedCount(aged)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerMonthlyEndorsementDto> getMonthlyEndorsements(int months) {
        int m = months <= 0 ? 12 : Math.min(months, 24);
        YearMonth endYm = YearMonth.from(today());
        YearMonth startYm = endYm.minusMonths(m - 1);

        LocalDateTime start = startYm.atDay(1).atStartOfDay(REPORT_TZ).toLocalDateTime();
        LocalDateTime endExclusive = endYm.plusMonths(1).atDay(1).atStartOfDay(REPORT_TZ).toLocalDateTime();

        List<Object[]> rows = endorsementRepository.aggregateMonthlyEndorsementActivityAllOrgs(start, endExclusive);
        Map<YearMonth, long[]> byMonth = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 4) {
                continue;
            }
            int y = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long additions = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long deletions = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            byMonth.put(YearMonth.of(y, month), new long[] { additions, deletions });
        }

        List<ManagerMonthlyEndorsementDto> out = new ArrayList<>();
        for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
            long[] pair = byMonth.getOrDefault(ym, new long[] { 0L, 0L });
            long add = pair[0];
            long del = pair[1];
            String label = ym.atDay(1).atStartOfDay(REPORT_TZ).format(MONTH_LABEL);
            out.add(ManagerMonthlyEndorsementDto.builder()
                    .month(label)
                    .additions(add)
                    .deletions(del)
                    .net(add - del)
                    .build());
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerUpcomingRenewalDto> getUpcomingRenewals(int days) {
        int d = days <= 0 ? 90 : Math.min(days, 365);
        LocalDate today = today();
        LocalDate until = today.plusDays(d);
        List<Object[]> rows = policyRepository.findUpcomingRenewalsNative(today, until);
        List<ManagerUpcomingRenewalDto> out = new ArrayList<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 8) {
                continue;
            }
            String policyIdStr = Objects.toString(row[0], null);
            String orgName = Objects.toString(row[1], "");
            String orgDisplay = row[2] != null ? Objects.toString(row[2], null) : null;
            String orgId = Objects.toString(row[3], "");
            String policyNumber = Objects.toString(row[4], "");
            String productRaw = Objects.toString(row[5], "GMC");
            BigDecimal sumInsuredBd = row[6] instanceof BigDecimal ? (BigDecimal) row[6]
                    : row[6] != null ? new BigDecimal(Objects.toString(row[6])) : BigDecimal.ZERO;
            LocalDate endDate = toLocalDate(row[7]);
            if (endDate == null) {
                continue;
            }
            int daysLeft = (int) ChronoUnit.DAYS.between(today, endDate);
            out.add(ManagerUpcomingRenewalDto.builder()
                    .id(policyIdStr)
                    .organizationName(orgName)
                    .organizationDisplayName(orgDisplay)
                    .organizationId(orgId)
                    .policyNumber(policyNumber)
                    .product(mapProductLabel(productRaw))
                    .sumInsured(sumInsuredBd.setScale(2, RoundingMode.HALF_UP).doubleValue())
                    .expiryDate(endDate.toString())
                    .daysLeft(daysLeft)
                    .build());
        }
        return out;
    }

    private static LocalDate toLocalDate(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDate ld) {
            return ld;
        }
        if (o instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (o instanceof java.time.Instant ins) {
            return ins.atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(o.toString());
    }

    private static String mapProductLabel(String productRaw) {
        if (productRaw == null || productRaw.isBlank()) {
            return "GMC";
        }
        try {
            ProductType pt = ProductType.fromValue(productRaw.trim());
            return switch (pt) {
                case GMC, PARENT_GMC, TOP_UP, SUPER_TOP_UP, HEALTH, GHI -> "GMC";
                case GPA, GTI -> "GPA";
                case GTL, TERM, LIFE -> "GTL";
                default -> "GMC";
            };
        } catch (Exception e) {
            return "GMC";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ManagerTatSummaryDto getTatSummary() {
        Double eAvg = endorsementRepository.avgEndorsementApprovalTurnaroundDays();
        Double cAvg = claimRepository.avgSettledClaimTurnaroundDays();
        return ManagerTatSummaryDto.builder()
                .endorsementAvgTatDays(round1(eAvg))
                .claimsAvgTatDays(round1(cAvg))
                .build();
    }

    private static double round1(Double v) {
        if (v == null || v.isNaN() || v.isInfinite()) {
            return 0.0;
        }
        return Math.round(v * 10.0) / 10.0;
    }
}
