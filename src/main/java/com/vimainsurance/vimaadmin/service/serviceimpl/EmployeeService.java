package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.dto.BulkEmployeeDeletionRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeeUploadDto;
import com.vimainsurance.vimaadmin.dto.EmployeeUploadResponse;
import com.vimainsurance.vimaadmin.dto.EndorsementSplitSummaryDto;
import com.vimainsurance.vimaadmin.dto.BulkEmployeePremiumPreviewRequestDto;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationResponseDto;
import com.vimainsurance.vimaadmin.dto.CostShareSplit;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.mapper.EmployeeToDeals;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.NomineeRelationship;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.entity.EmployeePolicyMap;

import org.javers.core.Javers;
import org.javers.core.diff.Diff;

import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.enums.ConfirmationMethod;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.EndorsementType;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IDealEndorsementRepository;

import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.entity.DealEndorsement;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IPremiumCalculationService;
import com.vimainsurance.vimaadmin.service.ICostSharingRuleService;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IEmployeePolicyMapRepository;
import com.vimainsurance.vimaadmin.exception.DocumentUploadException;
import com.vimainsurance.vimaadmin.util.SlackNotificationUtil;
import com.vimainsurance.vimaadmin.util.TopupPremiumOptionsUtil;

@Slf4j
@Service
public class EmployeeService {

    @Autowired
    private  Validator validator;

    @Autowired
    private IDealsRepository dealsRepository;



    @Autowired
    private EmployeeBatchService employeeBatchService;

    @Autowired
    private IEndorsementRepository endorsementRepository;

    @Autowired(required = false)
    private IEmployeePolicyMapService employeePolicyMapService;

    @Autowired
    private IDealEndorsementRepository dealEndorsementRepository;

    @Autowired
    private IDocumentRepository documentRepository;


    @Autowired
    private IDocumentService iDocumentService;

       
    @Autowired
    private Javers javers;

    @Autowired
    private SlackNotificationUtil slackNotificationUtil;

    @Autowired
    private IPolicyRepository policyRepository;

    @Autowired
    private IEmployeePolicyMapRepository employeePolicyMapRepository;

    @Autowired
    private IPremiumCalculationService premiumCalculationService;

    @Autowired
    private ICostSharingRuleService costSharingRuleService;

    private static java.math.BigDecimal toBigDecimalSafe(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        try {
            return new java.math.BigDecimal(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static java.time.LocalDate parseDateSafe(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        try {
            return java.time.LocalDate.parse(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<IPremiumCalculationService.MemberInfo> buildMembersForBulk(List<EmployeeUploadDto> group) {
        if (group == null) return List.of();
        LocalDate today = LocalDate.now();
        List<IPremiumCalculationService.MemberInfo> out = new ArrayList<>();
        for (EmployeeUploadDto dto : group) {
            if (dto == null) continue;
            String rel = dto.getRelationship() != null ? dto.getRelationship().trim() : "";
            LocalDate dob = parseDateSafe(dto.getDateOfBirth());
            int age = 0;
            if (dob != null) {
                age = java.time.Period.between(dob, today).getYears();
            }
            String memberType = "dependent";
            if ("Self".equalsIgnoreCase(rel)) {
                memberType = "EMPLOYEE";
            } else if ("Father".equalsIgnoreCase(rel) || "Mother".equalsIgnoreCase(rel)) {
                memberType = "parent";
            } else if (rel != null && (rel.toLowerCase().contains("in law") || rel.toLowerCase().contains("in-law"))) {
                memberType = "parent_in_law";
            }
            out.add(new IPremiumCalculationService.MemberInfo(memberType, age, dob));
        }
        return out;
    }

    /**
     * Admin endpoint used by Bulk Upload review UI to compute premium breakdown for a single employee group.
     * This uses the existing premium engine + cost-sharing rules. No premium input is accepted.
     */
    public ResponseEntity<ResponseDto<PremiumCalculationResponseDto>> previewBulkEmployeePremium(
            UUID companyId,
            BulkEmployeePremiumPreviewRequestDto request) {
        BaseResponse<PremiumCalculationResponseDto> responseObj = new BaseResponse<>();
        try {
            if (companyId == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "companyId is required"));
            }
            if (request == null || request.getPolicyIds() == null || request.getPolicyIds().isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(400, "policyIds is required"));
            }
            if (request.getEmployees() == null || request.getEmployees().isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(400, "employees is required"));
            }

            // Validate optional cover SI values the same way upload does.
            // (Reuses validateEmployee list-level checks by creating a fake Organization context would be heavy; do minimal here.)
            EmployeeUploadDto self = request.getEmployees().stream()
                    .filter(e -> e != null && e.getRelationship() != null && "Self".equalsIgnoreCase(e.getRelationship()))
                    .findFirst().orElse(null);
            if (self == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "Self row is required for premium preview"));
            }

            List<Policy> policies = policyRepository.findAllById(request.getPolicyIds());
            if (policies == null || policies.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(400, "No policies found for given policyIds"));
            }

            List<IPremiumCalculationService.MemberInfo> members = buildMembersForBulk(request.getEmployees());
            List<IPremiumCalculationService.MemberInfo> membersForBase = members; // includes employee + dependents
            IPremiumCalculationService.MemberInfo employeeOnly = members.stream()
                    .filter(m -> m.memberType() != null && ("EMPLOYEE".equalsIgnoreCase(m.memberType()) || "self".equalsIgnoreCase(m.memberType())))
                    .findFirst()
                    .orElse(members.isEmpty() ? null : members.get(0));
            List<IPremiumCalculationService.MemberInfo> membersEmployeeOnly =
                    employeeOnly != null ? List.of(employeeOnly) : membersForBase;

            List<PremiumCalculationResponseDto.PlanBreakdownItemDto> breakdowns = new ArrayList<>();
            java.math.BigDecimal totalAnnual = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalEmployer = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalEmployee = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalGst = java.math.BigDecimal.ZERO;

            for (Policy p : policies) {
                if (p == null || p.getProductType() == null) continue;
                String planType = p.getProductType().name();
                String upper = planType.toUpperCase();

                // Bulk flow: exclude parent coverage from preview.
                if ("PARENT_GMC".equals(upper)) continue;

                // Sum insured resolution:
                java.math.BigDecimal sumInsured = null;
                if ("GMC".equals(upper) || "GHI".equals(upper)) {
                    sumInsured = toBigDecimalSafe(self.getSumInsured());
                    if (sumInsured == null) sumInsured = p.getSumInsured();
                } else if ("TOP_UP".equals(upper)) {
                    sumInsured = toBigDecimalSafe(self.getTopupSumInsured());
                } else if ("SUPER_TOP_UP".equals(upper)) {
                    sumInsured = toBigDecimalSafe(self.getSuperTopupSumInsured());
                } else {
                    sumInsured = p.getSumInsured();
                }
                if (sumInsured == null) continue;

                // Skip optional covers if not selected in input.
                if ("TOP_UP".equals(upper) && (self.getTopupSumInsured() == null || self.getTopupSumInsured().trim().isEmpty())) continue;
                if ("SUPER_TOP_UP".equals(upper) && (self.getSuperTopupSumInsured() == null || self.getSuperTopupSumInsured().trim().isEmpty())) continue;

                String coverageTier = "INDIVIDUAL";
                List<IPremiumCalculationService.MemberInfo> coveredMembers =
                        ("TOP_UP".equals(upper) || "SUPER_TOP_UP".equals(upper))
                                ? membersEmployeeOnly
                                : membersForBase;

                // Compute plan premium
                IPremiumCalculationService.PlanPremiumBreakdown b = premiumCalculationService.calculatePlanPremium(
                        companyId, planType, coverageTier, sumInsured, coveredMembers);

                java.math.BigDecimal planPremium = b.premium() != null ? b.premium() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal gst = b.gstAmount() != null ? b.gstAmount() : java.math.BigDecimal.ZERO;

                // Apply cost sharing
                String coverageCategory = "FAMILY";
                CostShareSplit split = costSharingRuleService.applyCostSharing(companyId, planType, coverageCategory, planPremium);

                // Voluntary add-ons always 100% employee-paid
                if ("TOP_UP".equals(upper) || "SUPER_TOP_UP".equals(upper)) {
                    split = CostShareSplit.builder()
                            .employerShare(java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP))
                            .employeeShare(planPremium.setScale(2, java.math.RoundingMode.HALF_UP))
                            .shareType(split.getShareType())
                            .shareValue(split.getShareValue())
                            .ruleId(split.getRuleId())
                            .build();
                }

                breakdowns.add(PremiumCalculationResponseDto.PlanBreakdownItemDto.builder()
                        .planType(planType)
                        .premium(planPremium)
                        .employerShare(split.getEmployerShare())
                        .employeeShare(split.getEmployeeShare())
                        .gstAmount(gst)
                        .build());

                totalAnnual = totalAnnual.add(planPremium);
                totalEmployer = totalEmployer.add(split.getEmployerShare());
                totalEmployee = totalEmployee.add(split.getEmployeeShare());
                totalGst = totalGst.add(gst);
            }

            PremiumCalculationResponseDto out = PremiumCalculationResponseDto.builder()
                    .totalAnnualPremium(totalAnnual)
                    .totalEmployerShare(totalEmployer)
                    .totalEmployeeShare(totalEmployee)
                    .gstAmount(totalGst)
                    .perPlanBreakdown(breakdowns)
                    .deductionOptions(costSharingRuleService.calculateDeductions(totalEmployee))
                    .build();

