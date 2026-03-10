package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.PayrollDeductionSchedule;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IPayrollDeductionScheduleRepository;
import com.vimainsurance.vimaadmin.service.IPayrollSchedulePopulationService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PayrollSchedulePopulationServiceImpl implements IPayrollSchedulePopulationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_PRODUCT_TYPE = "GMC";
    private static final String DEFAULT_DEDUCTION_FREQUENCY = "MONTHLY";

    @Autowired
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;
    @Autowired
    private IPayrollDeductionScheduleRepository payrollDeductionScheduleRepository;

    @Override
    @Transactional
    public void populateFromEnrollmentSubmission(UUID submissionId) {
        if (submissionId == null) {
            return;
        }
        Optional<EnrollmentSubmission> opt = enrollmentSubmissionRepository.findById(submissionId);
        if (opt.isEmpty()) {
            log.warn("populateFromEnrollmentSubmission: submission not found {}", submissionId);
            return;
        }
        EnrollmentSubmission sub = opt.get();
        if (sub.getEmployee() == null) {
            log.warn("populateFromEnrollmentSubmission: submission {} has no employee", submissionId);
            return;
        }
        UUID orgId = sub.getEmployee().getOrganization() != null
                ? sub.getEmployee().getOrganization().getOrganizationId()
                : null;
        if (orgId == null) {
            log.warn("populateFromEnrollmentSubmission: submission {} employee has no organization", submissionId);
            return;
        }
        UUID employeeId = sub.getEmployee().getIndividualId();
        LocalDate effectiveFrom = resolveEffectiveFrom(sub);

        List<PayrollDeductionSchedule> fromSnapshot = tryParseCostSharingSnapshot(
                sub.getCostSharingSnapshot(), orgId, employeeId, submissionId, effectiveFrom);
        if (!fromSnapshot.isEmpty()) {
            payrollDeductionScheduleRepository.saveAll(fromSnapshot);
            log.info("populateFromEnrollmentSubmission: created {} payroll rows from snapshot for submission {}", fromSnapshot.size(), submissionId);
            return;
        }

        // Fallback: one row from submission-level totals
        BigDecimal totalEmp = sub.getTotalEmployeeAnnualPremium() != null ? sub.getTotalEmployeeAnnualPremium() : BigDecimal.ZERO;
        BigDecimal totalEmpr = sub.getTotalEmployerAnnualPremium() != null ? sub.getTotalEmployerAnnualPremium() : BigDecimal.ZERO;
        BigDecimal totalPremium = totalEmp.add(totalEmpr);
        if (totalPremium.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("populateFromEnrollmentSubmission: submission {} has no premium, skipping", submissionId);
            return;
        }
        String frequency = sub.getDeductionFrequency() != null && !sub.getDeductionFrequency().isBlank()
                ? sub.getDeductionFrequency()
                : DEFAULT_DEDUCTION_FREQUENCY;
        BigDecimal deductionPerPeriod = sub.getDeductionAmountPerPeriod() != null
                ? sub.getDeductionAmountPerPeriod()
                : BigDecimal.ZERO;

        PayrollDeductionSchedule row = PayrollDeductionSchedule.builder()
                .organizationId(orgId)
                .employeeId(employeeId)
                .enrollmentSubmissionId(submissionId)
                .productType(DEFAULT_PRODUCT_TYPE)
                .planType(null)
                .coverageAmount(null)
                .totalPremiumAnnual(totalPremium)
                .employerShareAnnual(totalEmpr)
                .employeeShareAnnual(totalEmp)
                .deductionFrequency(frequency)
                .deductionAmountPerPeriod(deductionPerPeriod)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .build();
        payrollDeductionScheduleRepository.save(row);
        log.info("populateFromEnrollmentSubmission: created 1 payroll row (submission-level) for submission {}", submissionId);
    }

    private LocalDate resolveEffectiveFrom(EnrollmentSubmission sub) {
        if (sub.getEnrollmentWindow() != null && sub.getEnrollmentWindow().getStartDate() != null) {
            return sub.getEnrollmentWindow().getStartDate();
        }
        return LocalDate.now();
    }

    /**
     * Parse cost_sharing_snapshot JSON. Expected shape: array of { planType, productType, totalPremium, employerShare, employeeShare, deductionFrequency, deductionAmountPerPeriod, coverageAmount? }
     * or object with "deductions" array. Returns empty list if not parseable.
     */
    private List<PayrollDeductionSchedule> tryParseCostSharingSnapshot(
            String snapshotJson, UUID orgId, UUID employeeId, UUID submissionId, LocalDate effectiveFrom) {
        List<PayrollDeductionSchedule> rows = new ArrayList<>();
        if (snapshotJson == null || snapshotJson.isBlank() || "{}".equals(snapshotJson.trim())) {
            return rows;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(snapshotJson);
            JsonNode deductions = root.isArray() ? root : root.get("deductions");
            if (deductions == null || !deductions.isArray()) {
                return rows;
            }
            for (JsonNode node : deductions) {
                if (!node.isObject()) {
                    continue;
                }
                BigDecimal total = decimal(node.get("totalPremiumAnnual")).or(() -> decimal(node.get("totalPremium"))).orElse(BigDecimal.ZERO);
                BigDecimal empShare = decimal(node.get("employerShareAnnual")).or(() -> decimal(node.get("employerShare"))).orElse(BigDecimal.ZERO);
                BigDecimal eeShare = decimal(node.get("employeeShareAnnual")).or(() -> decimal(node.get("employeeShare"))).orElse(BigDecimal.ZERO);
                if (total.compareTo(BigDecimal.ZERO) <= 0 && empShare.add(eeShare).compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                if (total.compareTo(BigDecimal.ZERO) <= 0) {
                    total = empShare.add(eeShare);
                }
                String planType = text(node.get("planType"));
                String productType = text(node.get("productType"));
                if (productType == null || productType.isBlank()) {
                    productType = planType != null ? planType : DEFAULT_PRODUCT_TYPE;
                }
                String frequency = text(node.get("deductionFrequency"));
                if (frequency == null || frequency.isBlank()) {
                    frequency = DEFAULT_DEDUCTION_FREQUENCY;
                }
                BigDecimal deductionPerPeriod = decimal(node.get("deductionAmountPerPeriod")).or(() -> decimal(node.get("deductionAmount"))).orElse(BigDecimal.ZERO);
                BigDecimal coverage = decimal(node.get("coverageAmount")).orElse(null);

                rows.add(PayrollDeductionSchedule.builder()
                        .organizationId(orgId)
                        .employeeId(employeeId)
                        .enrollmentSubmissionId(submissionId)
                        .productType(productType)
                        .planType(planType)
                        .coverageAmount(coverage)
                        .totalPremiumAnnual(total)
                        .employerShareAnnual(empShare)
                        .employeeShareAnnual(eeShare)
                        .deductionFrequency(frequency)
                        .deductionAmountPerPeriod(deductionPerPeriod)
                        .effectiveFrom(effectiveFrom)
                        .effectiveTo(null)
                        .build());
            }
        } catch (Exception e) {
            log.debug("tryParseCostSharingSnapshot: could not parse snapshot for submission {}: {}", submissionId, e.getMessage());
        }
        return rows;
    }

    private static Optional<BigDecimal> decimal(JsonNode n) {
        if (n == null || n.isNull()) {
            return Optional.empty();
        }
        if (n.isNumber()) {
            return Optional.of(BigDecimal.valueOf(n.asDouble()));
        }
        String s = n.asText(null);
        if (s == null || s.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(s.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String text(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        String s = n.asText(null);
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }
}
