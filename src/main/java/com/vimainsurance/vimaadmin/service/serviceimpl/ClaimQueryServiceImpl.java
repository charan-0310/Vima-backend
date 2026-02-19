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
import com.vimainsurance.vimaadmin.service.claim.ClaimStatusTransitionValidator;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimQueryServiceImpl implements IClaimQueryService {

    private static final java.util.Set<ClaimStatus> ALLOWED_STATUSES_FOR_CREATE_QUERY =
            java.util.EnumSet.of(ClaimStatus.SUBMITTED_TO_INSURER, ClaimStatus.IN_PROGRESS);

    private final IClaimRepository claimRepository;
    private final IClaimQueryRepository claimQueryRepository;
    private final IAdminUserRepository adminUserRepository;
    private final ClaimStatusTransitionValidator statusValidator;
    private final ClaimAuditService auditService;
    private final InsurerAdapterFactory adapterFactory;
    private final IEmailService emailService;
    private final JwtUserExtractor jwtUserExtractor;

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<QueryCreateResponse>> createQuery(UUID claimId, QueryCreateRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        if (!ALLOWED_STATUSES_FOR_CREATE_QUERY.contains(claim.getInternalStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>("Claim status does not allow creating a query", null));
        }
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

        ClaimStatus oldStatus = claim.getInternalStatus();
        claim.setInternalStatus(ClaimStatus.QUERY_RAISED);
        claimRepository.save(claim);

        String detail = request.getQueryText() != null && request.getQueryText().length() > 100
                ? request.getQueryText().substring(0, 100) + "..." : (request.getQueryText() != null ? request.getQueryText() : "");
        auditService.logAction(claimId, "QUERY_CREATED", oldStatus.getValue(), ClaimStatus.QUERY_RAISED.getValue(),
                jwtUserExtractor.getCurrentUserId(), jwtUserExtractor.getCurrentUserRole() != null ? jwtUserExtractor.getCurrentUserRole().getValue() : "ADMIN",
                "Query: " + detail);

        sendQueryRaisedEmail(claim);

        QueryCreateResponse response = QueryCreateResponse.builder()
                .id(query.getId())
                .queryStatus(QueryStatus.OPEN)
                .claimStatus(ClaimStatus.QUERY_RAISED)
                .build();
        return ResponseEntity.ok(new ResponseDto<>("Success", response));
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<Void>> respondToQuery(UUID claimId, UUID queryId, QueryResponseRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
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

        ClaimStatus oldStatus = claim.getInternalStatus();
        statusValidator.validateTransition(oldStatus, ClaimStatus.QUERY_RESPONDED);
        claim.setInternalStatus(ClaimStatus.QUERY_RESPONDED);
        claimRepository.save(claim);

        auditService.logAction(claimId, "QUERY_RESPONDED", oldStatus.getValue(), ClaimStatus.QUERY_RESPONDED.getValue(),
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
