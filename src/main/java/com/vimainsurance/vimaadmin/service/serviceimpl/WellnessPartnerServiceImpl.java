package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.config.MantraCareProperties;
import com.vimainsurance.vimaadmin.service.wellness.exception.MantraCareException;
import com.vimainsurance.vimaadmin.service.wellness.exception.MantraCareSigningUnavailableException;
import com.vimainsurance.vimaadmin.service.wellness.exception.WellnessConfigException;
import com.vimainsurance.vimaadmin.service.wellness.mantra.MantraCareClient;
import com.vimainsurance.vimaadmin.service.wellness.mantra.MantraCareSigningService;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessOrgAssignmentResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerOrgRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerOrgResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerReorderRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessRedirectResponseDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.entity.WellnessPartner;
import com.vimainsurance.vimaadmin.entity.WellnessPartnerOrganization;
import com.vimainsurance.vimaadmin.enums.WellnessCategory;
import com.vimainsurance.vimaadmin.enums.WellnessRedirectType;
import com.vimainsurance.vimaadmin.mapper.WellnessPartnerMapper;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IWellnessPartnerRepository;
import com.vimainsurance.vimaadmin.repository.IWellnessPartnerOrganizationRepository;
import com.vimainsurance.vimaadmin.service.IWellnessAccessService;
import com.vimainsurance.vimaadmin.service.IWellnessPartnerService;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WellnessPartnerServiceImpl implements IWellnessPartnerService {

    private static final Logger logger = LoggerFactory.getLogger(WellnessPartnerServiceImpl.class);
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final Pattern MANTRACARE_STATUS_PATTERN = Pattern.compile("MantraCare API error:\\s*(\\d{3})");

    private final IWellnessPartnerRepository wellnessPartnerRepository;
    private final IWellnessPartnerOrganizationRepository wellnessPartnerOrganizationRepository;
    private final IOrganizationRepository organizationRepository;
    private final IDealsRepository dealsRepository;
    private final JwtUserExtractor jwtUserExtractor;
    private final IWellnessAccessService wellnessAccessService;
    private final MantraCareProperties mantraCareProperties;
    private final MantraCareSigningService mantraCareSigningService;
    private final MantraCareClient mantraCareClient;

    @Override
    public ResponseEntity<ResponseDto<List<WellnessPartnerResponseDto>>> getAllPartners() {
        BaseResponse<List<WellnessPartnerResponseDto>> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] getAllPartners called", MDC.get("correlationId"));
        try {
            List<WellnessPartnerResponseDto> response = wellnessPartnerRepository.findAll().stream()
                    .map(this::toResponseDtoWithOrganizationCount)
                    .toList();
            return responseObj.render(responseObj.formSuccessResponse("Wellness partners fetched successfully", response));
        } catch (Exception e) {
            logger.error("[correlationId:{}] getAllPartners failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to fetch wellness partners"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> getPartnerById(UUID id) {
        BaseResponse<WellnessPartnerResponseDto> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] getPartnerById called for id {}", MDC.get("correlationId"), id);
        try {
            if (id == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "id is required"));
            }
            Optional<WellnessPartner> partnerOpt = wellnessPartnerRepository.findById(id);
            if (partnerOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Wellness partner not found"));
            }
            return responseObj.render(responseObj.formSuccessResponse(
                    "Wellness partner fetched successfully",
                    toResponseDtoWithOrganizationCount(partnerOpt.get())));
        } catch (Exception e) {
            logger.error("[correlationId:{}] getPartnerById failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to fetch wellness partner"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> createPartner(WellnessPartnerRequestDto requestDto) {
        BaseResponse<WellnessPartnerResponseDto> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] createPartner called for slug {}", MDC.get("correlationId"), requestDto.getSlug());
        try {
            String normalizedSlug = normalizeSlug(requestDto.getSlug());
            validateCreateOrUpdateRequest(requestDto, null, normalizedSlug);

            requestDto.setSlug(normalizedSlug);
            WellnessPartner partner = WellnessPartnerMapper.mapToEntity(requestDto);
            partner.setCreatedAt(LocalDateTime.now());
            partner.setUpdatedAt(LocalDateTime.now());
            partner = wellnessPartnerRepository.save(partner);

            return responseObj.render(responseObj.formSuccessResponse(
                    "Wellness partner created successfully",
                    toResponseDtoWithOrganizationCount(partner)));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (IllegalStateException e) {
            return responseObj.render(responseObj.formErrorResponse(409, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] createPartner failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to create wellness partner"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> updatePartner(UUID id, WellnessPartnerRequestDto requestDto) {
        BaseResponse<WellnessPartnerResponseDto> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] updatePartner called for id {}", MDC.get("correlationId"), id);
        try {
            if (id == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "id is required"));
            }
            Optional<WellnessPartner> partnerOpt = wellnessPartnerRepository.findById(id);
            if (partnerOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Wellness partner not found"));
            }

            WellnessPartner existingPartner = partnerOpt.get();
            String normalizedSlug = normalizeSlug(requestDto.getSlug());
            validateCreateOrUpdateRequest(requestDto, existingPartner, normalizedSlug);

            requestDto.setSlug(normalizedSlug);
            WellnessPartnerMapper.updateEntityFromDto(existingPartner, requestDto);
            existingPartner.setUpdatedAt(LocalDateTime.now());
            WellnessPartner saved = wellnessPartnerRepository.save(existingPartner);

            return responseObj.render(responseObj.formSuccessResponse(
                    "Wellness partner updated successfully",
                    toResponseDtoWithOrganizationCount(saved)));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (IllegalStateException e) {
            return responseObj.render(responseObj.formErrorResponse(409, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] updatePartner failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to update wellness partner"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> activatePartner(UUID id) {
        logger.info("[correlationId:{}] activatePartner called for id {}", MDC.get("correlationId"), id);
        return setPartnerActiveState(id, true);
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> deactivatePartner(UUID id) {
        logger.info("[correlationId:{}] deactivatePartner called for id {}", MDC.get("correlationId"), id);
        return setPartnerActiveState(id, false);
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<String>> deletePartner(UUID id) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] deletePartner called for id {}", MDC.get("correlationId"), id);
        try {
            if (id == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "id is required"));
            }
            Optional<WellnessPartner> partnerOpt = wellnessPartnerRepository.findById(id);
            if (partnerOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Wellness partner not found"));
            }

            // DB-level FK handles cascading delete on org mappings.
            wellnessPartnerRepository.deleteById(id);
            return responseObj.render(responseObj.formSuccessResponse("Wellness partner deleted successfully", "DELETED"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] deletePartner failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to delete wellness partner"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<WellnessOrgAssignmentResponseDto>> getOrgAssignment(UUID orgId) {
        BaseResponse<WellnessOrgAssignmentResponseDto> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] getOrgAssignment called for orgId {}", MDC.get("correlationId"), orgId);
        try {
            if (orgId == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "organizationId is required"));
            }
            if (organizationRepository.findByOrganizationId(orgId).isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Organization not found"));
            }

            List<WellnessPartnerOrganization> assignedEntities = wellnessPartnerOrganizationRepository
                    .findByOrganizationIdOrderByDisplayOrderAsc(orgId);

            List<WellnessPartnerOrgResponseDto> assignedPartners = assignedEntities.stream()
                    .map(this::enrichOrgMappingWithPartner)
                    .sorted(Comparator
                            .comparing((WellnessPartnerOrgResponseDto dto) -> dto.getDisplayOrder() == null ? Integer.MAX_VALUE : dto.getDisplayOrder())
                            .thenComparing(dto -> dto.getPartnerName() == null ? "" : dto.getPartnerName(), String.CASE_INSENSITIVE_ORDER))
                    .toList();

            List<UUID> assignedPartnerIds = assignedEntities.stream()
                    .map(WellnessPartnerOrganization::getPartnerId)
                    .toList();

            List<WellnessPartnerResponseDto> availablePartners = wellnessPartnerRepository.findByIsActiveTrueOrderByNameAsc().stream()
                    .filter(partner -> !assignedPartnerIds.contains(partner.getId()))
                    .map(this::toResponseDtoWithOrganizationCount)
                    .toList();

            WellnessOrgAssignmentResponseDto payload = WellnessOrgAssignmentResponseDto.builder()
                    .assignedPartners(assignedPartners)
                    .availablePartners(availablePartners)
                    .build();
            return responseObj.render(responseObj.formSuccessResponse("Organization wellness assignment fetched successfully", payload));
        } catch (Exception e) {
            logger.error("[correlationId:{}] getOrgAssignment failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to fetch organization wellness assignments"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<WellnessPartnerOrgResponseDto>> assignPartnerToOrg(WellnessPartnerOrgRequestDto requestDto) {
        BaseResponse<WellnessPartnerOrgResponseDto> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] assignPartnerToOrg called for partnerId {} and orgId {}",
                MDC.get("correlationId"), requestDto.getPartnerId(), requestDto.getOrganizationId());
        try {
            validateOrgMappingRequest(requestDto);
            UUID partnerId = Objects.requireNonNull(requestDto.getPartnerId());
            UUID organizationId = Objects.requireNonNull(requestDto.getOrganizationId());

            if (wellnessPartnerOrganizationRepository.existsByPartnerIdAndOrganizationId(partnerId, organizationId)) {
                return responseObj.render(responseObj.formErrorResponse(409, "Partner is already assigned to this organization"));
            }

            Optional<WellnessPartner> partnerOpt = wellnessPartnerRepository.findById(partnerId);
            if (partnerOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Wellness partner not found"));
            }
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Organization not found"));
            }

            if (requestDto.getDisplayOrder() == null) {
                int maxDisplayOrder = wellnessPartnerOrganizationRepository.findMaxDisplayOrderByOrganizationId(organizationId);
                requestDto.setDisplayOrder(maxDisplayOrder + 1);
            }

            warnIfBackendTokenConfigMissing(partnerOpt.get(), requestDto.getConfig(), organizationId);

            WellnessPartnerOrganization mapping = WellnessPartnerMapper.mapOrgRequestToEntity(requestDto);
            mapping.setCreatedAt(LocalDateTime.now());
            mapping.setUpdatedAt(LocalDateTime.now());
            mapping = wellnessPartnerOrganizationRepository.save(mapping);
            mapping.setPartner(partnerOpt.get());

            return responseObj.render(responseObj.formSuccessResponse(
                    "Partner assigned to organization successfully",
                    WellnessPartnerMapper.mapToOrgResponseDto(mapping)));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] assignPartnerToOrg failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to assign partner to organization"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<WellnessPartnerOrgResponseDto>> updateOrgMapping(UUID id, WellnessPartnerOrgRequestDto requestDto) {
        BaseResponse<WellnessPartnerOrgResponseDto> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] updateOrgMapping called for id {}", MDC.get("correlationId"), id);
        try {
            if (id == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "id is required"));
            }
            validateOrgMappingRequest(requestDto);
            UUID partnerId = Objects.requireNonNull(requestDto.getPartnerId());
            UUID organizationId = Objects.requireNonNull(requestDto.getOrganizationId());

            Optional<WellnessPartnerOrganization> mappingOpt = wellnessPartnerOrganizationRepository.findById(id);
            if (mappingOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Organization partner mapping not found"));
            }

            Optional<WellnessPartner> partnerOpt = wellnessPartnerRepository.findById(partnerId);
            if (partnerOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Wellness partner not found"));
            }
            if (organizationRepository.findByOrganizationId(organizationId).isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Organization not found"));
            }

            WellnessPartnerOrganization existing = mappingOpt.get();
            boolean changingUniquePair = !existing.getPartnerId().equals(partnerId)
                    || !existing.getOrganizationId().equals(organizationId);
            if (changingUniquePair
                    && wellnessPartnerOrganizationRepository.existsByPartnerIdAndOrganizationId(partnerId, organizationId)) {
                return responseObj.render(responseObj.formErrorResponse(409, "Partner is already assigned to this organization"));
            }

            warnIfBackendTokenConfigMissing(partnerOpt.get(), requestDto.getConfig(), organizationId);

            WellnessPartnerMapper.updateOrgEntityFromDto(existing, requestDto);
            existing.setUpdatedAt(LocalDateTime.now());
            WellnessPartnerOrganization saved = wellnessPartnerOrganizationRepository.save(existing);
            saved.setPartner(partnerOpt.get());

            return responseObj.render(responseObj.formSuccessResponse(
                    "Organization partner mapping updated successfully",
                    WellnessPartnerMapper.mapToOrgResponseDto(saved)));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] updateOrgMapping failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to update organization partner mapping"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<List<WellnessPartnerOrgResponseDto>>> reorderPartnersForOrg(WellnessPartnerReorderRequestDto requestDto) {
        BaseResponse<List<WellnessPartnerOrgResponseDto>> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] reorderPartnersForOrg called for orgId {}", MDC.get("correlationId"), requestDto.getOrganizationId());
        try {
            if (requestDto.getOrganizationId() == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "organizationId is required"));
            }
            if (organizationRepository.findByOrganizationId(requestDto.getOrganizationId()).isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Organization not found"));
            }

            for (WellnessPartnerReorderRequestDto.PartnerOrderItem item : requestDto.getPartnerOrders()) {
                if (item.getId() == null) {
                    return responseObj.render(responseObj.formErrorResponse(400, "Mapping id is required"));
                }
                UUID mappingId = Objects.requireNonNull(item.getId());
                Optional<WellnessPartnerOrganization> mappingOpt = wellnessPartnerOrganizationRepository.findById(mappingId);
                if (mappingOpt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse(404, "Organization partner mapping not found: " + item.getId()));
                }
                WellnessPartnerOrganization mapping = mappingOpt.get();
                if (!requestDto.getOrganizationId().equals(mapping.getOrganizationId())) {
                    return responseObj.render(responseObj.formErrorResponse(400, "Mapping does not belong to organization: " + item.getId()));
                }
                mapping.setDisplayOrder(item.getDisplayOrder());
                mapping.setUpdatedAt(LocalDateTime.now());
                wellnessPartnerOrganizationRepository.save(mapping);
            }

            List<WellnessPartnerOrgResponseDto> response = wellnessPartnerOrganizationRepository
                    .findByOrganizationIdOrderByDisplayOrderAsc(requestDto.getOrganizationId()).stream()
                    .map(this::enrichOrgMappingWithPartner)
                    .sorted(Comparator
                            .comparing((WellnessPartnerOrgResponseDto dto) -> dto.getDisplayOrder() == null ? Integer.MAX_VALUE : dto.getDisplayOrder())
                            .thenComparing(dto -> dto.getPartnerName() == null ? "" : dto.getPartnerName(), String.CASE_INSENSITIVE_ORDER))
                    .toList();

            return responseObj.render(responseObj.formSuccessResponse("Organization partner order updated successfully", response));
        } catch (Exception e) {
            logger.error("[correlationId:{}] reorderPartnersForOrg failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to reorder organization partners"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<String>> removePartnerFromOrg(UUID id) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] removePartnerFromOrg called for id {}", MDC.get("correlationId"), id);
        try {
            if (id == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "id is required"));
            }
            if (wellnessPartnerOrganizationRepository.findById(id).isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Organization partner mapping not found"));
            }
            wellnessPartnerOrganizationRepository.deleteById(id);
            return responseObj.render(responseObj.formSuccessResponse("Partner removed from organization successfully", "DELETED"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] removePartnerFromOrg failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to remove partner from organization"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<WellnessPartnerResponseDto>>> getEmployeePartners(String category) {
        BaseResponse<List<WellnessPartnerResponseDto>> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] getEmployeePartners called for category {}", MDC.get("correlationId"), category);
        try {
            UUID organizationId = resolveCurrentOrganizationId();
            if (organizationId == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "Organization context not found"));
            }

            List<WellnessPartnerResponseDto> partners = wellnessPartnerOrganizationRepository
                    .findActivePartnersByOrganization(organizationId)
                    .stream()
                    .map(WellnessPartnerOrganization::getPartner)
                    .filter(Objects::nonNull)
                    .filter(partner -> category == null || category.isBlank() || partner.getCategory().equalsIgnoreCase(category))
                    .map(this::toResponseDtoWithOrganizationCount)
                    .toList();

            return responseObj.render(responseObj.formSuccessResponse("Employee wellness partners fetched successfully", partners));
        } catch (Exception e) {
            logger.error("[correlationId:{}] getEmployeePartners failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to fetch employee wellness partners"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<WellnessRedirectResponseDto>> getEmployeeRedirectUrl(String partnerSlug) {
        BaseResponse<WellnessRedirectResponseDto> responseObj = new BaseResponse<>();
        logger.info("[correlationId:{}] getEmployeeRedirectUrl called for slug {}", MDC.get("correlationId"), partnerSlug);
        UUID organizationId = null;
        UUID employeeId = null;
        String userIdentifier = jwtUserExtractor.extractCurrentUsername();
        UUID partnerId = null;
        String accessType = "LOGIN";
        try {
            if (partnerSlug == null || partnerSlug.isBlank()) {
                wellnessAccessService.logAccess(
                        null,
                        null,
                        null,
                        userIdentifier,
                        accessType,
                        "FAILED",
                        "partnerSlug is required");
                return responseObj.render(responseObj.formErrorResponse(400, "partnerSlug is required"));
            }

            organizationId = resolveCurrentOrganizationId();
            employeeId = jwtUserExtractor.getCurrentEmployeeId();
            if (organizationId == null) {
                wellnessAccessService.logAccess(
                        null,
                        null,
                        employeeId,
                        userIdentifier,
                        accessType,
                        "FAILED",
                        "Organization context not found");
                return responseObj.render(responseObj.formErrorResponse(400, "Organization context not found"));
            }

            Optional<WellnessPartnerOrganization> mappingOpt = wellnessPartnerOrganizationRepository
                    .findByPartner_SlugAndOrganizationId(partnerSlug.trim().toLowerCase(), organizationId);
            if (mappingOpt.isEmpty()) {
                wellnessAccessService.logAccess(
                        organizationId,
                        null,
                        employeeId,
                        userIdentifier,
                        accessType,
                        "FAILED",
                        "Wellness partner not assigned for this organization");
                return responseObj.render(responseObj.formErrorResponse(404, "Wellness partner not assigned for this organization"));
            }

            WellnessPartnerOrganization mapping = mappingOpt.get();
            WellnessPartner partner = mapping.getPartner();
            partnerId = mapping.getPartnerId();
            accessType = resolveAccessType(partner, mapping);
            if (Boolean.FALSE.equals(mapping.getIsActive()) || partner == null || Boolean.FALSE.equals(partner.getIsActive())) {
                wellnessAccessService.logAccess(
                        organizationId,
                        partnerId,
                        employeeId,
                        userIdentifier,
                        accessType,
                        "FAILED",
                        "Wellness partner is not active");
                return responseObj.render(responseObj.formErrorResponse(404, "Wellness partner is not active"));
            }

            if (employeeId == null) {
                wellnessAccessService.logAccess(
                        organizationId,
                        partnerId,
                        null,
                        userIdentifier,
                        accessType,
                        "FAILED",
                        "Employee context not found");
                return responseObj.render(responseObj.formErrorResponse(400, "Employee context not found"));
            }

            try {
                WellnessRedirectResponseDto payload;
                if ("mantracare".equalsIgnoreCase(partner.getSlug())
                        && "BACKEND_TOKEN".equalsIgnoreCase(partner.getRedirectType())) {
                    Deals employee = dealsRepository
                            .findById(employeeId)
                            .orElseThrow(() -> new WellnessConfigException("Employee record not found"));

                    String wellnessUserId = employee.getWellnessUserId();
                    if (wellnessUserId == null || wellnessUserId.isBlank()) {
                        wellnessUserId = UUID.randomUUID().toString();
                        employee.setWellnessUserId(wellnessUserId);
                        dealsRepository.save(employee);
                    }

                    Map<String, Object> meta = partner.getMetadata() != null ? partner.getMetadata() : Map.of();
                    Map<String, Object> orgCfg = mapping.getConfig() != null ? mapping.getConfig() : Map.of();

                    String apiBaseUrl = stringValue(meta.get("api_base_url"), "https://api.mantracare.com");
                    if (apiBaseUrl.endsWith("/")) {
                        apiBaseUrl = apiBaseUrl.substring(0, apiBaseUrl.length() - 1);
                    }
                    String setUserPath = stringValue(meta.get("set_user_path"), "/partner/user");
                    if (!setUserPath.startsWith("/")) {
                        setUserPath = "/" + setUserPath;
                    }
                    String inviteCode = stringOrNull(orgCfg.get("invite_code"));
                    String requestCookie = stringOrNull(meta.get("request_cookie"));
                    long tokenValidityMillis = numberValue(meta.get("token_validity_seconds"), 3600) * 1000L;

                    if (inviteCode == null || inviteCode.isBlank()) {
                        throw new WellnessConfigException("Wellness partner is not configured for token issuance");
                    }

                    String jws = mantraCareSigningService.signForEmployee(
                            mantraCareProperties.getKeyId(),
                            inviteCode,
                            tokenValidityMillis,
                            wellnessUserId);
                    userIdentifier = wellnessUserId;
                    if (mantraCareProperties.isEmployeeRedirectBrowserExchange()) {
                        String postUrl = apiBaseUrl + setUserPath;
                        payload = WellnessRedirectResponseDto.builder()
                                .redirectUrl(null)
                                .opensIn("new_tab")
                                .partnerExchangeToken(jws)
                                .partnerExchangePostUrl(postUrl)
                                .partnerSessionCookie(requestCookie)
                                .build();
                    } else {
                        int connectTimeoutMs = numberValue(meta.get("connect_timeout_ms"), 5000);
                        connectTimeoutMs = Math.min(15_000, Math.max(connectTimeoutMs, 1_000));
                        int readTimeoutMs = numberValue(meta.get("read_timeout_ms"), 120_000);
                        readTimeoutMs = Math.min(240_000, Math.max(readTimeoutMs, 120_000));
                        logger.info(
                                "[correlationId:{}] MantraCare server exchange timeouts: connectTimeoutMs={} readTimeoutMs={}",
                                MDC.get("correlationId"),
                                connectTimeoutMs,
                                readTimeoutMs);
                        String redirectFromPartner = mantraCareClient.exchangeTokenForRedirectUrl(
                                apiBaseUrl,
                                setUserPath,
                                connectTimeoutMs,
                                readTimeoutMs,
                                requestCookie,
                                jws);
                        payload = WellnessRedirectResponseDto.builder()
                                .redirectUrl(redirectFromPartner)
                                .opensIn("new_tab")
                                .build();
                    }
                } else {
                    String redirectUrl = mapping.getCustomRedirectUrl();
                    if (redirectUrl == null || redirectUrl.isBlank()) {
                        redirectUrl = partner.getRedirectUrl();
                    }
                    if (redirectUrl == null || redirectUrl.isBlank()) {
                        throw new WellnessConfigException("Redirect URL not configured for this partner");
                    }
                    payload = WellnessRedirectResponseDto.builder()
                            .redirectUrl(redirectUrl)
                            .opensIn("new_tab")
                            .build();
                }
                wellnessAccessService.logAccess(
                        organizationId,
                        partnerId,
                        employeeId,
                        userIdentifier,
                        accessType,
                        "SUCCESS",
                        null);
                return responseObj.render(responseObj.formSuccessResponse("Wellness redirect generated successfully", payload));
            } catch (WellnessConfigException e) {
                wellnessAccessService.logAccess(
                        organizationId,
                        partnerId,
                        employeeId,
                        userIdentifier,
                        accessType,
                        "FAILED",
                        e.getMessage());
                return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
            } catch (MantraCareSigningUnavailableException e) {
                wellnessAccessService.logAccess(
                        organizationId,
                        partnerId,
                        employeeId,
                        userIdentifier,
                        accessType,
                        "FAILED",
                        "Wellness partner integration unavailable");
                return responseObj.render(responseObj.formErrorResponse(503, "Wellness partner integration unavailable"));
            } catch (MantraCareException e) {
                int partnerStatus = extractMantraStatusCode(e.getMessage());
                int apiStatus = partnerStatus >= 400 && partnerStatus < 500 ? partnerStatus : 502;
                String apiMessage = apiStatus == 502 ? "Wellness partner is unreachable" : e.getMessage();
                wellnessAccessService.logAccess(
                        organizationId,
                        partnerId,
                        employeeId,
                        userIdentifier,
                        accessType,
                        "FAILED",
                        apiMessage);
                return responseObj.render(responseObj.formErrorResponse(apiStatus, apiMessage));
            }
        } catch (Exception e) {
            logger.error("[correlationId:{}] getEmployeeRedirectUrl failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            wellnessAccessService.logAccess(
                    organizationId,
                    partnerId,
                    employeeId,
                    userIdentifier,
                    accessType,
                    "FAILED",
                    e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to generate wellness redirect URL"));
        }
    }

    private ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> setPartnerActiveState(UUID id, boolean isActive) {
        BaseResponse<WellnessPartnerResponseDto> responseObj = new BaseResponse<>();
        try {
            if (id == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "id is required"));
            }
            Optional<WellnessPartner> partnerOpt = wellnessPartnerRepository.findById(id);
            if (partnerOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Wellness partner not found"));
            }

            WellnessPartner partner = partnerOpt.get();
            partner.setIsActive(isActive);
            partner.setUpdatedAt(LocalDateTime.now());
            WellnessPartner saved = wellnessPartnerRepository.save(partner);

            return responseObj.render(responseObj.formSuccessResponse(
                    isActive ? "Wellness partner activated successfully" : "Wellness partner deactivated successfully",
                    toResponseDtoWithOrganizationCount(saved)));
        } catch (Exception e) {
            logger.error("[correlationId:{}] setPartnerActiveState failed: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to update wellness partner status"));
        }
    }

    private void validateCreateOrUpdateRequest(WellnessPartnerRequestDto requestDto, WellnessPartner existing, String normalizedSlug) {
        validateSlug(normalizedSlug);
        validateCategory(requestDto.getCategory());
        validateRedirectType(requestDto.getRedirectType());

        boolean slugChanged = existing == null || !existing.getSlug().equalsIgnoreCase(normalizedSlug);
        if (slugChanged && wellnessPartnerRepository.existsBySlug(normalizedSlug)) {
            throw new IllegalStateException("Wellness partner slug already exists");
        }
    }

    private void validateSlug(String slug) {
        if (slug == null || !SLUG_PATTERN.matcher(slug).matches()) {
            throw new IllegalArgumentException("slug must be lowercase, URL-safe, and contain only alphanumeric characters and hyphens");
        }
    }

    private void validateCategory(String category) {
        WellnessCategory.fromValue(category);
    }

    private void validateRedirectType(String redirectType) {
        WellnessRedirectType.fromValue(redirectType);
    }

    private String normalizeSlug(String slug) {
        return slug == null ? null : slug.trim().toLowerCase();
    }

    private WellnessPartnerResponseDto toResponseDtoWithOrganizationCount(WellnessPartner partner) {
        long orgCount = wellnessPartnerRepository.countOrganizationsByPartnerId(partner.getId());
        return WellnessPartnerMapper.mapToResponseDto(partner, orgCount);
    }

    private WellnessPartnerOrgResponseDto enrichOrgMappingWithPartner(WellnessPartnerOrganization mapping) {
        WellnessPartner partner = mapping.getPartner();
        UUID partnerId = mapping.getPartnerId();
        if (partner == null && partnerId != null) {
            partner = wellnessPartnerRepository.findById(partnerId).orElse(null);
            mapping.setPartner(partner);
        }
        return WellnessPartnerMapper.mapToOrgResponseDto(mapping);
    }

    private void validateOrgMappingRequest(WellnessPartnerOrgRequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (requestDto.getPartnerId() == null) {
            throw new IllegalArgumentException("partnerId is required");
        }
        if (requestDto.getOrganizationId() == null) {
            throw new IllegalArgumentException("organizationId is required");
        }
    }

    private void warnIfBackendTokenConfigMissing(WellnessPartner partner, java.util.Map<String, Object> config, UUID organizationId) {
        if (partner == null || partner.getRedirectType() == null) {
            return;
        }
        if (!WellnessRedirectType.BACKEND_TOKEN.getValue().equalsIgnoreCase(partner.getRedirectType())) {
            return;
        }
        Object inviteCode = config != null ? config.get("invite_code") : null;
        if (inviteCode == null) {
            logger.warn(
                    "[correlationId:{}] BACKEND_TOKEN partner '{}' assigned/updated without invite_code for organization {}",
                    MDC.get("correlationId"),
                    partner.getSlug(),
                    organizationId);
        }
    }

    private static String stringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String out = value.toString().trim();
        return out.isEmpty() ? defaultValue : out;
    }

    private static String stringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String out = value.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private static int numberValue(Object value, int defaultValue) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static int extractMantraStatusCode(String message) {
        if (message == null || message.isBlank()) {
            return 502;
        }
        var matcher = MANTRACARE_STATUS_PATTERN.matcher(message);
        if (!matcher.find()) {
            return 502;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 502;
        }
    }

    private UUID resolveCurrentOrganizationId() {
        List<String> orgIds = jwtUserExtractor.getCurrentOrganizations();
        if (orgIds != null) {
            for (String orgId : orgIds) {
                try {
                    return UUID.fromString(orgId);
                } catch (IllegalArgumentException ignored) {
                    // Continue searching valid UUID in claim list.
                }
            }
        }

        java.util.Map<String, List<String>> tenantMap = TenantContext.getCurrentTenant();
        List<String> tenantOrgIds = tenantMap != null ? tenantMap.get("organizationIds") : null;
        if (tenantOrgIds != null) {
            for (String orgId : tenantOrgIds) {
                try {
                    return UUID.fromString(orgId);
                } catch (IllegalArgumentException ignored) {
                    // Continue searching valid UUID in tenant context list.
                }
            }
        }
        return null;
    }

    private String resolveAccessType(WellnessPartner partner, WellnessPartnerOrganization mapping) {
        if (partner == null || partner.getRedirectType() == null) {
            return "LOGIN";
        }
        if (WellnessRedirectType.DIRECT.getValue().equalsIgnoreCase(partner.getRedirectType())) {
            return "LOGIN";
        }
        if (WellnessRedirectType.BACKEND_TOKEN.getValue().equalsIgnoreCase(partner.getRedirectType())
                && mapping != null
                && mapping.getConfig() != null) {
            Object mappedAccessType = mapping.getConfig().get("access_type");
            if (mappedAccessType == null) {
                mappedAccessType = mapping.getConfig().get("accessType");
            }
            if (mappedAccessType != null && !mappedAccessType.toString().isBlank()) {
                String value = mappedAccessType.toString().trim().toUpperCase();
                if ("REGISTRATION".equals(value) || "LOGIN".equals(value)) {
                    return value;
                }
            }
        }
        return "LOGIN";
    }
}
