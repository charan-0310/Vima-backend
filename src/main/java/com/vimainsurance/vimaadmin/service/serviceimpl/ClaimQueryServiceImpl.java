package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.adapter.InsurerAdapter;
import com.vimainsurance.vimaadmin.adapter.InsurerAdapterFactory;
import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimQueryDto;
import com.vimainsurance.vimaadmin.dto.claim.EmployeeResponseRequest;
import com.vimainsurance.vimaadmin.dto.claim.QueryCreateRequest;
import com.vimainsurance.vimaadmin.dto.claim.QueryCreateResponse;
import com.vimainsurance.vimaadmin.dto.claim.QueryResponseRequest;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.ClaimQuery;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.QueryStatus;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.repository.IClaimQueryRepository;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.service.IClaimQueryService;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.service.claim.ClaimAuditService;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimQueryServiceImpl implements IClaimQueryService {

    private final IClaimRepository claimRepository;
    private final IClaimQueryRepository claimQueryRepository;
    private final IAdminUserRepository adminUserRepository;
    private final ClaimAuditService auditService;
    private final InsurerAdapterFactory adapterFactory;
    private final IEmailService emailService;
    private final JwtUserExtractor jwtUserExtractor;

    /**
     * Create a query for the claim. Query creation is allowed for all claim statuses;
     * no status-based validation is performed (any step, any status).
     * Does not change claim status or workflow step; the claim remains in its current stage.
     */
    @Override
    @Transactional
    public ResponseEntity<ResponseDto<QueryCreateResponse>> createQuery(UUID claimId, QueryCreateRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        if (claim.getInternalStatus() == ClaimStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>(400, "Claim is closed and cannot be modified"));
        }
        log.debug("createQuery claimId={} currentStatus={}", claimId, claim.getInternalStatus());
        ClaimQuery query = new ClaimQuery();
        query.setClaim(claim);
        query.setQueryText(request.getQueryText());
        if (request.getQueryDate() != null) {
            query.setQueryDate(request.getQueryDate());
        } else if (request.getQueryDateTimestamp() != null) {
            query.setQueryDate(request.getQueryDateTimestamp().toLocalDate());
        } else {
            query.setQueryDate(LocalDate.now());
        }
        query.setInsurerSysId(request.getInsurerSysId());
        query.setQueryStatus(QueryStatus.OPEN);
        query = claimQueryRepository.save(query);

        // Do not change claim status when creating a query; claim remains in current workflow step.
        ClaimStatus currentStatus = claim.getInternalStatus();
        String detail = request.getQueryText() != null && request.getQueryText().length() > 100
                ? request.getQueryText().substring(0, 100) + "..." : (request.getQueryText() != null ? request.getQueryText() : "");
        auditService.logAction(claimId, "QUERY_ADDED", currentStatus.getValue(), currentStatus.getValue(),
                jwtUserExtractor.getCurrentUserId(), jwtUserExtractor.getCurrentUserRole() != null ? jwtUserExtractor.getCurrentUserRole().getValue() : "ADMIN",
                "Query: " + detail);
        sendQueryRaisedEmail(claim);

        QueryCreateResponse response = QueryCreateResponse.builder()
                .id(query.getId())
                .queryStatus(QueryStatus.OPEN)
                .claimStatus(currentStatus)
                .build();
        return ResponseEntity.ok(new ResponseDto<>("Success", response));
    }

    /**
     * Record admin response to a query. Does not change claim status or workflow step (same as create query).
     */
    @Override
    @Transactional
    public ResponseEntity<ResponseDto<Void>> respondToQuery(UUID claimId, UUID queryId, QueryResponseRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        if (claim.getInternalStatus() == ClaimStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>(400, "Claim is closed and cannot be modified"));
        }
        ClaimQuery query = claimQueryRepository.findById(queryId)
                .orElseThrow(() -> new BadRequestException("Query not found"));
        if (!query.getClaim().getId().equals(claimId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>("Query does not belong to this claim", null));
        }

        query.setResponseText(request.getResponseText());
        query.setResponseDate(request.getResponseDate() != null ? request.getResponseDate() : LocalDate.now());
        query.setCourierName(request.getCourierName());
        query.setPodNumber(request.getPodNumber());
        query.setNumDocumentsAttached(request.getNumDocumentsAttached());
        UUID adminId = jwtUserExtractor.getCurrentUserId();
        if (adminId != null) {
            query.setRespondedBy(adminUserRepository.findById(adminId).orElse(null));
        }
        query.setQueryStatus(QueryStatus.RESPONDED);
        claimQueryRepository.save(query);

        InsurerAdapter adapter = adapterFactory.getAdapter(claim);
        adapter.respondToQuery(claim, query);

        // Do not change claim status when responding to a query; claim remains in current workflow step (same as create query).
        ClaimStatus currentStatus = claim.getInternalStatus();
        auditService.logAction(claimId, "QUERY_RESPONDED", currentStatus.getValue(), currentStatus.getValue(),
                adminId, jwtUserExtractor.getCurrentUserRole() != null ? jwtUserExtractor.getCurrentUserRole().getValue() : "ADMIN",
                "Query response recorded");

        return ResponseEntity.ok(new ResponseDto<>("Success", null));
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<Void>> addEmployeeResponse(UUID claimId, UUID queryId, EmployeeResponseRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        UUID currentEmployeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (currentEmployeeId == null || claim.getEmployee() == null || !claim.getEmployee().getIndividualId().equals(currentEmployeeId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>("Access denied: not your claim", null));
        }
        ClaimQuery query = claimQueryRepository.findById(queryId)
                .orElseThrow(() -> new BadRequestException("Query not found"));
        if (!query.getClaim().getId().equals(claimId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>("Query does not belong to this claim", null));
        }

        query.setEmployeeRemarks(request.getRemarks());
        query.setEmployeeResponseAt(LocalDateTime.now());
        claimQueryRepository.save(query);
        return ResponseEntity.ok(new ResponseDto<>("Success", null));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<List<ClaimQueryDto>>> listQueries(UUID claimId) {
        List<ClaimQuery> queries = claimQueryRepository.findByClaim_IdOrderByCreatedAtAsc(claimId);
        List<ClaimQueryDto> dtos = queries.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(new ResponseDto<>("Success", dtos));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<List<ClaimQueryDto>>> listQueriesForEmployee(UUID claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        UUID currentEmployeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (currentEmployeeId == null || claim.getEmployee() == null || !claim.getEmployee().getIndividualId().equals(currentEmployeeId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>("Access denied: not your claim", null));
        }
        return listQueries(claimId);
    }

    private void sendQueryRaisedEmail(Claim claim) {
        try {
            if (claim.getEmployee() == null || claim.getEmployee().getEmail() == null || claim.getEmployee().getEmail().isBlank()) {
                return;
            }
            String subject = "Claim query raised – " + (claim.getClaimNumber() != null ? claim.getClaimNumber() : claim.getId());
            String body = "A query has been raised on your claim.\n\nClaim number: " + (claim.getClaimNumber() != null ? claim.getClaimNumber() : claim.getId())
                    + "\n\nPlease log in to provide your response (documents/remarks).\n\nThank you.";
            EmailRequest req = EmailRequest.builder()
                    .to(claim.getEmployee().getEmail())
                    .subject(subject)
                    .body(body)
                    .isHtml(false)
                    .build();
            emailService.sendSimpleEmail(req);
        } catch (Exception e) {
            log.warn("Failed to send query-raised email for claim {}: {}", claim.getId(), e.getMessage());
        }
    }

    private ClaimQueryDto toDto(ClaimQuery q) {
        return ClaimQueryDto.builder()
                .id(q.getId())
                .insurerSysId(q.getInsurerSysId())
                .queryText(q.getQueryText())
                .queryDate(q.getQueryDate())
                .queryStatus(q.getQueryStatus())
                .responseText(q.getResponseText())
                .responseDate(q.getResponseDate())
                .responseRemark(q.getResponseRemark())
                .courierName(q.getCourierName())
                .podNumber(q.getPodNumber())
                .numDocumentsAttached(q.getNumDocumentsAttached())
                .employeeRemarks(q.getEmployeeRemarks())
                .employeeResponseAt(q.getEmployeeResponseAt())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }
}
