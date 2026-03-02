package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.dto.claim.ClaimSubmissionRequest;
import com.vimainsurance.vimaadmin.dto.claim.ClaimSummaryDto;
import com.vimainsurance.vimaadmin.dto.claim.EmployeeClaimSubmitResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.EmployeeClaimsSummaryResponse;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.SubmissionSource;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.mapper.ClaimMapper;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IClaimsService;
import com.vimainsurance.vimaadmin.service.IEmployeeClaimsService;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeClaimsServiceImpl implements IEmployeeClaimsService {

    private static final String CLAIM_SUBMITTED_MESSAGE = "Claim submitted successfully";

    private final IClaimsService claimsService;
    private final IClaimRepository claimRepository;
    private final IPolicyRepository policyRepository;
    private final JwtUserExtractor jwtUserExtractor;

    @Override
    @AuditedOperation(schemaName = "claims", tableName = "claims", entityType = "CLAIM", action = "SUBMIT")
    public ResponseEntity<ResponseDto<EmployeeClaimSubmitResponseDto>> submitClaim(ClaimSubmissionRequest request) {
        UUID employeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Employee context (employee_id or individual_id) not found in token."));
        }

        if (request.getOrganizationId() == null && request.getPolicyId() != null) {
            resolveOrganizationFromPolicy(request);
        }
        if (request.getSubmissionSource() == null) {
            request.setSubmissionSource(SubmissionSource.EMPLOYEE_PORTAL);
        }

        try {
            ClaimDetailsResponse created = claimsService.submitClaim(request, employeeId);
            EmployeeClaimSubmitResponseDto payload = EmployeeClaimSubmitResponseDto.builder()
                    .id(created.getId())
                    .claimNumber(created.getClaimNumber())
                    .internalStatus(created.getInternalStatus())
                    .message(CLAIM_SUBMITTED_MESSAGE)
                    .build();
            return ResponseEntity.ok(new ResponseDto<>(CLAIM_SUBMITTED_MESSAGE, payload));
        } catch (BadRequestException e) {
            log.warn("[employee-claims] submitClaim validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Page<ClaimSummaryDto>>> getMyClaims(
            String status,
            String claimType,
            String dateFrom,
            String dateTo,
            Pageable pageable) {
        UUID employeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Employee context not found in token."));
        }

        ClaimListFilters filters = new ClaimListFilters();
        filters.setEmployeeId(employeeId);
        filters.setIsDeleted(false);
        if (status != null && !status.isBlank()) {
            try {
                filters.setInternalStatus(ClaimStatus.fromValue(status.trim()));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid status
            }
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            try {
                filters.setDateOfSubmissionFrom(LocalDate.parse(dateFrom.trim()));
            } catch (Exception ignored) {
            }
        }
        if (dateTo != null && !dateTo.isBlank()) {
            try {
                filters.setDateOfSubmissionTo(LocalDate.parse(dateTo.trim()));
            } catch (Exception ignored) {
            }
        }
        // claimType filter would require adding to ClaimListFilters and ClaimSpecification; skip for minimal scope

        Page<ClaimDetailsResponse> page = claimsService.listClaims(filters, pageable);
        Page<ClaimSummaryDto> summaryPage = page.map(ClaimMapper::toSummaryFromDetails);
        return ResponseEntity.ok(new ResponseDto<>("Success", summaryPage, summaryPage.getTotalElements()));
    }

    @Override
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> getClaimDetails(UUID claimId) {
        UUID employeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Employee context not found in token."));
        }

        Claim claim = claimRepository.findById(claimId).orElse(null);
        if (claim == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDto<>(404, "Claim not found"));
        }
        if (!Objects.equals(claim.getEmployee().getIndividualId(), employeeId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>(403, "You do not have access to this claim"));
        }

        ClaimDetailsResponse details = claimsService.getClaimDetails(claimId);
        return ResponseEntity.ok(new ResponseDto<>("Success", details));
    }

    @Override
    @AuditedOperation(schemaName = "claims", tableName = "claims", entityType = "CLAIM", action = "UPDATE")
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> updateDraftClaim(UUID claimId, ClaimSubmissionRequest request) {
        UUID employeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Employee context not found in token."));
        }

        try {
            ClaimDetailsResponse updated = claimsService.updateDraftClaim(claimId, request, employeeId);
            return ResponseEntity.ok(new ResponseDto<>("Claim updated successfully", updated));
        } catch (BadRequestException e) {
            if (e.getMessage() != null && e.getMessage().contains("BR-CS-008")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseDto<>(403, e.getMessage()));
            }
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, e.getMessage()));
        }
    }

    @Override
    @AuditedOperation(schemaName = "claims", tableName = "claims", entityType = "CLAIM", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> cancelClaim(UUID claimId) {
        UUID employeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Employee context not found in token."));
        }

        try {
            claimsService.cancelClaim(claimId, employeeId);
            return ResponseEntity.ok(new ResponseDto<>("Claim cancelled successfully", "OK"));
        } catch (BadRequestException e) {
            if (e.getMessage() != null && e.getMessage().contains("BR-CS-009")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseDto<>(403, e.getMessage()));
            }
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<EmployeeClaimsSummaryResponse>> getMyClaimsSummary() {
        UUID employeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Employee context not found in token."));
        }
        long totalClaims = claimRepository.countByEmployee_IndividualIdAndIsDeleted(employeeId, false);
        BigDecimal totalPaid = claimRepository.sumSettledAmountByEmployeeId(employeeId);
        if (totalPaid == null) {
            totalPaid = BigDecimal.ZERO;
        }
        EmployeeClaimsSummaryResponse summary = EmployeeClaimsSummaryResponse.builder()
                .totalClaims(totalClaims)
                .totalPaid(totalPaid)
                .totalPending(BigDecimal.ZERO)
                .build();
        return ResponseEntity.ok(new ResponseDto<>("Success", summary));
    }

    private void resolveOrganizationFromPolicy(ClaimSubmissionRequest request) {
        policyRepository.findById(request.getPolicyId())
                .filter(p -> p.getOrganizationId() != null)
                .ifPresent(p -> request.setOrganizationId(p.getOrganizationId()));
    }
}
