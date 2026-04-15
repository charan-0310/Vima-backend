package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessOrgAssignmentResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerOrgRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerOrgResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerReorderRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerResponseDto;
import com.vimainsurance.vimaadmin.service.IWellnessPartnerService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/admin/wellness/partners")
public class AdminWellnessController {

    private static final Logger logger = LoggerFactory.getLogger(AdminWellnessController.class);

    private final IWellnessPartnerService wellnessPartnerService;

    public AdminWellnessController(IWellnessPartnerService wellnessPartnerService) {
        this.wellnessPartnerService = wellnessPartnerService;
    }

    @GetMapping
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<List<WellnessPartnerResponseDto>>> getAllPartners() {
        logger.info("[correlationId:{}] GET /api/v1/admin/wellness/partners/ called", MDC.get("correlationId"));
        return wellnessPartnerService.getAllPartners();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> getPartnerById(@PathVariable UUID id) {
        logger.info("[correlationId:{}] GET /api/v1/admin/wellness/partners/{} called", MDC.get("correlationId"), id);
        return wellnessPartnerService.getPartnerById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> createPartner(@Valid @RequestBody WellnessPartnerRequestDto requestDto) {
        logger.info("[correlationId:{}] POST /api/v1/admin/wellness/partners/ called", MDC.get("correlationId"));
        return wellnessPartnerService.createPartner(requestDto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> updatePartner(
            @PathVariable UUID id,
            @Valid @RequestBody WellnessPartnerRequestDto requestDto) {
        logger.info("[correlationId:{}] PUT /api/v1/admin/wellness/partners/{} called", MDC.get("correlationId"), id);
        return wellnessPartnerService.updatePartner(id, requestDto);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> activatePartner(@PathVariable UUID id) {
        logger.info("[correlationId:{}] PATCH /api/v1/admin/wellness/partners/{}/activate called", MDC.get("correlationId"), id);
        return wellnessPartnerService.activatePartner(id);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> deactivatePartner(@PathVariable UUID id) {
        logger.info("[correlationId:{}] PATCH /api/v1/admin/wellness/partners/{}/deactivate called", MDC.get("correlationId"), id);
        return wellnessPartnerService.deactivatePartner(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> deletePartner(@PathVariable UUID id) {
        logger.info("[correlationId:{}] DELETE /api/v1/admin/wellness/partners/{} called", MDC.get("correlationId"), id);
        return wellnessPartnerService.deletePartner(id);
    }

    @GetMapping("/organizations/{orgId}")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<WellnessOrgAssignmentResponseDto>> getOrgAssignment(@PathVariable UUID orgId) {
        logger.info("[correlationId:{}] GET /api/v1/admin/wellness/partners/organizations/{} called", MDC.get("correlationId"), orgId);
        return wellnessPartnerService.getOrgAssignment(orgId);
    }

    @PostMapping("/organizations")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<WellnessPartnerOrgResponseDto>> assignPartnerToOrg(
            @Valid @RequestBody WellnessPartnerOrgRequestDto requestDto) {
        logger.info("[correlationId:{}] POST /api/v1/admin/wellness/partners/organizations called", MDC.get("correlationId"));
        return wellnessPartnerService.assignPartnerToOrg(requestDto);
    }

    @PutMapping("/organizations/{id}")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<WellnessPartnerOrgResponseDto>> updateOrgMapping(
            @PathVariable UUID id,
            @Valid @RequestBody WellnessPartnerOrgRequestDto requestDto) {
        logger.info("[correlationId:{}] PUT /api/v1/admin/wellness/partners/organizations/{} called", MDC.get("correlationId"), id);
        return wellnessPartnerService.updateOrgMapping(id, requestDto);
    }

    @PutMapping("/organizations/reorder")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<List<WellnessPartnerOrgResponseDto>>> reorderPartnersForOrg(
            @Valid @RequestBody WellnessPartnerReorderRequestDto requestDto) {
        logger.info("[correlationId:{}] PUT /api/v1/admin/wellness/partners/organizations/reorder called", MDC.get("correlationId"));
        return wellnessPartnerService.reorderPartnersForOrg(requestDto);
    }

    @DeleteMapping("/organizations/{id}")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> removePartnerFromOrg(@PathVariable UUID id) {
        logger.info("[correlationId:{}] DELETE /api/v1/admin/wellness/partners/organizations/{} called", MDC.get("correlationId"), id);
        return wellnessPartnerService.removePartnerFromOrg(id);
    }
}
