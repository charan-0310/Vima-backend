package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.dto.claim.ClaimsSummaryResponse;
import com.vimainsurance.vimaadmin.dto.claim.InsurerRefRequest;
import com.vimainsurance.vimaadmin.dto.claim.StatusUpdateRequest;
import com.vimainsurance.vimaadmin.dto.claim.StatusUpdateResponse;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.mapper.ClaimMapper;
import com.vimainsurance.vimaadmin.repository.IClaimAuditLogRepository;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.service.IAdminClaimsService;
import com.vimainsurance.vimaadmin.service.IClaimsService;
import com.vimainsurance.vimaadmin.specification.ClaimSpecification;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminClaimsServiceImpl implements IAdminClaimsService {

    private final IClaimsService claimsService;
    private final IClaimRepository claimRepository;
    private final IClaimAuditLogRepository auditLogRepository;
    private final JwtUserExtractor jwtUserExtractor;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<Page<ClaimDetailsResponse>>> listClaims(ClaimListFilters filters, Pageable pageable) {
        if (filters != null && filters.getIsDeleted() == null) {
            filters.setIsDeleted(false);
        }
        Page<ClaimDetailsResponse> page = claimsService.listClaims(filters, pageable);
        return ResponseEntity.ok(new ResponseDto<>("Success", page, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> getClaimDetail(UUID claimId) {
        ClaimDetailsResponse detail = claimsService.getClaimDetails(claimId);
        return ResponseEntity.ok(new ResponseDto<>("Success", detail));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<StatusUpdateResponse>> changeStatus(UUID claimId, StatusUpdateRequest request) {
        UUID actorId = jwtUserExtractor.getCurrentUserId();
        String actorRole = jwtUserExtractor.getCurrentUserRole() != null ? jwtUserExtractor.getCurrentUserRole().getValue() : "ADMIN";
        if (actorId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Admin context not found in token."));
        }
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new BadRequestException("Claim not found"));
        ClaimStatus oldStatus = claim.getInternalStatus();
        try {
            claimsService.updateStatus(claimId, request, actorId, actorRole);
            claim = claimRepository.findById(claimId).orElse(claim);
            StatusUpdateResponse response = StatusUpdateResponse.builder()
                    .claimId(claimId)
                    .oldStatus(oldStatus)
                    .newStatus(claim.getInternalStatus())
                    .updatedAt(claim.getUpdatedAt())
                    .build();
            return ResponseEntity.ok(new ResponseDto<>("Status updated successfully", response));
        } catch (BadRequestException e) {
            log.warn("[admin-claims] changeStatus failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, e.getMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> updateInsurerRef(UUID claimId, InsurerRefRequest request) {
        ClaimDetailsResponse updated = claimsService.updateInsurerRef(claimId, request);
        return ResponseEntity.ok(new ResponseDto<>("Insurer reference updated", updated));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> submitToInsurer(UUID claimId) {
        try {
            ClaimDetailsResponse result = claimsService.submitToInsurer(claimId);
            return ResponseEntity.ok(new ResponseDto<>("Submit to insurer completed", result));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, e.getMessage()));
        } catch (Exception e) {
            log.error("Submit to insurer failed for claimId={}", claimId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto<>(500, "Submit to insurer failed. Please try again or contact support."));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<List<ClaimDetailsResponse.ClaimAuditLogDto>>> getAuditLog(UUID claimId) {
        if (claimRepository.findById(claimId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDto<>(404, "Claim not found"));
        }
        List<ClaimDetailsResponse.ClaimAuditLogDto> logs = ClaimMapper.toAuditLogDtos(
                auditLogRepository.findByClaim_IdOrderByCreatedAtDesc(claimId));
        return ResponseEntity.ok(new ResponseDto<>("Success", logs));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<ClaimsSummaryResponse>> getSummaryMetrics(ClaimListFilters filters) {
        if (filters != null && filters.getIsDeleted() == null) {
            filters.setIsDeleted(false);
        }
        Specification<Claim> baseSpec = ClaimSpecification.withFiltersAndStatus(filters, null);
        long totalClaims = claimRepository.count(baseSpec);
        long pendingReview = claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.PENDING_REVIEW));
        long withInsurer = claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.SUBMITTED_TO_INSURER))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.IN_PROGRESS))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.QUERY_RAISED))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.QUERY_RESPONDED))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.APPROVED))
                + claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.PAYMENT_PENDING));
        long queryRaised = claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.QUERY_RAISED));
        long settled = claimRepository.count(ClaimSpecification.withFiltersAndStatus(filters, ClaimStatus.SETTLED));
        // Rejected bucket: all claims with status REJECTED, REJECTED_BY_ADMIN, or INTIMATION_REJECTED (single query, enum-safe)
        List<ClaimStatus> rejectedStatuses = Arrays.asList(
                ClaimStatus.REJECTED,
                ClaimStatus.REJECTED_BY_ADMIN,
                ClaimStatus.INTIMATION_REJECTED);
        long rejected = claimRepository.count(ClaimSpecification.withFiltersAndStatusIn(filters, rejectedStatuses));

        List<Claim> claims = claimRepository.findAll(baseSpec, Pageable.unpaged()).getContent();
        double avgProcessingDays = 0;
        int countWithDates = 0;
        long sumDays = 0;
        for (Claim c : claims) {
            if (c.getDateOfSubmission() != null && c.getUpdatedAt() != null) {
                sumDays += ChronoUnit.DAYS.between(c.getDateOfSubmission().atStartOfDay(), c.getUpdatedAt());
                countWithDates++;
            }
        }
        if (countWithDates > 0) {
            avgProcessingDays = (double) sumDays / countWithDates;
        }

        ClaimsSummaryResponse summary = ClaimsSummaryResponse.builder()
                .totalClaims(totalClaims)
                .pendingReview(pendingReview)
                .withInsurer(withInsurer)
                .queryRaised(queryRaised)
                .settled(settled)
                .rejected(rejected)
                .avgProcessingDays(avgProcessingDays)
                .build();
        return ResponseEntity.ok(new ResponseDto<>("Success", summary));
    }
}
