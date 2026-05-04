package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.vimainsurance.vimaadmin.dto.EnrollmentWindowRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowStatsDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.SelfEmployeeEnrollmentRequestDto;
import com.vimainsurance.vimaadmin.dto.DependentEnrollmentUpdateDto;
import com.vimainsurance.vimaadmin.dto.ValidateEmployeesRequestDto;
import com.vimainsurance.vimaadmin.service.IEnrollmentWindowService;

/**
 * Controller for Enrollment Window CRUD and actions.
 */
@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/admin/enrollment-windows")
public class EnrollmentWindowsController {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentWindowsController.class);

    @Autowired
    private IEnrollmentWindowService enrollmentWindowService;

    /**
     * Create a new enrollment window (no employees or file; use upload endpoint for that).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> create(@RequestBody EnrollmentWindowRequestDto requestDto) {
        logger.info("[correlationId:{}] POST /api/admin/enrollment-windows called", MDC.get("correlationId"));
        return enrollmentWindowService.create(requestDto);
    }

    /**
     * Validate employees for enrollment (no window or employees created).
     * Mirrors upload-time business validations so create can be safely blocked before persistence.
     */
    @PostMapping(value = "/validate-employees", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<String>>> validateEmployees(@RequestBody ValidateEmployeesRequestDto requestDto) {
        logger.info("[correlationId:{}] POST /api/admin/enrollment-windows/validate-employees called", MDC.get("correlationId"));
        List<SelfEmployeeEnrollmentRequestDto> dtos = requestDto.getSelfEmployeeEnrollmentRequestDtos();
        Map<String, List<DependentEnrollmentUpdateDto>> deps = requestDto.getDependentsByEmployeeId();
        return enrollmentWindowService.validateEmployees(
                requestDto.getOrganizationId(),
                dtos != null ? dtos : List.of(),
                deps != null ? deps : Map.of());
    }

    /**
     * Validate uploaded employee file (SELF + dependents) before window creation.
     */
    @PostMapping(value = "/validate-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<String>>> validateUpload(
            @RequestParam("organizationId") UUID organizationId,
            @RequestPart("file") MultipartFile file) {
        logger.info("[correlationId:{}] POST /api/admin/enrollment-windows/validate-upload called", MDC.get("correlationId"));
        return enrollmentWindowService.validateUploadFile(organizationId, file);
    }

    /**
     * Upload employees and document for an existing enrollment window.
     */
    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> upload(
            @PathVariable UUID id,
            @RequestPart(value = "selfEmployeeEnrollmentRequestDtos", required = false) List<SelfEmployeeEnrollmentRequestDto> selfEmployeeEnrollmentRequestDtos,
            @RequestPart("file") MultipartFile file) {
        logger.info("[correlationId:{}] POST /api/admin/enrollment-windows/{}/upload called", MDC.get("correlationId"), id);
        return enrollmentWindowService.uploadEmployees(id, selfEmployeeEnrollmentRequestDtos, file);
    }

    /**
     * List enrollment windows with filters and pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<EnrollmentWindowResponseDto>>> getAllWithFilters(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortDirection) {
        logger.info("[correlationId:{}] GET /api/admin/enrollment-windows called", MDC.get("correlationId"));
        return enrollmentWindowService.getAllWithFilters(
                organizationId, status, name, fromDate, toDate, page, size, sortBy, sortDirection);
    }

    /**
     * Get enrollment window by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> getById(@PathVariable UUID id) {
        logger.info("[correlationId:{}] GET /api/admin/enrollment-windows/{} called", MDC.get("correlationId"), id);
        return enrollmentWindowService.getById(id);
    }

    /**
     * Update an enrollment window (e.g. inline date edit on details page).
     * HR_ADMIN may update only their organization's windows (enforced by tenant filter).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody EnrollmentWindowRequestDto requestDto) {
        logger.info("[correlationId:{}] PUT /api/admin/enrollment-windows/{} called", MDC.get("correlationId"), id);
        return enrollmentWindowService.update(id, requestDto);
    }

    /**
     * Manually activate a scheduled enrollment window
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<String>> activate(@PathVariable UUID id) {
        logger.info("[correlationId:{}] POST /api/admin/enrollment-windows/{}/activate called", MDC.get("correlationId"), id);
        return enrollmentWindowService.activate(id);
    }

    /**
     * Close an enrollment window
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<String>> close(@PathVariable UUID id) {
        logger.info("[correlationId:{}] POST /api/admin/enrollment-windows/{}/close called", MDC.get("correlationId"), id);
        return enrollmentWindowService.close(id);
    }

    /**
     * Soft delete an enrollment window (sets status to CANCELLED)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> delete(@PathVariable UUID id) {
        logger.info("[correlationId:{}] DELETE /api/admin/enrollment-windows/{} called", MDC.get("correlationId"), id);
        return enrollmentWindowService.delete(id);
    }

    /**
     * Get statistics for an enrollment window (invitations and submissions by status)
     */
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<EnrollmentWindowStatsDto>> getStats(@PathVariable UUID id) {
        logger.info("[correlationId:{}] GET /api/admin/enrollment-windows/{}/stats called", MDC.get("correlationId"), id);
        return enrollmentWindowService.getStats(id);
    }

    /**
     * Download CSV of all employees and dependents linked to this window (columns aligned with bulk endorsement upload).
     */
    @GetMapping(value = "/{id}/employees/export-csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    public ResponseEntity<StreamingResponseBody> exportEmployeesCsv(@PathVariable UUID id) {
        logger.info("[correlationId:{}] GET /api/admin/enrollment-windows/{}/employees/export-csv called", MDC.get("correlationId"), id);
        return enrollmentWindowService.exportEmployeesCsv(id);
    }
}