            return responseObj.render(responseObj.formSuccessResponse("OK", out));
        } catch (Exception e) {
            log.error("previewBulkEmployeePremium error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to preview premium"));
        }
    }

    private static String formatLakhs(java.math.BigDecimal value) {
        if (value == null) return "";
        try {
            java.math.BigDecimal lakh = new java.math.BigDecimal("100000");
            if (value.compareTo(lakh) >= 0) {
                return value.divide(lakh, 0, java.math.RoundingMode.HALF_UP).toPlainString() + "L";
            }
        } catch (Exception ignored) {
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private List<java.math.BigDecimal> getTopupSumInsuredOptions(UUID organizationId, ProductType productType) {
        if (organizationId == null || productType == null) return List.of();
        List<com.vimainsurance.vimaadmin.entity.Policy> policies =
                policyRepository.findByOrganizationIdAndProductTypeAndStatus(organizationId, productType, PolicyStatus.ACTIVE);
        if (policies == null || policies.isEmpty()) return List.of();
        // If multiple active policies exist for same product type, treat as ambiguous for CSV/manual add selection.
        if (policies.size() > 1) {
            return List.of(new java.math.BigDecimal("-1")); // sentinel for ambiguity
        }
        String json = policies.get(0).getSumInsuredOptions();
        return TopupPremiumOptionsUtil.parseDecimalList(json);
    }


    public EmployeeUploadResponse validateEmployee(List<EmployeeUploadDto> employeeUploadDtoList, Organization organization) {
        List<String> errors = new ArrayList<>();
        Map<String, List<EmployeeUploadDto>> groupedEmployeeByEmployeeId = groupByEmployeeId(employeeUploadDtoList);
        for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedEmployeeByEmployeeId.entrySet()) {
            String employeeId = entry.getKey();
            List<EmployeeUploadDto> employeeUploadDtoListByEmployeeId = entry.getValue();
            

            //validate counts: 1 Self, 1 Spouse, 1 Father, 1 Mother, 1 Father in law, 1 Mother in law, max 4 Children
            long selfCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> "Self".equalsIgnoreCase(e.getRelationship()))
                .count();
            long spouseCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> "Spouse".equalsIgnoreCase(e.getRelationship()))
                .count();
            long fatherCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> "Father".equalsIgnoreCase(e.getRelationship()))
                .count();
            long motherCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> "Mother".equalsIgnoreCase(e.getRelationship()))
                .count();
            long fatherInLawCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> {
                    String rel = e.getRelationship();
                    return rel != null && isFatherInLawRelationship(rel);
                })
                .count();
            long motherInLawCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> {
                    String rel = e.getRelationship();
                    return rel != null && isMotherInLawRelationship(rel);
                })
                .count();
            // Count children (only Child1, Child2, Child3, Child4 with explicit indices)
            long childCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> {
                    String rel = e.getRelationship();
                    if (rel == null) return false;
                    int idx = extractChildIndexFromString(rel);
                    // Only count if it has a valid index (1-4)
                    return idx > 0 && idx <= 4;
                })
                .count();
            
            // Note: Sequential validation for children is done during upload
            // to check against existing children in database, not in the upload itself.
            // This allows updating individual children (e.g., Child3 alone) if they exist.
            
            if (selfCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Self is allowed");
            }
            if (spouseCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Spouse is allowed");
            }
            if (fatherCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Father is allowed");
            }
            if (motherCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Mother is allowed");
            }
            if (fatherInLawCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Father in law is allowed");
            }
            if (motherInLawCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Mother in law is allowed");
            }
            if (childCount > 4) {
                errors.add("employeeId: " + employeeId + " - Maximum 4 Children are allowed");
            }
            
            // Validate no duplicate child indices (e.g., multiple Child1 for same employee)
            Map<Integer, Long> childIndexCounts = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> {
                    String rel = e.getRelationship();
                    if (rel == null) return false;
                    int idx = extractChildIndexFromString(rel);
                    return idx > 0 && idx <= 4;
                })
                .collect(Collectors.groupingBy(
                    e -> extractChildIndexFromString(e.getRelationship()),
                    Collectors.counting()
                ));
            
            for (Map.Entry<Integer, Long> childIndexEntry : childIndexCounts.entrySet()) {
                if (childIndexEntry.getValue() > 1) {
                    errors.add("employeeId: " + employeeId + " - Duplicate Child" + childIndexEntry.getKey() + 
                        " found. Only one Child" + childIndexEntry.getKey() + " is allowed per employee");
                }
            }

            // Optional covers (Top-Up / Super Top-Up): validate SI on Self row only and against configured options
            EmployeeUploadDto selfDtoForCovers = employeeUploadDtoListByEmployeeId.stream()
                    .filter(e -> e != null && e.getRelationship() != null && "Self".equalsIgnoreCase(e.getRelationship()))
                    .findFirst()
                    .orElse(null);
            if (selfDtoForCovers != null && organization != null && organization.getOrganizationId() != null) {
                // Reject SI columns on non-self rows
                for (EmployeeUploadDto dto : employeeUploadDtoListByEmployeeId) {
                    if (dto == null) continue;
                    String rel = dto.getRelationship();
                    if (rel != null && !"Self".equalsIgnoreCase(rel)) {
                        boolean hasAny = (dto.getTopupSumInsured() != null && !dto.getTopupSumInsured().trim().isEmpty())
                                || (dto.getSuperTopupSumInsured() != null && !dto.getSuperTopupSumInsured().trim().isEmpty());
                        if (hasAny) {
                            errors.add("employeeId: " + employeeId + " - Top-Up/Super Top-Up selection is allowed only on the Self row");
                        }
                    }
                }

                String topupRaw = selfDtoForCovers.getTopupSumInsured() != null ? selfDtoForCovers.getTopupSumInsured().trim() : "";
                String superRaw = selfDtoForCovers.getSuperTopupSumInsured() != null ? selfDtoForCovers.getSuperTopupSumInsured().trim() : "";
                boolean hasTopup = !topupRaw.isEmpty();
                boolean hasSuper = !superRaw.isEmpty();
                if (hasTopup && hasSuper) {
                    errors.add("employeeId: " + employeeId + " - Select only one optional cover: topup_sum_insured or super_topup_sum_insured (not both)");
                }

                if (hasTopup) {
                    try {
                        java.math.BigDecimal selected = new java.math.BigDecimal(topupRaw);
                        List<java.math.BigDecimal> opts = getTopupSumInsuredOptions(organization.getOrganizationId(), ProductType.TOP_UP);
                        if (opts.size() == 1 && opts.get(0).compareTo(new java.math.BigDecimal("-1")) == 0) {
                            errors.add("employeeId: " + employeeId + " - Multiple active TOP_UP policies found. CSV selection is ambiguous; please keep only one active TOP_UP policy.");
                        } else if (!opts.isEmpty() && opts.stream().noneMatch(o -> o != null && o.compareTo(selected) == 0)) {
                            errors.add("employeeId: " + employeeId + " - Invalid top-up sum insured: " + formatLakhs(selected)
                                    + ". Available options: " + opts.stream().map(EmployeeService::formatLakhs).collect(Collectors.joining(", ")));
                        }
                    } catch (Exception e) {
                        errors.add("employeeId: " + employeeId + " - Invalid topup_sum_insured value. Must be numeric (e.g. 1000000 for 10L).");
                    }
                }

                if (hasSuper) {
                    try {
                        java.math.BigDecimal selected = new java.math.BigDecimal(superRaw);
                        List<java.math.BigDecimal> opts = getTopupSumInsuredOptions(organization.getOrganizationId(), ProductType.SUPER_TOP_UP);
                        if (opts.size() == 1 && opts.get(0).compareTo(new java.math.BigDecimal("-1")) == 0) {
                            errors.add("employeeId: " + employeeId + " - Multiple active SUPER_TOP_UP policies found. CSV selection is ambiguous; please keep only one active SUPER_TOP_UP policy.");
                        } else if (!opts.isEmpty() && opts.stream().noneMatch(o -> o != null && o.compareTo(selected) == 0)) {
                            errors.add("employeeId: " + employeeId + " - Invalid super top-up sum insured: " + formatLakhs(selected)
                                    + ". Available options: " + opts.stream().map(EmployeeService::formatLakhs).collect(Collectors.joining(", ")));
                        }
                    } catch (Exception e) {
                        errors.add("employeeId: " + employeeId + " - Invalid super_topup_sum_insured value. Must be numeric (e.g. 1000000 for 10L).");
                    }
                }
            }
            
            // Validate each employee in the group
            for (EmployeeUploadDto employeeUploadDto : employeeUploadDtoListByEmployeeId) {
                String relationship = employeeUploadDto.getRelationship();
                if(relationship == null || relationship.isBlank()) {
                    errors.add("employeeId: " + employeeId + " - Relationship is required");
                    continue;
                }
                
                // Validate that relationship is one of the allowed values
                if (!isValidRelationship(relationship)) {
                    errors.add("employeeId: " + employeeId + " - Invalid relationship: '" + relationship + 
                        "'. Allowed relationships: Self, Spouse, Father, Mother, Father in law, Mother in law, Child1, Child2, Child3, Child4");
                    continue;
                }
                
                // Validate Self relationship with bean validation
                if ("Self".equalsIgnoreCase(relationship)) {
                    Set<ConstraintViolation<EmployeeUploadDto>> violations = validator.validate(employeeUploadDto);
                    if (!violations.isEmpty()) {
                        errors.add("employeeId: " + employeeId + " - " + violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", ")));
                    }
                } 
                // Validate Child relationship - must have explicit index (Child1, Child2, Child3, or Child4)
                else if (relationship.toUpperCase().startsWith("CHILD")) {
                    // Extract child index
                    int childIndex = extractChildIndexFromString(relationship);
                    
                    // Validate that explicit index is provided (1-4)
                    if (childIndex == 0) {
                        errors.add("employeeId: " + employeeId + " - Child relationship must have explicit index. Use Child1, Child2, Child3, or Child4");
                    } else if (childIndex > 4) {
                        errors.add("employeeId: " + employeeId + " - Child index cannot be greater than 4. Maximum allowed: Child4");
                    } else {
                        // Validate age only if index is valid
                    try {
                        LocalDate dateOfBirth = LocalDate.parse(employeeUploadDto.getDateOfBirth());
                        int age = LocalDate.now().getYear() - dateOfBirth.getYear();
                        
                        // Adjust age if birthday hasn't occurred this year
                        LocalDate now = LocalDate.now();
                        if (dateOfBirth.plusYears(age).isAfter(now)) {
                            age--;
                        }
                        
                        // Child age should not be greater than or equal to 25 (i.e., must be < 25)
                        if (age >= 25) {
                            errors.add("employeeId: " + employeeId + " - Child age must be less than 25 years old");
                        }
                    } catch (Exception e) {
                        errors.add("employeeId: " + employeeId + " - Invalid date of birth format: " + employeeUploadDto.getDateOfBirth());
                    }
                }
                }
                else if ("Spouse".equalsIgnoreCase(relationship) || 
                        (relationship.toUpperCase().startsWith("SPOUSE"))) {
                            LocalDate dateOfBirth = LocalDate.parse(employeeUploadDto.getDateOfBirth());
                            int age = LocalDate.now().getYear() - dateOfBirth.getYear();
                            
                            // Adjust age if birthday hasn't occurred this year
                            LocalDate now = LocalDate.now();
                            if (dateOfBirth.plusYears(age).isAfter(now)) {
                                age--;
                            }
                            
                            // Spouse age should be greater than 18 (i.e., must be greater than 18)
                            if (age < 18) {
                                errors.add("employeeId: " + employeeId + " - Spouse age must be greater than 18 years old");
                            }
                }
                else if ("Father".equalsIgnoreCase(relationship) ||
                        relationship.toUpperCase().startsWith("FATHER") ||
                        isFatherInLawRelationship(relationship) ||
                        "Mother".equalsIgnoreCase(relationship) ||
                        relationship.toUpperCase().startsWith("MOTHER") ||
                        isMotherInLawRelationship(relationship)) {
                            LocalDate dateOfBirth = LocalDate.parse(employeeUploadDto.getDateOfBirth());
                            int age = LocalDate.now().getYear() - dateOfBirth.getYear();
                            
                            // Adjust age if birthday hasn't occurred this year
                            LocalDate now = LocalDate.now();
                            if (dateOfBirth.plusYears(age).isAfter(now)) {
                                age--;
                            }
                            
                            // Father/Mother/Father in law/Mother in law age should be less than 70 (i.e., must be less than 70)
                            if (age > 100) {
                                errors.add("employeeId: " + employeeId + " - Father/Mother/Father in law/Mother in law age must be less than 100 years old");
                        }
                }
            }
        }
        // List<Deals> existingDeals = dealsRepository.findByEmployeeNumberInAndOrganizationId(getEmployeeIds(employeeUploadDtoList), organization.getOrganizationId());
        //     Set<String> existingEmployeeIds = existingDeals.stream()
        //     .map(Deals::getEmployeeNumber)
        //     .filter(Objects::nonNull)
        //     .collect(Collectors.toSet());
        //     if(!existingDeals.isEmpty()) {
        //         errors.addAll(existingDeals.stream().distinct().map(d -> "employeeId: " + d.getEmployeeNumber() + " - Employee already exists").collect(Collectors.toList()));
        //     }
        //     if (existingEmployeeIds.size() < getEmployeeIds(employeeUploadDtoList).size()) {
                List<String> phonesToCheck = employeeUploadDtoList.stream()
                    .filter(e -> "Self".equalsIgnoreCase(e.getRelationship()))
                    .map(EmployeeUploadDto::getMobile)
                    .filter(Objects::nonNull)
                    .filter(phone -> !phone.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
                
                
                // if (!phonesToCheck.isEmpty() || !emailsToCheck.isEmpty()) {
                //     List<Deals> existingDealsByPhoneAndEmail = dealsRepository.findByEmployeePhoneAndEmployeeEmail(
                //         phonesToCheck, emailsToCheck, organization.getOrganizationId());
                    
                //     // Add errors for duplicates found by phone/email
                //     existingDealsByPhoneAndEmail.stream()
                //         .map(Deals::getEmployeeNumber)
                //         .filter(Objects::nonNull)
                //         .distinct()
                //         .forEach(empId -> 
                //             errors.add("employeeId: " + empId + " - Employee already exists by phone or email"));
                // }
            // }
        EmployeeUploadResponse response = new EmployeeUploadResponse();
        int totalEmployees = selfCount(employeeUploadDtoList).intValue();
        response.setTotalRows(employeeUploadDtoList.size());
        response.setTotalEmployees(totalEmployees);
        response.setTotalDependents(dependentCount(employeeUploadDtoList).intValue());
        response.setSuccessCount(totalEmployees - errors.size());
        response.setErrorCount(errors.size());
        response.setErrors(errors);
        response.setMessage(errors.isEmpty() ? "No Validation errors!" : "Validation errors");

        return response;
    }

    /**
     * Groups employee upload DTOs by employee ID
     * 
     * @param employeeUploadDtoList List of employee upload DTOs
     * @return Map where key is employeeId and value is list of DTOs with that employeeId
     */
    public Map<String, List<EmployeeUploadDto>> groupByEmployeeId(List<EmployeeUploadDto> employeeUploadDtoList) {
        if (employeeUploadDtoList == null || employeeUploadDtoList.isEmpty()) {
            return new LinkedHashMap<>();
        }
        
        return employeeUploadDtoList.stream()
            .collect(Collectors.groupingBy(
                dto -> dto.getEmployeeId() != null ? dto.getEmployeeId() : "",
                LinkedHashMap::new,  // Preserves insertion order
                Collectors.toList()
        ));
    }
    
    /**
     * Groups bulk employee deletion request DTOs by employee ID
     * 
     * @param bulkEmployeeDeletionRequestDtoList List of bulk employee deletion request DTOs
     * @return Map where key is employeeId and value is list of DTOs with that employeeId
     */
    public Map<String, List<BulkEmployeeDeletionRequestDto>> groupByEmployeeIdForBulkEmployeeDeletion(List<BulkEmployeeDeletionRequestDto> bulkEmployeeDeletionRequestDtoList) {
        if (bulkEmployeeDeletionRequestDtoList == null || bulkEmployeeDeletionRequestDtoList.isEmpty()) {
            return new LinkedHashMap<>();
        }
        
        return bulkEmployeeDeletionRequestDtoList.stream()
            .collect(Collectors.groupingBy(
                dto -> dto.getEmployeeId() != null ? dto.getEmployeeId() : "",
                LinkedHashMap::new,  // Preserves insertion order   
                Collectors.toList()
        ));
    }


    
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "EMPLOYEE_UPLOAD", action = "BULK_UPLOAD")
    @Transactional(rollbackFor = Exception.class)
    public EmployeeUploadResponse uploadEmployees(List<EmployeeUploadDto> employeeUploadDtoList, Organization organization, AdminUser adminUser, MultipartFile file, String uploadType, List<Long> policyIds) {
        try {
          Endorsement endorsement = new Endorsement();
          endorsement.setOrganization(organization);
          endorsement.setStatus(AccountStatus.PENDING_APPROVAL);
          endorsement.setEndorsementType(getEndorsementType(uploadType));
          endorsement.setConfirmationMethod(ConfirmationMethod.PORTAL);
          endorsement.setCreatedAt(LocalDateTime.now());
          endorsement.setUpdatedAt(LocalDateTime.now());
          endorsement.setUploadedBy(adminUser);
          EmployeeUploadResponse response = new EmployeeUploadResponse();
          EmployeeUploadResponse validateResponse = validateEmployee(employeeUploadDtoList, organization);
          if (validateResponse.getErrorCount() > 0)
            return validateResponse; 
          Map<String, List<EmployeeUploadDto>> groupedByEmployeeId = groupByEmployeeId(employeeUploadDtoList);
          List<Deals> dealsToSave = new ArrayList<>();
          int updatedCount = 0;
          int createdCount = 0;
          List<String> allEmployeeIds = new ArrayList<>(groupedByEmployeeId.keySet());
          List<Deals> existingPrimaries = this.dealsRepository.findByEmployeeNumberInAndOrganizationIdAndRelationship(allEmployeeIds, organization
              .getOrganizationId(), NomineeRelationship.SELF.getValue());
          Map<String, Deals> primaryEmployeeMap = (Map<String, Deals>)existingPrimaries.stream().collect(Collectors.toMap(Deals::getEmployeeNumber, d -> d, (d1, d2) -> d1));
          List<String> phonesToCheckBulk = new ArrayList<>();
          List<String> emailsToCheckBulk = new ArrayList<>();
        //   Map<String, EmployeeUploadDto> phoneToEmployeeMap = new HashMap<>();
        //   Map<String, EmployeeUploadDto> emailToEmployeeMap = new HashMap<>();
          for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedByEmployeeId.entrySet()) {
            String employeeId = entry.getKey();
            List<EmployeeUploadDto> employeeGroup = entry.getValue();
            EmployeeUploadDto selfDto = employeeGroup.stream().filter(e -> "Self".equalsIgnoreCase(e.getRelationship())).findFirst().orElse(null);
            if (selfDto != null && !primaryEmployeeMap.containsKey(employeeId)) {
              String phone = (selfDto.getMobile() != null && !selfDto.getMobile().trim().isEmpty()) ? selfDto.getMobile().trim() : null;
              String email = (selfDto.getEmail() != null && !selfDto.getEmail().trim().isEmpty()) ? selfDto.getEmail().trim() : null;
              if (phone != null) {
                phonesToCheckBulk.add(phone);
              } 
              if (email != null) {
                emailsToCheckBulk.add(email);
              } 
            } 
          } 
          Set<String> existingPhones = new HashSet<>();
          Set<String> existingEmails = new HashSet<>();
          if (!phonesToCheckBulk.isEmpty() || !emailsToCheckBulk.isEmpty()) {
            List<Deals> existingDuplicates = this.dealsRepository.findByEmployeePhoneAndEmployeeEmail(phonesToCheckBulk, emailsToCheckBulk, organization
                .getOrganizationId());
            existingPhones = (Set<String>)existingDuplicates.stream().map(Deals::getPhone).filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet());
            existingEmails = (Set<String>)existingDuplicates.stream().map(Deals::getEmail).filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet());
          } 
          List<Deals> primaryEmployeesForDependents = new ArrayList<>();
          for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedByEmployeeId.entrySet()) {
            String employeeId = entry.getKey();
            List<EmployeeUploadDto> employeeGroup = entry.getValue();
            Deals primaryEmployee = null;
            EmployeeUploadDto selfDto = employeeGroup.stream().filter(e -> "Self".equalsIgnoreCase(e.getRelationship())).findFirst().orElse(null);
            if (selfDto != null) {
              Deals existingPrimary = primaryEmployeeMap.get(employeeId);
              Deals existingPrimaryFromDb = null;
              Optional<Deals> existingPrimaryOptional = dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(employeeId, organization.getOrganizationId(), NomineeRelationship.SELF.getValue());
              if(existingPrimaryOptional.isPresent()) {
                existingPrimaryFromDb = existingPrimaryOptional.get();
                if(existingPrimaryFromDb.getStatus().equals(AccountStatus.PENDING_EXIT) || existingPrimaryFromDb.getStatus().equals(AccountStatus.LEAVING)) {
                    validateResponse.getErrors().add("employeeId: " + employeeId + " - Employee is currently in leaving or pending exit status.");
                    validateResponse.setMessage("Validation errors!!");
                    return validateResponse;
                }
              }
              if (existingPrimary != null || existingPrimaryFromDb != null) {
                primaryEmployee = new Deals();
                EmployeeToDeals.updateDealFromDto(primaryEmployee, selfDto, organization);
                primaryEmployee.setIndividualId(existingPrimaryFromDb.getIndividualId());
                primaryEmployee.setCreatedAt(existingPrimaryFromDb.getCreatedAt());
                primaryEmployee.setUpdatedAt(existingPrimaryFromDb.getUpdatedAt());
                primaryEmployee.setEndorsementId(existingPrimaryFromDb.getEndorsementId());
                primaryEmployee.setPrimaryIndividual(existingPrimaryFromDb.getPrimaryIndividual());
                primaryEmployee.setRelationship(existingPrimaryFromDb.getRelationship());
                primaryEmployee.setStatus(existingPrimaryFromDb.getStatus());
                primaryEmployee.setEndorsementId(existingPrimaryFromDb.getEndorsementId());
                Diff diff = javers.compare(existingPrimaryFromDb, primaryEmployee);
                if(diff.hasChanges()) {
                primaryEmployee.setRelationship(mapRelationshipToNomineeRelationship("Self", 0));
                primaryEmployee.setStatus(AccountStatus.PENDING_APPROVAL);
                updatedCount++;
                dealsToSave.add(primaryEmployee);
                log.debug("Updating existing primary employee: {}", employeeId);
                }
              } else {
                List<String> duplicateErrors = new ArrayList<>();
                String phone = (selfDto.getMobile() != null && !selfDto.getMobile().trim().isEmpty()) ? selfDto.getMobile().trim() : null;
                String email = (selfDto.getEmail() != null && !selfDto.getEmail().trim().isEmpty()) ? selfDto.getEmail().trim() : null;
                if (phone != null && existingPhones.contains(phone))
                  duplicateErrors.add("employeeId: " + employeeId + " - Phone number already exists: " + phone); 
                if (email != null && existingEmails.contains(email))
                  duplicateErrors.add("employeeId: " + employeeId + " - Email already exists: " + email); 
                if (!duplicateErrors.isEmpty()) {
                  validateResponse.getErrors().addAll(duplicateErrors);
                  validateResponse.setMessage("Validation errors!!");
                  return validateResponse;
                } 
                primaryEmployee = EmployeeToDeals.mapToDeals(selfDto, organization);
                primaryEmployee.setRelationship(mapRelationshipToNomineeRelationship("Self", 0));
                primaryEmployee.setIsPrimaryMember(true);
                primaryEmployee.setCreatedAt(LocalDateTime.now());
                primaryEmployee.setStatus(AccountStatus.PENDING_APPROVAL);
                createdCount++;
                dealsToSave.add(primaryEmployee);
                log.debug("Creating new primary employee: {}", employeeId);
              } 
              primaryEmployee.setUpdatedAt(LocalDateTime.now());
              primaryEmployeesForDependents.add(primaryEmployee);
              continue;
            } 
            Deals existingEmployee = primaryEmployeeMap.get(employeeId);
            if (existingEmployee != null) {
              if (existingEmployee.getIsPrimaryMember() != null && existingEmployee.getIsPrimaryMember()) {
                primaryEmployee = existingEmployee;
                log.debug("Found existing primary employee for dependents: {}", employeeId);
              } else if (existingEmployee.getPrimaryIndividual() != null) {
                primaryEmployee = existingEmployee.getPrimaryIndividual();
                log.debug("Found existing primary employee through dependent: {}", employeeId);
              } else {
                validateResponse.getErrors().add("employeeId: " + employeeId + " - Employee exists but is a dependent without primary individual");
                return validateResponse;
              } 
              primaryEmployeesForDependents.add(primaryEmployee);
              continue;
            } 
            validateResponse.getErrors().add("employeeId: " + employeeId + " - Employee not found. Cannot add dependents without primary employee (Self). Please include Self in the upload or ensure the employee exists in the database.");
            return validateResponse;
          } 
          Map<String, Deals> allPrimaryEmployeesMap = new HashMap<>(primaryEmployeeMap);
          for (Deals deal : dealsToSave) {
            if (deal.getRelationship() != null && NomineeRelationship.SELF.getValue().equals(deal.getRelationship()) && deal
              .getEmployeeNumber() != null)
              allPrimaryEmployeesMap.put(deal.getEmployeeNumber(), deal); 
          } 
          for (Deals primary : primaryEmployeesForDependents) {
            if (primary != null && primary.getEmployeeNumber() != null && 
              !allPrimaryEmployeesMap.containsKey(primary.getEmployeeNumber()))
              allPrimaryEmployeesMap.put(primary.getEmployeeNumber(), primary); 
          } 
          List<UUID> primaryIndividualIds = (List<UUID>)primaryEmployeesForDependents.stream().filter(p -> (p.getIndividualId() != null)).map(Deals::getIndividualId).collect(Collectors.toList());
          Map<UUID, List<Deals>> dependentsByPrimaryId = new HashMap<>();
          if (!primaryIndividualIds.isEmpty()) {
            List<Deals> allExistingDependents = this.dealsRepository.findByPrimaryIndividualIdIn(primaryIndividualIds);
            dependentsByPrimaryId = (Map<UUID, List<Deals>>)allExistingDependents.stream().filter(d -> (d.getPrimaryIndividual() != null && d.getPrimaryIndividual().getIndividualId() != null)).collect(Collectors.groupingBy(d -> d.getPrimaryIndividual().getIndividualId()));
          } 
          for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedByEmployeeId.entrySet()) {
            String employeeId = entry.getKey();
            List<EmployeeUploadDto> employeeGroup = entry.getValue();
            Deals primaryEmployee = allPrimaryEmployeesMap.get(employeeId);
            if (primaryEmployee != null) {
              List<Deals> existingDependents = new ArrayList<>();
              if (primaryEmployee.getIndividualId() != null)
                existingDependents = dependentsByPrimaryId.getOrDefault(primaryEmployee
                    .getIndividualId(), new ArrayList<>()); 
              Map<String, List<Deals>> existingDependentsByRelationship = (Map<String, List<Deals>>)existingDependents.stream().filter(d -> (d.getRelationship() != null)).collect(Collectors.groupingBy(d -> d.getRelationship().toUpperCase(), 
                    
                    Collectors.toList()));
              for (EmployeeUploadDto dependentDto : employeeGroup) {
                if (!"Self".equalsIgnoreCase(dependentDto.getRelationship())) {
                  String mappedRelationship, inputRelationship = dependentDto.getRelationship();
                  Deals existingDependent = null;
                  if (inputRelationship != null && inputRelationship.toUpperCase().startsWith("CHILD")) {
                    int childIndexFromJson = extractChildIndexFromString(inputRelationship);
                    if (childIndexFromJson == 0) {
                      validateResponse.getErrors().add("employeeId: " + employeeId + " - Child relationship must have explicit index. Use Child1, Child2, Child3, or Child4");
                      return validateResponse;
                    } 
                    if (childIndexFromJson > 4) {
                      validateResponse.getErrors().add("employeeId: " + employeeId + " - Child index cannot be greater than 4. Maximum allowed: Child4");
                      return validateResponse;
                    } 
                    mappedRelationship = mapRelationshipToNomineeRelationship(inputRelationship, childIndexFromJson);
                    List<Deals> existingWithSameRelationship = existingDependentsByRelationship.getOrDefault(mappedRelationship
                        .toUpperCase(), new ArrayList<>());
                    if (!existingWithSameRelationship.isEmpty()) {
                      existingDependent = existingWithSameRelationship.get(0);
                      log.debug("Found existing child with index {} for employee {}, will update", childIndexFromJson, employeeId);
                    } else {
                      for (int j = 1; j < childIndexFromJson; j++) {
                        String prevChildRel;
                        int prevChildIndex = j;
                        try {
                          prevChildRel = NomineeRelationship.valueOf("CHILD" + prevChildIndex).getValue();
                        } catch (IllegalArgumentException e) {
                          prevChildRel = "CHILD" + prevChildIndex;
                        } 
                        boolean existsInDatabase = existingDependentsByRelationship.containsKey(prevChildRel.toUpperCase());
                        boolean existsInUpload = employeeGroup.stream().anyMatch(dto -> {
                              if (dto.getRelationship() == null)
                                return false; 
                              int idx = extractChildIndexFromString(dto.getRelationship());
                              return (idx == prevChildIndex);
                            });
                        if (!existsInDatabase && !existsInUpload) {
                          validateResponse.getErrors().add("employeeId: " + employeeId + " - Cannot insert Child" + childIndexFromJson + " without Child" + prevChildIndex);
                          return validateResponse;
                        } 
                      } 
                      log.debug("Validated previous children exist (in database or upload), will create new Child{} for employee {}", childIndexFromJson, employeeId);
                    } 
                  } else {
                    mappedRelationship = mapRelationshipToNomineeRelationship(inputRelationship, 0);
                    List<Deals> existingWithSameRelationship = existingDependentsByRelationship.getOrDefault(mappedRelationship
                        .toUpperCase(), new ArrayList<>());
                    if (!existingWithSameRelationship.isEmpty())
                      existingDependent = existingWithSameRelationship.get(0); 
                  } 
                  if (existingDependent != null) {
                    Deals existingDependentToCompare = new Deals();
                    if(existingDependent.getStatus().equals(AccountStatus.PENDING_EXIT) || existingDependent.getStatus().equals(AccountStatus.LEAVING)) {
                        validateResponse.getErrors().add("employeeId: " + employeeId + " - Dependent is currently in leaving or pending exit status.");
                        validateResponse.setMessage("Validation errors!!");
                        return validateResponse;
                    }
                    EmployeeToDeals.updateDealFromDto(existingDependentToCompare, dependentDto, organization);
                    existingDependentToCompare.setIndividualId(existingDependent.getIndividualId());
                    existingDependentToCompare.setCreatedAt(existingDependent.getCreatedAt());
                    existingDependentToCompare.setPrimaryIndividual(existingDependent.getPrimaryIndividual());
                    existingDependentToCompare.setRelationship(existingDependent.getRelationship());
                    existingDependentToCompare.setStatus(existingDependent.getStatus());
                    existingDependentToCompare.setUpdatedAt(existingDependent.getUpdatedAt());
                    existingDependentToCompare.setEndorsementId(existingDependent.getEndorsementId());
                    existingDependentToCompare.setEmployeeNumber(existingDependent.getEmployeeNumber());
                    Diff diff = javers.compare(existingDependent,existingDependentToCompare);
                    if(diff.hasChanges()) {
                    log.info("Differences found in existing dependent: {}", diff.prettyPrint());
                    existingDependentToCompare.setRelationship(mappedRelationship);
                    existingDependentToCompare.setPrimaryIndividual(primaryEmployee);
                    existingDependentToCompare.setUpdatedAt(LocalDateTime.now());
                    existingDependentToCompare.setStatus(AccountStatus.PENDING_APPROVAL);
                    dealsToSave.add(existingDependentToCompare);
                    updatedCount++;
                    log.debug("Updating existing dependent: {} - {} (mapped to {})", new Object[] { employeeId, inputRelationship, mappedRelationship });
                    }
                  } else {
                  Deals newDependent = EmployeeToDeals.mapToDeals(dependentDto, organization);
                  newDependent.setEmployeeNumber(employeeId);
                  newDependent.setRelationship(mappedRelationship);
                  newDependent.setPrimaryIndividual(primaryEmployee);
                  newDependent.setCreatedAt(LocalDateTime.now());
                  newDependent.setUpdatedAt(LocalDateTime.now());
                  newDependent.setStatus(AccountStatus.PENDING_APPROVAL);
                  dealsToSave.add(newDependent);
                  createdCount++;
                  log.debug("Creating new dependent: {} - {} (mapped to {})", new Object[] { employeeId, inputRelationship, mappedRelationship });
                   } 
                  } 
              } 
            } 
          } 
          int batchSize = 1000;
          int totalSaved = 0;
          log.info("Saving {} deals ({} new, {} updated) in batches of {}", new Object[] { dealsToSave.size(), createdCount, updatedCount, batchSize });
          int i;
          Endorsement savedEndorsement = null;
          List<EndorsementSplitSummaryDto> splitSummaries = new ArrayList<>();
          if(updatedCount > 0 || createdCount > 0) {
          List<Deals> allSavedDeals = new ArrayList<>();
          // Save deals first
          for (i = 0; i < dealsToSave.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dealsToSave.size());
            List<Deals> batch = dealsToSave.subList(i, end);
            List<Deals> savedDeals = this.dealsRepository.saveAll(batch);
            totalSaved += savedDeals.size();
            allSavedDeals.addAll(savedDeals);
          }
          List<Policy> selectedPolicies = policyRepository.findAllById(policyIds);
          Map<Long, Policy> selectedPolicyMap = selectedPolicies.stream()
              .collect(Collectors.toMap(Policy::getPolicyId, p -> p, (a, b) -> a, LinkedHashMap::new));
          if (selectedPolicyMap.isEmpty()) {
              throw new RuntimeException("No active policies found for split endorsement creation");
          }
          UUID splitGroupId = UUID.randomUUID();
          Endorsement primaryEndorsement = null;
          Map<Long, List<Deals>> dealsByPolicy = mapDealsByPolicyForUpload(allSavedDeals, groupedByEmployeeId, selectedPolicyMap);
          for (Map.Entry<Long, List<Deals>> entry : dealsByPolicy.entrySet()) {
              if (entry.getValue().isEmpty()) {
                  continue;
              }
              Policy policy = selectedPolicyMap.get(entry.getKey());
              Endorsement splitEndorsement = new Endorsement();
              splitEndorsement.setOrganization(organization);
              splitEndorsement.setStatus(AccountStatus.PENDING_APPROVAL);
              splitEndorsement.setEndorsementType(getEndorsementType(uploadType));
              splitEndorsement.setConfirmationMethod(ConfirmationMethod.PORTAL);
              splitEndorsement.setCreatedAt(LocalDateTime.now());
              splitEndorsement.setUpdatedAt(LocalDateTime.now());
              splitEndorsement.setUploadedBy(adminUser);
              splitEndorsement.setPolicy(policy);
              splitEndorsement.setSplitGroupId(splitGroupId);
              splitEndorsement.setTotalEmployees((int) entry.getValue().stream().filter(d -> "SELF".equalsIgnoreCase(d.getRelationship())).count());
              splitEndorsement.setTotalDependents((int) entry.getValue().stream().filter(d -> !"SELF".equalsIgnoreCase(d.getRelationship())).count());
              if (primaryEndorsement != null) {
                  splitEndorsement.setParentEndorsement(primaryEndorsement);
              }
              splitEndorsement = endorsementRepository.save(splitEndorsement);
              if (primaryEndorsement == null) {
                  primaryEndorsement = splitEndorsement;
                  savedEndorsement = splitEndorsement;
              }
              for (Deals deal : entry.getValue()) {
                  if (deal.getEndorsementId() == null) {
                      deal.setEndorsementId(primaryEndorsement.getEndorsementId());
                  }
                  boolean exists = dealEndorsementRepository.existsByDeal_IndividualIdAndEndorsement_EndorsementId(
                      deal.getIndividualId(), splitEndorsement.getEndorsementId());
                  if (!exists) {
                      DealEndorsement de = new DealEndorsement();
                      de.setDeal(deal);
                      de.setEndorsement(splitEndorsement);
                      dealEndorsementRepository.save(de);
                  }
              }
              splitSummaries.add(new EndorsementSplitSummaryDto(
                  splitEndorsement.getEndorsementId(),
                  policy != null ? policy.getPolicyId() : null,
                  policy != null && policy.getProductType() != null ? policy.getProductType().getValue() : null,
                  splitEndorsement.getTotalEmployees(),
                  splitEndorsement.getTotalDependents(),
                  splitEndorsement.getSplitGroupId(),
                  splitEndorsement.getParentEndorsement() != null ? splitEndorsement.getParentEndorsement().getEndorsementId() : null
              ));
          }
          if (file != null && primaryEndorsement != null) {
              try {
                  Document document = uploadDocuments(file, organization, adminUser, primaryEndorsement);
                  primaryEndorsement.setDocument(document);
                  endorsementRepository.save(primaryEndorsement);
              } catch (DocumentUploadException e) {
                  log.error("Failed to upload document for endorsement {}: {}", primaryEndorsement.getEndorsementId(), e.getMessage(), e);
                  return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), "Failed to upload documents: " + e.getMessage(), 0, 0, new ArrayList<>());
              }
          }
          if (employeePolicyMapService != null) {
            List<UUID> primaryEmployeeIds = allSavedDeals.stream()
                .filter(deal -> deal.getRelationship() != null && "SELF".equalsIgnoreCase(deal.getRelationship()))
                .map(Deals::getIndividualId)
                .distinct()
                .toList();
            if (!primaryEmployeeIds.isEmpty() && policyIds != null && !policyIds.isEmpty()) {
              employeePolicyMapService.createMappingsFromBulkUpload(organization.getOrganizationId(), primaryEmployeeIds, "BULK_UPLOAD", policyIds);
            }
          }
        }
          log.info("Successfully saved {} deals ({} new, {} updated)", new Object[] { totalSaved, createdCount, updatedCount });
          response.setTotalRows(employeeUploadDtoList.size());
          response.setTotalEmployees(selfCount(employeeUploadDtoList).intValue());
          response.setTotalDependents(dependentCount(employeeUploadDtoList).intValue());
          response.setSuccessCount(totalSaved);
          response.setErrorCount(0);
          response.setErrors(new ArrayList());
          response.setEndorsements(splitSummaries);
          response.setMessage(String.format("Employees processed successfully: %d created, %d updated", new Object[] { createdCount, updatedCount }));
          if(createdCount == 0 && updatedCount == 0) {
            response.setMessage("No changes detected!");
          }
          if(savedEndorsement != null) {
            slackNotificationUtil.sendSlackMessage(slackNotificationUtil.buildEndorsementNotificationMessage(savedEndorsement), false);
          }
          return response;
        } catch (Exception e) {
          log.error("Error uploading employees: {}", e.getMessage(), e);
          throw new RuntimeException("Failed to save employees: " + e.getMessage(), e);
        } 
      }

    /**
     * Insert employees manually (no file). Uses same validation and insert logic as bulk upload.
     * No file parsing or document storage.
     */
    @Transactional(rollbackFor = Exception.class)
    public EmployeeUploadResponse manualAddEmployees(List<EmployeeUploadDto> employeeUploadDtoList, Organization organization, AdminUser adminUser, List<Long> policyIds) {
        return uploadEmployees(employeeUploadDtoList, organization, adminUser, null, "addition", policyIds);
    }

    
    public Long selfCount(List<EmployeeUploadDto> employeeUploadDtoList) {
        return employeeUploadDtoList.stream().filter(e ->  e.getRelationship().equalsIgnoreCase("Self")).count();
    }

    public Long dependentCount(List<EmployeeUploadDto> employeeUploadDtoList) {
        return employeeUploadDtoList.stream().filter(e -> !e.getRelationship().equalsIgnoreCase("Self")).count();
    }


    public List<String> getEmployeeIds(List<EmployeeUploadDto> employeeUploadDtoList) {
        return employeeUploadDtoList.stream().distinct().map(EmployeeUploadDto::getEmployeeId).collect(Collectors.toList());
    }

    public List<String> getEmployeePhones(List<EmployeeUploadDto> employeeUploadDtoList) {
        return employeeUploadDtoList.stream().distinct().map(EmployeeUploadDto::getMobile).collect(Collectors.toList());
    }

    public List<String> getEmployeeEmails(List<EmployeeUploadDto> employeeUploadDtoList) {
        return employeeUploadDtoList.stream().distinct().map(EmployeeUploadDto::getEmail).collect(Collectors.toList());
    }

   public EmployeeUploadResponse validateBulkEmployeeDeletion(List<BulkEmployeeDeletionRequestDto> bulkEmployeeDeletionRequestDtoList, Organization organization) {
    List<String> errors = new ArrayList<>();
    EmployeeUploadResponse response = new EmployeeUploadResponse();
    try{
        for (BulkEmployeeDeletionRequestDto bulkEmployeeDeletionRequestDto : bulkEmployeeDeletionRequestDtoList) {
            Set<ConstraintViolation<BulkEmployeeDeletionRequestDto>> violations = validator.validate(bulkEmployeeDeletionRequestDto);
            if (!violations.isEmpty()) {
                errors.addAll(violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList()));
            }
        }
        List<String> employeeIds = bulkEmployeeDeletionRequestDtoList.stream().map(BulkEmployeeDeletionRequestDto::getEmployeeId).collect(Collectors.toList());
        List<Deals> dealsList = dealsRepository.findByEmployeeNumberInAndOrganizationId(employeeIds, organization.getOrganizationId());
        if(dealsList.isEmpty()) {
            errors.addAll(employeeIds.stream().map(id -> "employeeId: " + id + " - Employee not found").collect(Collectors.toList()));
        }
        response.setTotalRows(bulkEmployeeDeletionRequestDtoList.size());
        response.setTotalEmployees(employeeIds.size());
        response.setTotalDependents(0);
        response.setSuccessCount(employeeIds.size() - errors.size());
        response.setErrorCount(errors.size());
        response.setErrors(errors);
        response.setMessage(errors.isEmpty() ? "No Validation errors!" : "Validation errors");
        return response;
    } catch (Exception e) {
        return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), e.getMessage(), 0, 0);
    }
   }

    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "EMPLOYEE", action = "BULK_DELETE")
    public EmployeeUploadResponse deleteEmployee(List<BulkEmployeeDeletionRequestDto> bulkEmployeeDeletionRequestDtoList, Organization organization, AdminUser adminUser, MultipartFile file, String uploadType) {
        try{
            Set<UUID> individualIdsToDelete = new HashSet<>();
            HashMap<String, LocalDate> dateOfExitMap = new HashMap<>();
            HashMap<String, String> reasonForExitMap = new HashMap<>();
            for (BulkEmployeeDeletionRequestDto bulkEmployeeDeletionRequestDto : bulkEmployeeDeletionRequestDtoList) {
                    dateOfExitMap.put(bulkEmployeeDeletionRequestDto.getEmployeeId(),EmployeeToDeals.parseDate(bulkEmployeeDeletionRequestDto.getDateOfExit()));
                    reasonForExitMap.put(bulkEmployeeDeletionRequestDto.getEmployeeId(),bulkEmployeeDeletionRequestDto.getDeletionReason());
            }
            List<String> errors = new ArrayList<>();
            int deletedCount = 0;
            int employeeCount = 0;
            int dependentCount = 0;
            EmployeeUploadResponse validateResponse = validateBulkEmployeeDeletion(bulkEmployeeDeletionRequestDtoList, organization);
            if(validateResponse.getSuccessCount() == 0) {
                return validateResponse;
            }
            for (Map.Entry<String, List<BulkEmployeeDeletionRequestDto>> entry : groupByEmployeeIdForBulkEmployeeDeletion(bulkEmployeeDeletionRequestDtoList).entrySet()) {
                List<BulkEmployeeDeletionRequestDto> bulkEmployeeDeletionRequestDtoListByEmployeeId = entry.getValue();
                for (BulkEmployeeDeletionRequestDto bulkEmployeeDeletionRequestDto : bulkEmployeeDeletionRequestDtoListByEmployeeId) {
                    if(bulkEmployeeDeletionRequestDto.getRelationship().equalsIgnoreCase("Self")) {
                        Optional<Deals> deal = dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(bulkEmployeeDeletionRequestDto.getEmployeeId(), organization.getOrganizationId(), "SELF");
                        if(deal.isPresent()) {
                            if(!deal.get().getStatus().equals(AccountStatus.ACTIVE)) {
                                errors.add("employeeId: " + bulkEmployeeDeletionRequestDto.getEmployeeId() + " - Employee is not active");
                                continue;
                            }
                            List<Deals> dependents = dealsRepository.findByPrimaryIndividualIdIn(List.of(deal.get().getIndividualId()));
                            dependents.forEach(dependent -> individualIdsToDelete.add(dependent.getIndividualId()));
                            individualIdsToDelete.add(deal.get().getIndividualId());
                            employeeCount++;
                            dependentCount += dependents.size();
                        }
                        else {
                            errors.add("employeeId: " + bulkEmployeeDeletionRequestDto.getEmployeeId() + " - Employee not found");
                        }
                    } else {
                        final int childIndexFromJson = extractChildIndexFromString(bulkEmployeeDeletionRequestDto.getRelationship());
                        String relationship = mapRelationshipToNomineeRelationship(bulkEmployeeDeletionRequestDto.getRelationship(), childIndexFromJson);
                        Optional<Deals> deal = dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(bulkEmployeeDeletionRequestDto.getEmployeeId(), organization.getOrganizationId(), relationship);
                        if(deal.isPresent()) {
                            if(!deal.get().getStatus().equals(AccountStatus.ACTIVE)) {
                                errors.add("employeeId: " + bulkEmployeeDeletionRequestDto.getEmployeeId() + " - Dependent is not active");
                                continue;
                            }
                            if(individualIdsToDelete.contains(deal.get().getIndividualId())) {
                                continue;
                            }
                            individualIdsToDelete.add(deal.get().getIndividualId());
                            dependentCount++;
                        }
                        else {
                            errors.add("employeeId: " + bulkEmployeeDeletionRequestDto.getEmployeeId() + " - Dependent not found");
                        }
                    }
                }
            }
            if(!errors.isEmpty()) {
                return new EmployeeUploadResponse(0, 0, errors.size(), errors, "Errors occurred while deleting employees", 0, 0);
            }
            if(individualIdsToDelete.isEmpty()) {
                return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), "No individuals to delete", 0, 0);
            }
            if(!individualIdsToDelete.isEmpty()) {
            List<Deals> dealsToDelete = dealsRepository.findByIndividualIdIn(new ArrayList<>(individualIdsToDelete));
            Set<Long> policyIds = employeePolicyMapRepository.findAll().stream()
                    .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()) && individualIdsToDelete.contains(m.getIndividualId()))
                    .map(EmployeePolicyMap::getPolicyId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (policyIds.isEmpty()) {
                policyIds.add(null);
            }
            UUID splitGroupId = UUID.randomUUID();
            Endorsement primaryEndorsement = null;
            List<Endorsement> splitEndorsements = new ArrayList<>();
            for (Long policyId : policyIds) {
                Endorsement endorsement = new Endorsement();
                endorsement.setOrganization(organization);
                endorsement.setStatus(AccountStatus.PENDING_EXIT);
                endorsement.setEndorsementType(getEndorsementType(uploadType));
                endorsement.setConfirmationMethod(ConfirmationMethod.PORTAL);
                endorsement.setCreatedAt(LocalDateTime.now());
                endorsement.setUpdatedAt(LocalDateTime.now());
                endorsement.setUploadedBy(adminUser);
                endorsement.setTotalEmployees(employeeCount);
                endorsement.setTotalDependents(dependentCount);
                endorsement.setSplitGroupId(splitGroupId);
                if (policyId != null) {
                    endorsement.setPolicy(policyRepository.findById(policyId).orElse(null));
                }
                if (primaryEndorsement != null) {
                    endorsement.setParentEndorsement(primaryEndorsement);
                }
                Endorsement saved = endorsementRepository.save(endorsement);
                if (primaryEndorsement == null) {
                    primaryEndorsement = saved;
                }
                splitEndorsements.add(saved);
            }
            if (file != null && primaryEndorsement != null) {
                try {
                    Document document = uploadDocuments(file, organization, adminUser, primaryEndorsement);
                    primaryEndorsement.setDocument(document);
                    endorsementRepository.save(primaryEndorsement);
                } catch (DocumentUploadException e) {
                    log.error("Failed to upload document for endorsement {}: {}", primaryEndorsement.getEndorsementId(), e.getMessage(), e);
                    return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), "Failed to upload documents: " + e.getMessage(), 0, 0);
                }
            }
            dealsToDelete.forEach(deal -> deal.setStatus(AccountStatus.PENDING_EXIT));
            dealsToDelete.forEach(deal -> deal.setDateOfExit(dateOfExitMap.get(deal.getEmployeeNumber())));
            dealsToDelete.forEach(deal -> deal.setReasonForExit(reasonForExitMap.get(deal.getEmployeeNumber())));
            dealsToDelete.forEach(deal -> deal.setUpdatedAt(LocalDateTime.now()));
            if (primaryEndorsement != null) {
                UUID primaryEndorsementId = primaryEndorsement.getEndorsementId();
                dealsToDelete.forEach(deal -> deal.setEndorsementId(primaryEndorsementId));
            }
            
            // Save deals first, then create DealEndorsement relationships
            // Maintain endorsement history: preserve existing relationships, only create new ones
            // A deal can have multiple endorsements, and all historical relationships must be preserved
            List<Deals> savedDeals = dealsRepository.saveAll(dealsToDelete);
            List<DealEndorsement> dealEndorsements = new ArrayList<>();
            for (Deals deal : savedDeals) {
              for (Endorsement splitEndorsement : splitEndorsements) {
                  boolean exists = dealEndorsementRepository.existsByDeal_IndividualIdAndEndorsement_EndorsementId(
                          deal.getIndividualId(), splitEndorsement.getEndorsementId());
                  if (!exists) {
                      DealEndorsement dealEndorsement = new DealEndorsement();
                      dealEndorsement.setDeal(deal);
                      dealEndorsement.setEndorsement(splitEndorsement);
                      dealEndorsements.add(dealEndorsement);
                  }
              }
            }
            if (!dealEndorsements.isEmpty()) {
              dealEndorsementRepository.saveAll(dealEndorsements);
            }
            deletedCount = dealsToDelete.size();
            if (primaryEndorsement != null) {
                slackNotificationUtil.sendSlackMessage(slackNotificationUtil.buildEndorsementNotificationMessage(primaryEndorsement), false);
            }
        }
            return new EmployeeUploadResponse(deletedCount, deletedCount, 0, new ArrayList<>(), "Employees" + "(" + employeeCount + ")" + " and dependents" + "(" + dependentCount + ")" + " deleted successfully", employeeCount, dependentCount);
        }
        catch (Exception e) { 
            log.error("Error deleting employees: {}", e.getMessage(), e);
            return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), "ERROR OCCURED WHILE DELETING EMPLOYEES", 0, 0);
        }
    }   



    /**
     * Extracts child index from relationship string
     * Examples: "Child1" -> 1, "Child2" -> 2, "Child" -> 0 (no index)
     */
    private int extractChildIndexFromString(String relationship) {
        if (relationship == null || relationship.trim().isEmpty()) {
            return 0;
        }
        String rel = relationship.trim();
        
        // Check if it's "Child" without index
        if ("Child".equalsIgnoreCase(rel)) {
            return 0;
        }
        
        // Check if it starts with "Child" and has a number
        if (rel.toUpperCase().startsWith("CHILD")) {
            String remaining = rel.substring(5).trim(); // Remove "Child" prefix
            if (remaining.isEmpty()) {
                return 0;
            }
            try {
                // Try to parse the number (e.g., "1", "2", "3", "4")
                return Integer.parseInt(remaining);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        return 0;
    }

    /**
     * Normalize Father-in-law/Mother-in-law variants to canonical enum-style values.
     * Accepts: "Father in law", "FatherInLaw", "FATHER_IN_LAW", "father-in-law", etc.
     */
    private String normalizeInLawRelationship(String relationship) {
        if (relationship == null) return null;

        String compact = relationship
                .trim()
                // remove whitespace, underscores, and hyphens so "mother-in-law" and "MOTHER_IN_LAW" both match.
                .replaceAll("[\\s_-]+", "")
                .toUpperCase(Locale.ROOT);

        if ("FATHERINLAW".equals(compact)) {
            return NomineeRelationship.FATHER_IN_LAW.getValue();
        }
        if ("MOTHERINLAW".equals(compact)) {
            return NomineeRelationship.MOTHER_IN_LAW.getValue();
        }
        return null;
    }

    private boolean isFatherInLawRelationship(String relationship) {
        return NomineeRelationship.FATHER_IN_LAW.getValue().equals(normalizeInLawRelationship(relationship));
    }

    private boolean isMotherInLawRelationship(String relationship) {
        return NomineeRelationship.MOTHER_IN_LAW.getValue().equals(normalizeInLawRelationship(relationship));
    }

    private boolean isParentRelationship(String relationship) {
        if (relationship == null) return false;
        return "FATHER".equalsIgnoreCase(relationship)
                || "MOTHER".equalsIgnoreCase(relationship)
                || isFatherInLawRelationship(relationship)
                || isMotherInLawRelationship(relationship);
    }

    /**
     * Maps input relationship string to NomineeRelationship enum value (as string)
     * Maps: Self -> SELF, Spouse -> SPOUSE, Father -> FATHER, Mother -> MOTHER, 
     *       Father in law -> FATHER_IN_LAW, Mother in law -> MOTHER_IN_LAW,
     *       Child -> CHILD1/CHILD2/CHILD3/CHILD4
     */
    private String mapRelationshipToNomineeRelationship(String relationship, int childIndex) {
        if (relationship == null || relationship.trim().isEmpty()) {
            return null;
        }
        String rel = relationship.trim();

        // Handle in-law variants early (e.g. "mother-in-law" and "MOTHER_IN_LAW" both map here)
        String inLaw = normalizeInLawRelationship(rel);
        if (inLaw != null) {
            return inLaw;
        }
        
        if ("Self".equalsIgnoreCase(rel)) {
            return NomineeRelationship.SELF.getValue();
        } else if ("Spouse".equalsIgnoreCase(rel)) {
            return NomineeRelationship.SPOUSE.getValue();
        } else if ("Father".equalsIgnoreCase(rel)) {
            return NomineeRelationship.FATHER.getValue();
        } else if ("Mother".equalsIgnoreCase(rel)) {
            return NomineeRelationship.MOTHER.getValue();
        } else if (rel.equalsIgnoreCase("Father in law") || rel.equalsIgnoreCase("FatherInLaw") || 
                   rel.equalsIgnoreCase("FATHER_IN_LAW")) {
            return NomineeRelationship.FATHER_IN_LAW.getValue();
        } else if (rel.equalsIgnoreCase("Mother in law") || rel.equalsIgnoreCase("MotherInLaw") || 
                   rel.equalsIgnoreCase("MOTHER_IN_LAW")) {
            return NomineeRelationship.MOTHER_IN_LAW.getValue();
        } else if ("Child".equalsIgnoreCase(rel) || rel.toUpperCase().startsWith("CHILD")) {
            // Map Child to CHILD1, CHILD2, CHILD3, or CHILD4 based on index
            return switch (childIndex) {
                case 1 -> NomineeRelationship.CHILD1.getValue();
                case 2 -> NomineeRelationship.CHILD2.getValue();
                case 3 -> NomineeRelationship.CHILD3.getValue();
                case 4 -> NomineeRelationship.CHILD4.getValue();
                default -> NomineeRelationship.CHILD1.getValue(); // Fallback
            };
        }
        
        // If we reach here, the relationship is not supported
        throw new IllegalArgumentException("Unsupported relationship: " + relationship + 
            ". Allowed relationships: Self, Spouse, Father, Mother, Father in law, Mother in law, Child1, Child2, Child3, Child4");
    }
    
    /**
     * Validates if the relationship is one of the allowed values
     * Allowed: Self, Spouse, Father, Mother, Father in law, Mother in law, Child1, Child2, Child3, Child4
     */
    private boolean isValidRelationship(String relationship) {
        if (relationship == null || relationship.trim().isEmpty()) {
            return false;
        }
        String rel = relationship.trim();

        // Accept common in-law variants: "mother-in-law", "Mother in law", "MOTHER_IN_LAW", etc.
        if (normalizeInLawRelationship(rel) != null) {
            return true;
        }
        
        // Check for allowed relationships
        if ("Self".equalsIgnoreCase(rel)) {
            return true;
        } else if ("Spouse".equalsIgnoreCase(rel)) {
            return true;
        } else if ("Father".equalsIgnoreCase(rel)) {
            return true;
        } else if ("Mother".equalsIgnoreCase(rel)) {
            return true;
        } else if (rel.equalsIgnoreCase("Father in law") || rel.equalsIgnoreCase("FatherInLaw") || 
                   rel.equalsIgnoreCase("FATHER_IN_LAW")) {
            return true;
        } else if (rel.equalsIgnoreCase("Mother in law") || rel.equalsIgnoreCase("MotherInLaw") || 
                   rel.equalsIgnoreCase("MOTHER_IN_LAW")) {
            return true;
        } else if (rel.toUpperCase().startsWith("CHILD")) {
            // Check if it's Child1, Child2, Child3, or Child4
            int childIndex = extractChildIndexFromString(rel);
            return childIndex >= 1 && childIndex <= 4;
        }
        
        return false;
    }


    /**
     * Uploads a document for an endorsement and returns the created Document entity.
     * 
     * @param file The file to upload
     * @param organization The organization associated with the document
     * @param adminUser The user uploading the document
     * @param endorsement The endorsement this document is associated with
     * @return The created Document entity
     * @throws DocumentUploadException if the upload fails or document cannot be found after upload
     */
    public Document uploadDocuments(MultipartFile file, Organization organization, AdminUser adminUser, Endorsement endorsement) 
        throws DocumentUploadException {
        try {
            String documentId = iDocumentService.uploadDocument(
                file, 
                DocumentType.ENDORSEMENT.getValue(), 
                DocumentCategory.ENDORSEMENT_DOCUMENTS.getValue(), 
                DocumentEntityType.ORGANIZATION.getValue(), 
                endorsement.getEndorsementId().toString(), 
                ""
            ).getBody().getPayload();
            
            if (documentId == null || documentId.isEmpty()) {
                throw new DocumentUploadException("Document upload returned null or empty ID for endorsement: " + endorsement.getEndorsementId());
            }
            
            return documentRepository.findByDocumentId(UUID.fromString(documentId))
                .orElseThrow(() -> new DocumentUploadException(
                    "Document with ID " + documentId + " not found after upload for endorsement: " + endorsement.getEndorsementId()));
                
        } catch (DocumentUploadException e) {
            // Re-throw specific exceptions
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid document ID format for endorsement {}: {}", endorsement.getEndorsementId(), e.getMessage(), e);
            throw new DocumentUploadException("Invalid document ID format for endorsement: " + endorsement.getEndorsementId(), e);
        } catch (Exception e) {
            log.error("Error uploading document for endorsement {}: {}", endorsement.getEndorsementId(), e.getMessage(), e);
            throw new DocumentUploadException("Failed to upload document for endorsement: " + endorsement.getEndorsementId() + ". " + e.getMessage(), e);
        }
    }

    private Map<Long, List<Deals>> mapDealsByPolicyForUpload(
            List<Deals> savedDeals,
            Map<String, List<EmployeeUploadDto>> groupedByEmployeeId,
            Map<Long, Policy> selectedPolicyMap) {
        Map<Long, List<Deals>> result = new LinkedHashMap<>();
        for (Long policyId : selectedPolicyMap.keySet()) {
            result.put(policyId, new ArrayList<>());
        }
        Map<String, EmployeeUploadDto> selfByEmployeeNumber = new HashMap<>();
        groupedByEmployeeId.forEach((employeeId, rows) -> {
            EmployeeUploadDto self = rows.stream()
                    .filter(r -> r != null && r.getRelationship() != null && "Self".equalsIgnoreCase(r.getRelationship()))
                    .findFirst()
                    .orElse(null);
            if (self != null) {
                selfByEmployeeNumber.put(employeeId, self);
            }
        });
        Policy parentPolicy = selectedPolicyMap.values().stream()
                .filter(p -> p != null && p.getProductType() == ProductType.PARENT_GMC)
                .findFirst().orElse(null);
        Policy topupPolicy = selectedPolicyMap.values().stream()
                .filter(p -> p != null && p.getProductType() == ProductType.TOP_UP)
                .findFirst().orElse(null);
        Policy superTopupPolicy = selectedPolicyMap.values().stream()
                .filter(p -> p != null && p.getProductType() == ProductType.SUPER_TOP_UP)
                .findFirst().orElse(null);
        for (Deals deal : savedDeals) {
            Set<Long> policyIdsForDeal = new LinkedHashSet<>();
            boolean parentMember = isParentRelationship(deal.getRelationship());
            if (parentMember && parentPolicy != null) {
                policyIdsForDeal.add(parentPolicy.getPolicyId());
            } else {
                selectedPolicyMap.values().stream()
                        .filter(p -> p != null && p.getProductType() != ProductType.PARENT_GMC
                                && p.getProductType() != ProductType.TOP_UP
                                && p.getProductType() != ProductType.SUPER_TOP_UP)
                        .forEach(p -> policyIdsForDeal.add(p.getPolicyId()));
            }
            if ("SELF".equalsIgnoreCase(deal.getRelationship())) {
                EmployeeUploadDto selfDto = selfByEmployeeNumber.get(deal.getEmployeeNumber());
                if (selfDto != null && topupPolicy != null && selfDto.getTopupSumInsured() != null && !selfDto.getTopupSumInsured().trim().isEmpty()) {
                    policyIdsForDeal.add(topupPolicy.getPolicyId());
                }
                if (selfDto != null && superTopupPolicy != null && selfDto.getSuperTopupSumInsured() != null && !selfDto.getSuperTopupSumInsured().trim().isEmpty()) {
                    policyIdsForDeal.add(superTopupPolicy.getPolicyId());
                }
            }
            for (Long policyId : policyIdsForDeal) {
                result.computeIfAbsent(policyId, k -> new ArrayList<>()).add(deal);
            }
        }
        return result;
    }

    public static EndorsementType getEndorsementType(String uploadType) {
        if(uploadType.equalsIgnoreCase("addition")) {
            return EndorsementType.ADDITION;
        } else if(uploadType.equalsIgnoreCase("deletion")) {
            return EndorsementType.DELETION;
        } else if(uploadType.equalsIgnoreCase("bulk-upload")) {
            return EndorsementType.BULK_UPLOAD;
        }
        throw new IllegalArgumentException("Invalid upload type: " + uploadType);
    }

    /**
     * Manual delete: same process as bulk delete (/delete) but without file.
     * Creates a Deletion endorsement with status PENDING_EXIT and sets employee (and dependents) status to PENDING_EXIT.
     * Used by POST /organization/{orgId}/employees/manual-delete.
     */
    @Transactional(rollbackFor = Exception.class)
    public EmployeeUploadResponse deleteEmployeeManual(List<String> employeeIds, Organization organization, AdminUser adminUser) {
        try {
            if (employeeIds == null || employeeIds.isEmpty()) {
                return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), "Employee IDs list cannot be empty", 0, 0);
            }
            Set<UUID> individualIdsToDelete = new HashSet<>();
            int employeeCount = 0;
            int dependentCount = 0;
            List<String> errors = new ArrayList<>();

            for (String employeeId : employeeIds) {
                Optional<Deals> dealOpt = dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(
                    employeeId, organization.getOrganizationId(), "SELF");
                if (dealOpt.isEmpty()) {
                    errors.add("employeeId: " + employeeId + " - Employee not found");
                    continue;
                }
                Deals deal = dealOpt.get();
                if (!AccountStatus.ACTIVE.equals(deal.getStatus())) {
                    errors.add("employeeId: " + employeeId + " - Employee is not active");
                    continue;
                }
                List<Deals> dependents = dealsRepository.findByPrimaryIndividualIdIn(List.of(deal.getIndividualId()));
                dependents.forEach(d -> individualIdsToDelete.add(d.getIndividualId()));
                individualIdsToDelete.add(deal.getIndividualId());
                employeeCount++;
                dependentCount += dependents.size();
            }

            if (!errors.isEmpty()) {
                return new EmployeeUploadResponse(0, 0, errors.size(), errors, "Errors occurred while processing employees", 0, 0);
            }
            if (individualIdsToDelete.isEmpty()) {
                return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), "No individuals to delete", 0, 0);
            }

            List<Deals> dealsToDelete = dealsRepository.findByIndividualIdIn(new ArrayList<>(individualIdsToDelete));
            Set<Long> policyIds = employeePolicyMapRepository.findAll().stream()
                    .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()) && individualIdsToDelete.contains(m.getIndividualId()))
                    .map(EmployeePolicyMap::getPolicyId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (policyIds.isEmpty()) {
                policyIds.add(null);
            }
            UUID splitGroupId = UUID.randomUUID();
            Endorsement primaryEndorsement = null;
            List<Endorsement> splitEndorsements = new ArrayList<>();
            for (Long policyId : policyIds) {
                Endorsement endorsement = new Endorsement();
                endorsement.setOrganization(organization);
                endorsement.setStatus(AccountStatus.PENDING_EXIT);
                endorsement.setEndorsementType(EndorsementType.DELETION);
                endorsement.setConfirmationMethod(ConfirmationMethod.PORTAL);
                endorsement.setCreatedAt(LocalDateTime.now());
                endorsement.setUpdatedAt(LocalDateTime.now());
                endorsement.setUploadedBy(adminUser);
                endorsement.setTotalEmployees(employeeCount);
                endorsement.setTotalDependents(dependentCount);
                endorsement.setSplitGroupId(splitGroupId);
                if (policyId != null) {
                    endorsement.setPolicy(policyRepository.findById(policyId).orElse(null));
                }
                if (primaryEndorsement != null) {
                    endorsement.setParentEndorsement(primaryEndorsement);
                }
                Endorsement saved = endorsementRepository.save(endorsement);
                if (primaryEndorsement == null) {
                    primaryEndorsement = saved;
                }
                splitEndorsements.add(saved);
            }
            dealsToDelete.forEach(deal -> deal.setStatus(AccountStatus.PENDING_EXIT));
            dealsToDelete.forEach(deal -> deal.setUpdatedAt(LocalDateTime.now()));
            if (primaryEndorsement != null) {
                UUID primaryEndorsementId = primaryEndorsement.getEndorsementId();
                dealsToDelete.forEach(deal -> deal.setEndorsementId(primaryEndorsementId));
            }

            List<Deals> savedDeals = dealsRepository.saveAll(dealsToDelete);
            List<DealEndorsement> dealEndorsements = new ArrayList<>();
            for (Deals deal : savedDeals) {
                for (Endorsement splitEndorsement : splitEndorsements) {
                    boolean exists = dealEndorsementRepository.existsByDeal_IndividualIdAndEndorsement_EndorsementId(
                        deal.getIndividualId(), splitEndorsement.getEndorsementId());
                    if (!exists) {
                        DealEndorsement dealEndorsement = new DealEndorsement();
                        dealEndorsement.setDeal(deal);
                        dealEndorsement.setEndorsement(splitEndorsement);
                        dealEndorsements.add(dealEndorsement);
                    }
                }
            }
            if (!dealEndorsements.isEmpty()) {
                dealEndorsementRepository.saveAll(dealEndorsements);
            }

            if (primaryEndorsement != null) {
                slackNotificationUtil.sendSlackMessage(slackNotificationUtil.buildEndorsementNotificationMessage(primaryEndorsement), false);
            }

            String message = String.format("Employees (%d) and dependents (%d) submitted for deletion successfully. Endorsement created with status Pending.", employeeCount, dependentCount);
            return new EmployeeUploadResponse(dealsToDelete.size(), dealsToDelete.size(), 0, new ArrayList<>(), message, employeeCount, dependentCount);
        } catch (Exception e) {
            log.error("Error in deleteEmployeeManual: {}", e.getMessage(), e);
            return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), "ERROR OCCURRED WHILE SUBMITTING EMPLOYEES FOR DELETION", 0, 0);
        }
    }
}
