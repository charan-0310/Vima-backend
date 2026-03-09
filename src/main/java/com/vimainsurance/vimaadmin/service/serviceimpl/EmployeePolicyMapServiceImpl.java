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
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EmployeePolicyMap;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.InsuranceProvider;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.mapper.EmployeePolicyMapMapper;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEmployeePolicyMapRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
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
    private IPolicyRepository policyRepository;
    @Autowired
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;
    @Autowired
    private IInsuranceProviderRepository insuranceProviderRepository;
    @Autowired
    private IEndorsementRepository endorsementRepository;

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
    public void createMappingsFromBulkUpload(UUID organizationId, List<UUID> employeeIds, String source) {
        if (organizationId == null || employeeIds == null || employeeIds.isEmpty()) {
            return;
        }
        List<Policy> policies = getApplicablePolicies(organizationId);
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
            for (Policy policy : policies) {
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
            List<Deals> dependents = dealsRepository.findByPrimaryIndividualId(employeeId);
            for (Deals dep : dependents) {
                for (Policy policy : policies) {
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
        log.info("createMappingsFromEnrollmentSubmission: created {} mappings for submission {}", toSave.size(), submissionId);
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
        if (deals.isEmpty()) {
            return;
        }
        List<Policy> policies = getApplicablePolicies(orgId);
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
        LocalDate effectiveTo = LocalDate.now();
        for (Deals deal : deals) {
            List<EmployeePolicyMap> forIndividual = employeePolicyMapRepository.findByIndividualIdAndStatus(deal.getIndividualId(), STATUS_ACTIVE);
            for (EmployeePolicyMap m : forIndividual) {
                m.setStatus(STATUS_CANCELLED);
                m.setEffectiveTo(effectiveTo);
                m.setCancellationReason("ENDORSEMENT_DELETION");
                m.setCancelledAt(LocalDateTime.now());
                m.setUpdatedAt(LocalDateTime.now());
                employeePolicyMapRepository.save(m);
            }
            List<EmployeePolicyMap> forDependents = employeePolicyMapRepository.findByPrimaryEmployeeIdAndStatus(deal.getIndividualId(), STATUS_ACTIVE);
            for (EmployeePolicyMap m : forDependents) {
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

    private List<Policy> getApplicablePolicies(UUID organizationId) {
        List<Policy> all = policyRepository.findByOrganizationIdAndStatus(organizationId, PolicyStatus.ACTIVE);
        return all.stream().filter(p -> Boolean.TRUE.equals(p.getAppliesToEmployees())).collect(Collectors.toList());
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
