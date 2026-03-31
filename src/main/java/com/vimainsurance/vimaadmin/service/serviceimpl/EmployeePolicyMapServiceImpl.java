package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.BulkEmployeePolicyMapRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.DealEndorsement;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EmployeePolicyMap;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.InsuranceProvider;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.entity.ProductCatalog;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.mapper.EmployeePolicyMapMapper;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDealEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IEmployeePolicyMapRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.repository.IProductCatalogRepository;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;

@Service
public class EmployeePolicyMapServiceImpl implements IEmployeePolicyMapService {

    private static final Logger log = LoggerFactory.getLogger(EmployeePolicyMapServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int BATCH_SIZE = 500;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Autowired
    private IEmployeePolicyMapRepository employeePolicyMapRepository;
    @Autowired
    private IDealsRepository dealsRepository;
    @Autowired
    private IDealEndorsementRepository dealEndorsementRepository;
    @Autowired
    private IPolicyRepository policyRepository;
    @Autowired
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;
    @Autowired
    private IInsuranceProviderRepository insuranceProviderRepository;
    @Autowired
    private IEndorsementRepository endorsementRepository;
    @Autowired
    private IProductCatalogRepository productCatalogRepository;

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "employee_policy_map", entityType = "EMPLOYEE_POLICY_MAP", action = "CREATE")
    public ResponseEntity<ResponseDto<EmployeePolicyMapResponseDto>> createMapping(EmployeePolicyMapRequestDto dto) {
        BaseResponse<EmployeePolicyMapResponseDto> responseObj = new BaseResponse<>();
        try {
            if (dto.getIndividualId() == null || dto.getPolicyId() == null || dto.getOrganizationId() == null
                    || dto.getEffectiveFrom() == null) {
                return responseObj.render(responseObj.formErrorResponse("Missing required fields"));
            }
            if (dealsRepository.findById(dto.getIndividualId()).isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Individual not found"));
            }
            if (policyRepository.findById(dto.getPolicyId()).isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Policy not found"));
            }
            if (employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(
                    dto.getIndividualId(), dto.getPolicyId(), STATUS_ACTIVE)) {
                return responseObj.render(responseObj.formErrorResponse(409, "Active mapping already exists for this individual and policy"));
            }
            EmployeePolicyMap entity = EmployeePolicyMapMapper.toEntity(dto);
            entity = employeePolicyMapRepository.save(entity);
            EmployeePolicyMapResponseDto response = buildResponseDto(entity);
            return responseObj.render(responseObj.formSuccessResponse("Mapping created", response));
        } catch (Exception e) {
            log.error("createMapping error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to create mapping"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "employee_policy_map", entityType = "EMPLOYEE_POLICY_MAP", action = "CREATE")
    public ResponseEntity<ResponseDto<String>> createBulkMappings(BulkEmployeePolicyMapRequestDto dto, String source) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            List<EmployeePolicyMap> toSave = new ArrayList<>();
            for (BulkEmployeePolicyMapRequestDto.IndividualMappingDto ind : dto.getIndividuals()) {
                EmployeePolicyMap map = EmployeePolicyMap.builder()
                        .individualId(ind.getIndividualId())
                        .primaryEmployeeId(ind.getPrimaryEmployeeId())
                        .relationship(ind.getRelationship())
                        .policyId(dto.getPolicyId())
                        .organizationId(dto.getOrganizationId())
                        .sumInsured(ind.getSumInsured())
                        .coverageTier(ind.getCoverageTier())
                        .isVoluntary(false)
                        .status(STATUS_ACTIVE)
                        .effectiveFrom(LocalDate.now())
                        .source(source != null ? source : "MANUAL")
                        .build();
                toSave.add(map);
            }
            for (int i = 0; i < toSave.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, toSave.size());
                employeePolicyMapRepository.saveAll(toSave.subList(i, end));
            }
            return responseObj.render(responseObj.formSuccessResponse("Bulk mappings created", "OK"));
        } catch (Exception e) {
            log.error("createBulkMappings error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to create bulk mappings"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "employee_policy_map", entityType = "EMPLOYEE_POLICY_MAP", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> cancelMapping(UUID mappingId, String reason) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            EmployeePolicyMap map = employeePolicyMapRepository.findById(mappingId).orElse(null);
            if (map == null) {
                return responseObj.render(responseObj.formErrorResponse(404, "Mapping not found"));
            }
            if (!STATUS_ACTIVE.equals(map.getStatus())) {
                return responseObj.render(responseObj.formErrorResponse(400, "Mapping is not active"));
            }
            map.setStatus(STATUS_CANCELLED);
            map.setEffectiveTo(LocalDate.now());
            map.setCancellationReason(reason);
            map.setCancelledAt(LocalDateTime.now());
            map.setUpdatedAt(LocalDateTime.now());
            employeePolicyMapRepository.save(map);
            return responseObj.render(responseObj.formSuccessResponse("Mapping cancelled", "OK"));
        } catch (Exception e) {
            log.error("cancelMapping error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to cancel mapping"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "employee_policy_map", entityType = "EMPLOYEE_POLICY_MAP", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> cancelAllForEmployee(UUID employeeId, LocalDate effectiveDate, String reason) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            int updated = employeePolicyMapRepository.cancelAllForEmployee(
                    employeeId, effectiveDate != null ? effectiveDate : LocalDate.now(), reason);
            return responseObj.render(responseObj.formSuccessResponse("Cancelled " + updated + " mapping(s)", "OK"));
        } catch (Exception e) {
            log.error("cancelAllForEmployee error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to cancel mappings"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForIndividual(UUID individualId) {
        BaseResponse<List<EmployeePolicyMapResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<EmployeePolicyMap> list = employeePolicyMapRepository.findByIndividualIdAndStatus(individualId, STATUS_ACTIVE);
            List<EmployeePolicyMapResponseDto> dtos = buildResponseDtos(list);
            return responseObj.render(responseObj.formSuccessResponse("OK", dtos));
        } catch (Exception e) {
            log.error("getMappingsForIndividual error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get mappings"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForPolicy(Long policyId) {
        BaseResponse<List<EmployeePolicyMapResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<EmployeePolicyMap> list = employeePolicyMapRepository.findByPolicyIdAndStatus(policyId, STATUS_ACTIVE);
            List<EmployeePolicyMapResponseDto> dtos = buildResponseDtos(list);
            return responseObj.render(responseObj.formSuccessResponse("OK", dtos));
        } catch (Exception e) {
            log.error("getMappingsForPolicy error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get mappings"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForEmployeeFamily(UUID employeeId) {
        BaseResponse<List<EmployeePolicyMapResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<EmployeePolicyMap> selfList = employeePolicyMapRepository.findByIndividualIdAndRelationshipAndStatus(
                    employeeId, "SELF", STATUS_ACTIVE);
            List<EmployeePolicyMap> dependentsList = employeePolicyMapRepository.findByPrimaryEmployeeIdAndStatus(employeeId, STATUS_ACTIVE);
            Set<UUID> seen = new HashSet<>();
            List<EmployeePolicyMap> combined = new ArrayList<>();
            for (EmployeePolicyMap m : selfList) {
                if (seen.add(m.getId())) {
                    combined.add(m);
                }
            }
            for (EmployeePolicyMap m : dependentsList) {
                if (seen.add(m.getId())) {
                    combined.add(m);
                }
            }
            List<EmployeePolicyMapResponseDto> dtos = buildResponseDtos(combined);
            return responseObj.render(responseObj.formSuccessResponse("OK", dtos));
        } catch (Exception e) {
            log.error("getMappingsForEmployeeFamily error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get mappings"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Page<EmployeePolicyMapResponseDto>>> getMappingsForOrganization(
            UUID organizationId, int page, int size, String status) {
        BaseResponse<Page<EmployeePolicyMapResponseDto>> responseObj = new BaseResponse<>();
        try {
            String st = status != null && !status.isBlank() ? status : STATUS_ACTIVE;
            Pageable pageable = PageRequest.of(page, size);
            var mapPage = employeePolicyMapRepository.findByOrganizationIdAndStatus(organizationId, st, pageable);
            List<EmployeePolicyMapResponseDto> dtos = buildResponseDtos(mapPage.getContent());
            Page<EmployeePolicyMapResponseDto> resultPage = new PageImpl<>(dtos, pageable, mapPage.getTotalElements());
            return responseObj.render(responseObj.formSuccessResponse("OK", resultPage));
        } catch (Exception e) {
            log.error("getMappingsForOrganization error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get mappings"));
        }
    }

    @Override
    public boolean isIndividualCoveredByPolicy(UUID individualId, Long policyId) {
        return employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(individualId, policyId, STATUS_ACTIVE);
    }

    @Override
    @Transactional
    public void createMappingsFromBulkUpload(UUID organizationId, List<UUID> employeeIds, String source, List<Long> policyIds) {
        if (organizationId == null || employeeIds == null || employeeIds.isEmpty()) {
            return;
        }
        if (policyIds == null || policyIds.isEmpty()) {
            throw new IllegalArgumentException("At least one policy must be selected");
        }
        List<Policy> policies = new ArrayList<>();
        for (Long id : policyIds) {
            Optional<Policy> opt = policyRepository.findById(id);
            if (opt.isEmpty()) {
                log.warn("createMappingsFromBulkUpload: policy {} not found, skipping", id);
                continue;
            }
            Policy p = opt.get();
            if (!organizationId.equals(p.getOrganizationId()) || p.getStatus() != PolicyStatus.ACTIVE
                    || !Boolean.TRUE.equals(p.getAppliesToEmployees())) {
                log.warn("createMappingsFromBulkUpload: policy {} not applicable for org {}, skipping", id, organizationId);
                continue;
            }
            policies.add(p);
        }
        if (policies.isEmpty()) {
            log.warn("createMappingsFromBulkUpload: no applicable policies for org {}", organizationId);
            return;
        }
        String src = source != null ? source : "BULK_UPLOAD";
        List<EmployeePolicyMap> toSave = new ArrayList<>();
        for (UUID employeeId : employeeIds) {
            Optional<Deals> employeeOpt = dealsRepository.findById(employeeId);
            if (employeeOpt.isEmpty()) {
                continue;
            }
            Deals employee = employeeOpt.get();
            LocalDate effectiveFrom = employee.getDateOfJoining() != null
                    ? employee.getDateOfJoining() : LocalDate.now();
            List<Deals> dependents = dealsRepository.findByPrimaryIndividualId(employeeId);
            for (Policy policy : policies) {
                if (policy.getProductType() == ProductType.PARENT_GMC && !hasParentDependent(dependents)) {
                    continue;
                }
                LocalDate from = policy.getStartDate() != null && policy.getStartDate().isAfter(effectiveFrom)
                        ? policy.getStartDate() : effectiveFrom;
                if (employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(employeeId, policy.getPolicyId(), STATUS_ACTIVE)) {
                    continue;
                }
                toSave.add(EmployeePolicyMap.builder()
                        .individualId(employeeId)
                        .primaryEmployeeId(null)
                        .relationship("SELF")
                        .policyId(policy.getPolicyId())
                        .organizationId(organizationId)
                        .sumInsured(policy.getSumInsured())
                        .isVoluntary(false)
                        .status(STATUS_ACTIVE)
                        .effectiveFrom(from)
                        .source(src)
                        .build());
            }
            for (Deals dep : dependents) {
                for (Policy policy : policies) {
                    if (policy.getProductType() == ProductType.GTL || policy.getProductType() == ProductType.GPA) {
                        continue;
                    }
                    if (policy.getProductType() == ProductType.PARENT_GMC && !isParentRelationship(dep.getRelationship())) {
                        continue;
                    }
                    if (employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(dep.getIndividualId(), policy.getPolicyId(), STATUS_ACTIVE)) {
                        continue;
                    }
                    LocalDate depFrom = effectiveFrom;
                    toSave.add(EmployeePolicyMap.builder()
                            .individualId(dep.getIndividualId())
                            .primaryEmployeeId(employeeId)
                            .relationship(dep.getRelationship() != null ? dep.getRelationship() : "OTHER")
                            .policyId(policy.getPolicyId())
                            .organizationId(organizationId)
                            .sumInsured(policy.getSumInsured())
                            .isVoluntary(false)
                            .status(STATUS_ACTIVE)
                            .effectiveFrom(depFrom)
                            .source(src)
                            .build());
                }
            }
        }
        for (int i = 0; i < toSave.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, toSave.size());
            employeePolicyMapRepository.saveAll(toSave.subList(i, end));
        }
        log.info("createMappingsFromBulkUpload: created {} mappings for org {}", toSave.size(), organizationId);
    }

    @Override
    @Transactional
    public void createMappingsFromEnrollmentSubmission(UUID submissionId) {
        if (submissionId == null) {
            return;
        }
        Optional<EnrollmentSubmission> subOpt = enrollmentSubmissionRepository.findById(submissionId);
        if (subOpt.isEmpty()) {
            return;
        }
        EnrollmentSubmission sub = subOpt.get();
        Deals employee = sub.getEmployee();
        if (employee == null || employee.getOrganization() == null) {
            return;
        }
        UUID orgId = employee.getOrganization().getOrganizationId();
        List<Policy> policies = getApplicablePolicies(orgId);
        if (policies.isEmpty()) {
            return;
        }
        Set<String> optedPlanTypes = parseOptedPlanTypes(sub.getPlanSelections());
        if (optedPlanTypes.isEmpty()) {
            optedPlanTypes = Set.of("GMC", "GPA", "GTL");
        }
        Map<String, Policy> policyByProductType = new HashMap<>();
        for (Policy p : policies) {
            String pt = p.getProductType() != null ? p.getProductType().name() : null;
            if (pt != null && optedPlanTypes.contains(pt) && !policyByProductType.containsKey(pt)) {
                policyByProductType.put(pt, p);
            }
        }
        List<EmployeePolicyMap> toSave = new ArrayList<>();
        LocalDate effectiveFrom = LocalDate.now();
        UUID windowId = sub.getEnrollmentWindow() != null ? sub.getEnrollmentWindow().getId() : null;
        for (Policy policy : policyByProductType.values()) {
            if (employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(employee.getIndividualId(), policy.getPolicyId(), STATUS_ACTIVE)) {
                continue;
            }
            toSave.add(EmployeePolicyMap.builder()
                    .individualId(employee.getIndividualId())
                    .primaryEmployeeId(null)
                    .relationship(employee.getRelationship() != null ? employee.getRelationship() : "SELF")
                    .policyId(policy.getPolicyId())
                    .organizationId(orgId)
                    .sumInsured(policy.getSumInsured())
                    .isVoluntary(false)
                    .status(STATUS_ACTIVE)
                    .effectiveFrom(effectiveFrom)
                    .source("ENROLLMENT")
                    .enrollmentWindowId(windowId)
                    .enrollmentSubmissionId(submissionId)
                    .build());
        }
        List<Deals> dependents = dealsRepository.findByPrimaryIndividualId(employee.getIndividualId());
        for (Deals dep : dependents) {
            for (Policy policy : policyByProductType.values()) {
                if (employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(dep.getIndividualId(), policy.getPolicyId(), STATUS_ACTIVE)) {
                    continue;
                }
                toSave.add(EmployeePolicyMap.builder()
                        .individualId(dep.getIndividualId())
                        .primaryEmployeeId(employee.getIndividualId())
                        .relationship(dep.getRelationship() != null ? dep.getRelationship() : "OTHER")
                        .policyId(policy.getPolicyId())
                        .organizationId(orgId)
                        .sumInsured(policy.getSumInsured())
                        .isVoluntary(false)
                        .status(STATUS_ACTIVE)
                        .effectiveFrom(effectiveFrom)
                        .source("ENROLLMENT")
                        .enrollmentWindowId(windowId)
                        .enrollmentSubmissionId(submissionId)
                        .build());
            }
        }
        for (int i = 0; i < toSave.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, toSave.size());
            employeePolicyMapRepository.saveAll(toSave.subList(i, end));
        }
        createMappingsForTopupFromSubmission(submissionId);
        createMappingsForParentFromSubmission(submissionId);
        log.info("createMappingsFromEnrollmentSubmission: created {} mappings for submission {}", toSave.size(), submissionId);
    }

    @Override
    @Transactional
    public void createMappingsForTopupFromSubmission(UUID submissionId) {
        if (submissionId == null) return;
        Optional<EnrollmentSubmission> subOpt = enrollmentSubmissionRepository.findById(submissionId);
        if (subOpt.isEmpty()) return;
        EnrollmentSubmission sub = subOpt.get();
        Deals employee = sub.getEmployee();
        if (employee == null || employee.getOrganization() == null) return;
        UUID orgId = employee.getOrganization().getOrganizationId();
        String planSelectionsJson = sub.getPlanSelections();
        if (planSelectionsJson == null || planSelectionsJson.isBlank()) return;
        try {
            JsonNode arr = OBJECT_MAPPER.readTree(planSelectionsJson);
            if (!arr.isArray()) return;
            List<EmployeePolicyMap> toSave = new ArrayList<>();
            LocalDate effectiveFrom = LocalDate.now();
            UUID windowId = sub.getEnrollmentWindow() != null ? sub.getEnrollmentWindow().getId() : null;
            for (JsonNode node : arr) {
                if (!node.has("opted") || !node.get("opted").asBoolean()) continue;
                if (!node.has("topupPlanOptionId") || node.get("topupPlanOptionId").isNull()) continue;
                String optIdStr = node.get("topupPlanOptionId").asText();
                UUID topupOptionId;
                try {
                    topupOptionId = UUID.fromString(optIdStr);
                } catch (Exception e) {
                    continue;
                }
                Optional<ProductCatalog> catalogOpt = productCatalogRepository.findById(topupOptionId);
                if (catalogOpt.isEmpty() || !catalogOpt.get().getOrganizationId().equals(orgId)) continue;
                ProductCatalog catalog = catalogOpt.get();
                Long policyId = catalog.getPolicyId() != null ? catalog.getPolicyId() : null;
                if (policyId == null) continue;
                java.math.BigDecimal sumInsured = node.has("sumInsured") && !node.get("sumInsured").isNull()
                        ? java.math.BigDecimal.valueOf(node.get("sumInsured").asDouble()) : null;
                boolean coversDependents = false;
                Optional<Policy> policyOpt = policyRepository.findById(policyId);
                if (policyOpt.isPresent()) {
                    coversDependents = Boolean.TRUE.equals(policyOpt.get().getCoversDependents());
                }
                if (!employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(employee.getIndividualId(), policyId, STATUS_ACTIVE)) {
                    toSave.add(EmployeePolicyMap.builder()
                            .individualId(employee.getIndividualId())
                            .primaryEmployeeId(null)
                            .relationship("SELF")
                            .policyId(policyId)
                            .organizationId(orgId)
                            .sumInsured(sumInsured)
                            .isVoluntary(true)
                            .status(STATUS_ACTIVE)
                            .effectiveFrom(effectiveFrom)
                            .source("ENROLLMENT")
                            .enrollmentWindowId(windowId)
                            .enrollmentSubmissionId(submissionId)
                            .build());
                }
                if (coversDependents) {
                    List<Deals> dependents = dealsRepository.findByPrimaryIndividualId(employee.getIndividualId());
                    for (Deals dep : dependents) {
                        if (employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(dep.getIndividualId(), policyId, STATUS_ACTIVE)) continue;
                        toSave.add(EmployeePolicyMap.builder()
                                .individualId(dep.getIndividualId())
                                .primaryEmployeeId(employee.getIndividualId())
                                .relationship(dep.getRelationship() != null ? dep.getRelationship() : "OTHER")
                                .policyId(policyId)
                                .organizationId(orgId)
                                .sumInsured(sumInsured)
                                .isVoluntary(true)
                                .status(STATUS_ACTIVE)
                                .effectiveFrom(effectiveFrom)
                                .source("ENROLLMENT")
                                .enrollmentWindowId(windowId)
                                .enrollmentSubmissionId(submissionId)
                                .build());
                    }
                }
            }
            for (int i = 0; i < toSave.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, toSave.size());
                employeePolicyMapRepository.saveAll(toSave.subList(i, end));
            }
            if (!toSave.isEmpty()) {
                log.info("createMappingsForTopupFromSubmission: created {} top-up mappings for submission {}", toSave.size(), submissionId);
            }
        } catch (Exception e) {
            log.warn("createMappingsForTopupFromSubmission failed for submission {}: {}", submissionId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void createMappingsForParentFromSubmission(UUID submissionId) {
        if (submissionId == null) return;
        Optional<EnrollmentSubmission> subOpt = enrollmentSubmissionRepository.findById(submissionId);
        if (subOpt.isEmpty()) return;
        EnrollmentSubmission sub = subOpt.get();
        Deals employee = sub.getEmployee();
        if (employee == null || employee.getOrganization() == null) return;
        UUID orgId = employee.getOrganization().getOrganizationId();
        List<Policy> policies = policyRepository.findByOrganizationIdAndStatus(orgId, PolicyStatus.ACTIVE);
        Optional<Policy> parentPolicyOpt = policies.stream()
                .filter(p -> p.getProductType() == com.vimainsurance.vimaadmin.enums.ProductType.PARENT_GMC)
                .findFirst();
        if (parentPolicyOpt.isEmpty()) return;
        Policy parentPolicy = parentPolicyOpt.get();
        List<Deals> dependents = dealsRepository.findByPrimaryIndividualId(employee.getIndividualId());
        List<Deals> parentDependents = dependents.stream()
                .filter(d -> "PARENT".equalsIgnoreCase(d.getRelationship()) || "PARENT_IN_LAW".equalsIgnoreCase(d.getRelationship()))
                .toList();
        if (parentDependents.isEmpty()) return;
        List<EmployeePolicyMap> toSave = new ArrayList<>();
        LocalDate effectiveFrom = LocalDate.now();
        UUID windowId = sub.getEnrollmentWindow() != null ? sub.getEnrollmentWindow().getId() : null;
        for (Deals parent : parentDependents) {
            if (employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(parent.getIndividualId(), parentPolicy.getPolicyId(), STATUS_ACTIVE)) continue;
            toSave.add(EmployeePolicyMap.builder()
                    .individualId(parent.getIndividualId())
                    .primaryEmployeeId(employee.getIndividualId())
                    .relationship(parent.getRelationship() != null ? parent.getRelationship() : "PARENT")
                    .policyId(parentPolicy.getPolicyId())
                    .organizationId(orgId)
                    .sumInsured(parentPolicy.getSumInsured())
                    .isVoluntary(true)
                    .status(STATUS_ACTIVE)
                    .effectiveFrom(effectiveFrom)
                    .source("ENROLLMENT")
                    .enrollmentWindowId(windowId)
                    .enrollmentSubmissionId(submissionId)
                    .build());
        }
        for (int i = 0; i < toSave.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, toSave.size());
            employeePolicyMapRepository.saveAll(toSave.subList(i, end));
        }
        if (!toSave.isEmpty()) {
            log.info("createMappingsForParentFromSubmission: created {} parent mappings for submission {}", toSave.size(), submissionId);
        }
    }

    @Override
    @Transactional
    public void createMappingsFromEndorsement(UUID endorsementId, String endorsementType) {
        if (endorsementId == null) {
            return;
        }
        Optional<Endorsement> endOpt = endorsementRepository.findByEndorsementId(endorsementId);
        if (endOpt.isEmpty()) {
            return;
        }
        Endorsement endorsement = endOpt.get();
        if (endorsement.getOrganization() == null) {
            return;
        }
        UUID orgId = endorsement.getOrganization().getOrganizationId();
        List<Deals> deals = dealsRepository.findByEndorsementId(endorsementId);
        if (deals.isEmpty() && dealEndorsementRepository != null) {
            Map<UUID, Deals> byId = new HashMap<>();
            List<DealEndorsement> links = dealEndorsementRepository.findByEndorsement_EndorsementId(endorsementId);
            if (links != null) {
                links.stream()
                        .map(DealEndorsement::getDeal)
                        .filter(d -> d != null && d.getIndividualId() != null)
                        .forEach(d -> byId.put(d.getIndividualId(), d));
            }
            deals = new ArrayList<>(byId.values());
        }
        if (deals.isEmpty()) {
            return;
        }
        List<Policy> policies;
        if (endorsement.getPolicy() != null
                && endorsement.getPolicy().getPolicyId() != null
                && Boolean.TRUE.equals(endorsement.getPolicy().getAppliesToEmployees())) {
            policies = List.of(endorsement.getPolicy());
        } else {
            // Legacy fallback (endorsements without linked policy)
            policies = getApplicablePolicies(orgId);
        }
        if (policies.isEmpty()) {
            return;
        }
        List<EmployeePolicyMap> toSave = new ArrayList<>();
        LocalDate effectiveFrom = LocalDate.now();
        for (Deals deal : deals) {
            UUID individualId = deal.getIndividualId();
            UUID primaryEmployeeId = deal.getPrimaryIndividual() != null ? deal.getPrimaryIndividual().getIndividualId() : null;
            String relationship = deal.getRelationship() != null ? deal.getRelationship() : "SELF";
            if ("SELF".equalsIgnoreCase(relationship)) {
                primaryEmployeeId = null;
            }
            for (Policy policy : policies) {
                if (!appliesPolicyToRelationship(policy, relationship)) {
                    continue;
                }
                if (employeePolicyMapRepository.existsByIndividualIdAndPolicyIdAndStatus(individualId, policy.getPolicyId(), STATUS_ACTIVE)) {
                    continue;
                }
                toSave.add(EmployeePolicyMap.builder()
                        .individualId(individualId)
                        .primaryEmployeeId(primaryEmployeeId)
                        .relationship(relationship)
                        .policyId(policy.getPolicyId())
                        .organizationId(orgId)
                        .sumInsured(policy.getSumInsured())
                        .isVoluntary(false)
                        .status(STATUS_ACTIVE)
                        .effectiveFrom(effectiveFrom)
                        .source("ENDORSEMENT")
                        .endorsementId(endorsementId)
                        .build());
            }
        }
        for (int i = 0; i < toSave.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, toSave.size());
            employeePolicyMapRepository.saveAll(toSave.subList(i, end));
        }
        log.info("createMappingsFromEndorsement: created {} mappings for endorsement {}", toSave.size(), endorsementId);
    }

    /**
     * Keep endorsement mapping aligned with policy rules:
     * GPA/GTL/TOP_UP/SUPER_TOP_UP => SELF only,
     * PARENT_GMC => parent / in-law only,
     * GMC/GHI/others => self + valid dependents.
     */
    private boolean appliesPolicyToRelationship(Policy policy, String relationship) {
        if (policy == null || policy.getProductType() == null) {
            return true;
        }
        String rel = relationship != null ? relationship.trim() : "";
        boolean isSelf = "SELF".equalsIgnoreCase(rel);
        ProductType pt = policy.getProductType();
        if (pt == ProductType.GPA || pt == ProductType.GTL || pt == ProductType.TOP_UP || pt == ProductType.SUPER_TOP_UP) {
            return isSelf;
        }
        if (pt == ProductType.PARENT_GMC) {
            return isParentRelationship(rel);
        }
        return true;
    }

    @Override
    @Transactional
    public void cancelMappingsFromEndorsement(UUID endorsementId) {
        if (endorsementId == null) {
            return;
        }
        List<Deals> deals = dealsRepository.findByEndorsementId(endorsementId);
        if (deals.isEmpty()) {
            return;
        }
        List<Long> topupPolicyIds = policyRepository.findPolicyIdsByProductTypeIn(List.of(ProductType.TOP_UP, ProductType.SUPER_TOP_UP));
        if (topupPolicyIds == null) {
            topupPolicyIds = List.of();
        }
        LocalDate effectiveTo = LocalDate.now();
        for (Deals deal : deals) {
            List<EmployeePolicyMap> forIndividual = employeePolicyMapRepository.findByIndividualIdAndStatus(deal.getIndividualId(), STATUS_ACTIVE);
            for (EmployeePolicyMap m : forIndividual) {
                cancelTopupMappingsIfBaseGmc(m.getIndividualId(), m.getPolicyId(), topupPolicyIds, effectiveTo);
                m.setStatus(STATUS_CANCELLED);
                m.setEffectiveTo(effectiveTo);
                m.setCancellationReason("ENDORSEMENT_DELETION");
                m.setCancelledAt(LocalDateTime.now());
                m.setUpdatedAt(LocalDateTime.now());
                employeePolicyMapRepository.save(m);
            }
            List<EmployeePolicyMap> forDependents = employeePolicyMapRepository.findByPrimaryEmployeeIdAndStatus(deal.getIndividualId(), STATUS_ACTIVE);
            for (EmployeePolicyMap m : forDependents) {
                cancelTopupMappingsIfBaseGmc(m.getIndividualId(), m.getPolicyId(), topupPolicyIds, effectiveTo);
                m.setStatus(STATUS_CANCELLED);
                m.setEffectiveTo(effectiveTo);
                m.setCancellationReason("ENDORSEMENT_DELETION");
                m.setCancelledAt(LocalDateTime.now());
                m.setUpdatedAt(LocalDateTime.now());
                employeePolicyMapRepository.save(m);
            }
        }
        log.info("cancelMappingsFromEndorsement: cancelled mappings for endorsement {}", endorsementId);
    }

    /**
     * If the given policy is base GMC (GMC and not a top-up policy), cancel any active top-up mappings
     * for the same individual with reason BASE_GMC_CANCELLED.
     */
    private void cancelTopupMappingsIfBaseGmc(UUID individualId, Long policyId, List<Long> topupPolicyIds, LocalDate effectiveTo) {
        if (individualId == null || policyId == null || topupPolicyIds.isEmpty()) {
            return;
        }
        Optional<Policy> policyOpt = policyRepository.findById(policyId);
        if (policyOpt.isEmpty() || policyOpt.get().getProductType() != ProductType.GMC) {
            return;
        }
        if (topupPolicyIds.contains(policyId)) {
            return;
        }
        List<EmployeePolicyMap> topupMappings = employeePolicyMapRepository.findByIndividualIdAndPolicyIdInAndStatus(individualId, topupPolicyIds, STATUS_ACTIVE);
        for (EmployeePolicyMap t : topupMappings) {
            t.setStatus(STATUS_CANCELLED);
            t.setEffectiveTo(effectiveTo);
            t.setCancellationReason("BASE_GMC_CANCELLED");
            t.setCancelledAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            employeePolicyMapRepository.save(t);
            log.info("cancelMappingsFromEndorsement: auto-cancelled top-up mapping {} for individual {} (BASE_GMC_CANCELLED)", t.getId(), individualId);
        }
    }

    private List<Policy> getApplicablePolicies(UUID organizationId) {
        List<Policy> all = policyRepository.findByOrganizationIdAndStatus(organizationId, PolicyStatus.ACTIVE);
        return all.stream().filter(p -> Boolean.TRUE.equals(p.getAppliesToEmployees())).collect(Collectors.toList());
    }

    /** True if the relationship indicates a parent (Father, Mother, Father-In-Law, Mother-In-Law, PARENT). Used to create PARENT_GMC mappings only when employee has parent dependents. */
    private static boolean isParentRelationship(String relationship) {
        if (relationship == null || relationship.isBlank()) return false;
        String r = relationship.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        return "FATHER".equals(r) || "MOTHER".equals(r) || "PARENT".equals(r)
                || "FATHER_IN_LAW".equals(r) || r.startsWith("FATHER_IN_LAW")
                || "MOTHER_IN_LAW".equals(r) || r.startsWith("MOTHER_IN_LAW");
    }

    /** True if the employee has at least one dependent with a parent relationship (Father, Mother, etc.). */
    private static boolean hasParentDependent(List<Deals> dependents) {
        if (dependents == null || dependents.isEmpty()) return false;
        return dependents.stream().anyMatch(d -> isParentRelationship(d.getRelationship()));
    }

    private Set<String> parseOptedPlanTypes(String planSelectionsJson) {
        Set<String> types = new HashSet<>();
        if (planSelectionsJson == null || planSelectionsJson.isBlank()) {
            return types;
        }
        try {
            JsonNode arr = OBJECT_MAPPER.readTree(planSelectionsJson);
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    if (node.has("opted") && node.get("opted").asBoolean() && node.has("planType")) {
                        types.add(node.get("planType").asText());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("parseOptedPlanTypes failed: {}", e.getMessage());
        }
        return types;
    }

    private EmployeePolicyMapResponseDto buildResponseDto(EmployeePolicyMap entity) {
        String individualName = null;
        String primaryEmployeeName = null;
        Optional<Deals> indOpt = dealsRepository.findById(entity.getIndividualId());
        if (indOpt.isPresent()) {
            Deals d = indOpt.get();
            individualName = d.getFullName() != null ? d.getFullName() : (d.getFirstName() + " " + (d.getLastName() != null ? d.getLastName() : "")).trim();
        }
        if (entity.getPrimaryEmployeeId() != null) {
            Optional<Deals> empOpt = dealsRepository.findById(entity.getPrimaryEmployeeId());
            if (empOpt.isPresent()) {
                Deals d = empOpt.get();
                primaryEmployeeName = d.getFullName() != null ? d.getFullName() : (d.getFirstName() + " " + (d.getLastName() != null ? d.getLastName() : "")).trim();
            }
        }
        String policyNumber = null;
        String productType = null;
        String insurerName = null;
        Optional<Policy> policyOpt = policyRepository.findById(entity.getPolicyId());
        if (policyOpt.isPresent()) {
            Policy p = policyOpt.get();
            policyNumber = p.getPolicyNumber();
            productType = p.getProductType() != null ? p.getProductType().name() : null;
            if (p.getInsuranceProviderId() != null) {
                Optional<InsuranceProvider> provOpt = insuranceProviderRepository.findById(p.getInsuranceProviderId());
                if (provOpt.isPresent()) {
                    insurerName = provOpt.get().getProviderName();
                }
            }
        }
        return EmployeePolicyMapMapper.toResponseDto(entity, individualName, primaryEmployeeName, policyNumber, productType, insurerName);
    }

    private List<EmployeePolicyMapResponseDto> buildResponseDtos(List<EmployeePolicyMap> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        Set<UUID> individualIds = new HashSet<>();
        Set<UUID> primaryIds = new HashSet<>();
        Set<Long> policyIds = new HashSet<>();
        for (EmployeePolicyMap m : list) {
            individualIds.add(m.getIndividualId());
            if (m.getPrimaryEmployeeId() != null) {
                primaryIds.add(m.getPrimaryEmployeeId());
            }
            policyIds.add(m.getPolicyId());
        }
        Map<UUID, String> individualNames = new HashMap<>();
        for (UUID id : individualIds) {
            individualNames.put(id, dealsRepository.findById(id).map(this::dealDisplayName).orElse(null));
        }
        for (UUID id : primaryIds) {
            individualNames.putIfAbsent(id, dealsRepository.findById(id).map(this::dealDisplayName).orElse(null));
        }
        Map<Long, Policy> policyMap = new HashMap<>();
        for (Long id : policyIds) {
            policyRepository.findById(id).ifPresent(p -> policyMap.put(id, p));
        }
        Map<UUID, String> insurerNames = new HashMap<>();
        for (Policy p : policyMap.values()) {
            if (p.getInsuranceProviderId() != null) {
                insuranceProviderRepository.findById(p.getInsuranceProviderId())
                        .ifPresent(ip -> insurerNames.put(p.getInsuranceProviderId(), ip.getProviderName()));
            }
        }
        List<EmployeePolicyMapResponseDto> result = new ArrayList<>();
        for (EmployeePolicyMap m : list) {
            String policyNumber = null;
            String productType = null;
            String insurerName = null;
            Policy p = policyMap.get(m.getPolicyId());
            if (p != null) {
                policyNumber = p.getPolicyNumber();
                productType = p.getProductType() != null ? p.getProductType().name() : null;
                if (p.getInsuranceProviderId() != null) {
                    insurerName = insurerNames.get(p.getInsuranceProviderId());
                }
            }
            result.add(EmployeePolicyMapMapper.toResponseDto(m,
                    individualNames.get(m.getIndividualId()),
                    m.getPrimaryEmployeeId() != null ? individualNames.get(m.getPrimaryEmployeeId()) : null,
                    policyNumber, productType, insurerName));
        }
        return result;
    }

    private String dealDisplayName(Deals d) {
        if (d.getFullName() != null && !d.getFullName().isBlank()) {
            return d.getFullName();
        }
        String first = d.getFirstName() != null ? d.getFirstName() : "";
        String last = d.getLastName() != null ? d.getLastName() : "";
        return (first + " " + last).trim();
    }
}
