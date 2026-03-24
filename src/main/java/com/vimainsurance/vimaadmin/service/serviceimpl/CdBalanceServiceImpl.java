package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.CdBalanceResponseDto;
import com.vimainsurance.vimaadmin.dto.CdBalanceTransactionRequestDto;
import com.vimainsurance.vimaadmin.dto.CdBalanceTransactionResponseDto;
import com.vimainsurance.vimaadmin.dto.EndorsementCdBalanceEntryDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.CdBalanceTransaction;
import com.vimainsurance.vimaadmin.entity.CdTransactionDocument;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.CdTransactionSource;
import com.vimainsurance.vimaadmin.enums.CdTransactionType;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.EndorsementType;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.mapper.CdBalanceMapper;
import com.vimainsurance.vimaadmin.repository.ICdBalanceTransactionRepository;
import com.vimainsurance.vimaadmin.specification.CdBalanceTransactionSpecification;
import com.vimainsurance.vimaadmin.repository.ICdTransactionDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.ICdBalanceService;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

@Service
public class CdBalanceServiceImpl implements ICdBalanceService {

    private static final Logger logger = LoggerFactory.getLogger(CdBalanceServiceImpl.class);

    @Autowired
    private IPolicyRepository policyRepository;

    @Autowired
    private IEndorsementRepository endorsementRepository;

    @Autowired
    private ICdBalanceTransactionRepository cdBalanceTransactionRepository;

    @Autowired
    private ICdTransactionDocumentRepository cdTransactionDocumentRepository;

    @Autowired
    private IDocumentService documentService;

    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<CdBalanceResponseDto>> getCdBalance(Long policyId) {
        BaseResponse<CdBalanceResponseDto> responseObj = new BaseResponse<>();
        try {
            Policy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));
            CdBalanceResponseDto responseDto = CdBalanceMapper.toBalanceResponseDto(policy);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching CD balance for policyId={}", MDC.get("correlationId"), policyId, e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "cd_balance_transactions", entityType = "CD_BALANCE", action = "CREATE")
    public ResponseEntity<ResponseDto<CdBalanceTransactionResponseDto>> recordTransaction(
            CdBalanceTransactionRequestDto requestDto,
            MultipartFile[] files) {
        BaseResponse<CdBalanceTransactionResponseDto> responseObj = new BaseResponse<>();
        try {
            Policy policy = policyRepository.findByIdForUpdate(requestDto.getPolicyId())
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            validatePolicyOrganization(policy, requestDto.getOrganizationId());
            validateDocumentCount(files, requestDto.getDocuments());

            Endorsement endorsement = null;
            if (requestDto.getEndorsementId() != null) {
                endorsement = endorsementRepository.findById(requestDto.getEndorsementId())
                        .orElseThrow(() -> new RuntimeException("Endorsement not found"));
            }

            BigDecimal currentBalance = policy.getCdBalance() == null ? BigDecimal.ZERO : policy.getCdBalance();
            BigDecimal runningBalance = currentBalance.add(requestDto.getAmount());

            CdBalanceTransaction transaction = new CdBalanceTransaction();
            transaction.setPolicy(policy);
            transaction.setOrganizationId(requestDto.getOrganizationId());
            transaction.setEndorsement(endorsement);
            transaction.setTransactionType(CdTransactionType.fromValue(requestDto.getTransactionType()));
            transaction.setAmount(requestDto.getAmount());
            transaction.setRunningBalance(runningBalance);
            transaction.setDescription(requestDto.getDescription());
            transaction.setNotes(requestDto.getNotes());
            transaction.setReferenceNumber(requestDto.getReferenceNumber());
            transaction.setSource(CdTransactionSource.fromValue(requestDto.getSource()));
            transaction.setPerformedBy(resolvePerformedBy(requestDto.getPerformedBy()));
            CdBalanceTransaction saved = cdBalanceTransactionRepository.save(transaction);

            List<UUID> uploadedDocumentIds = uploadAndLinkProofDocuments(saved, policy, files, requestDto.getDocuments());

            policy.setCdBalance(runningBalance);
            policyRepository.save(policy);

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, CdBalanceMapper.toTransactionResponseDto(saved, uploadedDocumentIds)));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error recording CD transaction for policyId={}", MDC.get("correlationId"), requestDto.getPolicyId(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "cd_balance_transactions", entityType = "CD_BALANCE", action = "ENDORSEMENT_UPDATE")
    public void recordEndorsementCdBalanceEntries(
            UUID endorsementId,
            UUID organizationId,
            List<EndorsementCdBalanceEntryDto> entries,
            EndorsementType endorsementType,
            String performedBy) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        Endorsement endorsement = endorsementRepository.findById(endorsementId)
                .orElseThrow(() -> new RuntimeException("Endorsement not found"));

        for (EndorsementCdBalanceEntryDto entry : entries) {
            Policy policy = policyRepository.findByIdForUpdate(entry.getPolicyId())
                    .orElseThrow(() -> new RuntimeException("Policy not found"));
            validatePolicyOrganization(policy, organizationId);
            validateDocumentCount(entry.getDocuments(), null);

            BigDecimal signedAmount = endorsementType == EndorsementType.ADDITION
                    ? entry.getAmount().abs().negate()
                    : entry.getAmount().abs();
            CdTransactionType transactionType = endorsementType == EndorsementType.ADDITION
                    ? CdTransactionType.ENDORSEMENT_DEBIT
                    : CdTransactionType.ENDORSEMENT_CREDIT;

            BigDecimal currentBalance = policy.getCdBalance() == null ? BigDecimal.ZERO : policy.getCdBalance();
            BigDecimal runningBalance = currentBalance.add(signedAmount);

            CdBalanceTransaction transaction = new CdBalanceTransaction();
            transaction.setPolicy(policy);
            transaction.setOrganizationId(organizationId);
            transaction.setEndorsement(endorsement);
            transaction.setTransactionType(transactionType);
            transaction.setAmount(signedAmount);
            transaction.setRunningBalance(runningBalance);
            transaction.setDescription(entry.getDescription());
            transaction.setNotes(entry.getNotes());
            transaction.setReferenceNumber(entry.getReferenceNumber());
            transaction.setSource(CdTransactionSource.ENDORSEMENT_APPROVAL);
            transaction.setPerformedBy(resolvePerformedBy(performedBy));
            CdBalanceTransaction saved = cdBalanceTransactionRepository.save(transaction);

            uploadAndLinkProofDocuments(saved, policy, entry.getDocuments(), null);

            policy.setCdBalance(runningBalance);
            policyRepository.save(policy);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<List<CdBalanceTransactionResponseDto>>> getTransactionLedger(
            Long policyId,
            int page,
            int size,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo) {
        BaseResponse<List<CdBalanceTransactionResponseDto>> responseObj = new BaseResponse<>();
        try {
            CdTransactionType txType = (type == null || type.isBlank()) ? null : CdTransactionType.fromValue(type);
            LocalDateTime from = dateFrom == null ? null : dateFrom.atStartOfDay();
            LocalDateTime to = dateTo == null ? null : dateTo.atTime(LocalTime.MAX);

            Page<CdBalanceTransaction> transactionPage = cdBalanceTransactionRepository.findAll(
                    CdBalanceTransactionSpecification.ledgerByPolicy(policyId, txType, from, to),
                    PageRequest.of(
                            Math.max(page, 0),
                            Math.max(size, 1),
                            Sort.by(Sort.Direction.DESC, "createdAt")));

            List<UUID> transactionIds = transactionPage.getContent().stream()
                    .map(CdBalanceTransaction::getTransactionId)
                    .toList();
            Map<UUID, List<UUID>> docsByTransaction = batchLoadDocumentIds(transactionIds);

            List<CdBalanceTransactionResponseDto> payload = transactionPage.getContent().stream()
                    .map(tx -> CdBalanceMapper.toTransactionResponseDto(tx, docsByTransaction.getOrDefault(tx.getTransactionId(), Collections.emptyList())))
                    .collect(Collectors.toList());

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, payload, transactionPage.getTotalElements()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching CD ledger for policyId={}", MDC.get("correlationId"), policyId, e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "cd_balance_transactions", entityType = "CD_BALANCE", action = "RECALCULATE")
    public ResponseEntity<ResponseDto<CdBalanceResponseDto>> recalculateBalance(Long policyId) {
        BaseResponse<CdBalanceResponseDto> responseObj = new BaseResponse<>();
        try {
            validateRecalculateAccess();
            Policy policy = policyRepository.findByIdForUpdate(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            BigDecimal recalculated = cdBalanceTransactionRepository.sumAmountByPolicyId(policyId);
            policy.setCdBalance(recalculated);
            policyRepository.save(policy);

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, CdBalanceMapper.toBalanceResponseDto(policy)));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error recalculating CD balance for policyId={}", MDC.get("correlationId"), policyId, e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    private void validatePolicyOrganization(Policy policy, UUID organizationId) {
        if (policy.getOrganizationId() == null || !policy.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Policy does not belong to organization");
        }
    }

    private void validateDocumentCount(MultipartFile[] primary, MultipartFile[] fallback) {
        MultipartFile[] selected = (primary != null && primary.length > 0) ? primary : fallback;
        if (selected != null && selected.length > 3) {
            throw new RuntimeException("Maximum 3 proof documents are allowed");
        }
    }

    private String resolvePerformedBy(String input) {
        if (input != null && !input.isBlank()) {
            return input.trim();
        }
        String username = jwtUserExtractor.extractCurrentUsername();
        return username != null ? username : "SYSTEM";
    }

    private List<UUID> uploadAndLinkProofDocuments(
            CdBalanceTransaction transaction,
            Policy policy,
            MultipartFile[] primaryFiles,
            MultipartFile[] fallbackFiles) {
        MultipartFile[] files = (primaryFiles != null && primaryFiles.length > 0) ? primaryFiles : fallbackFiles;
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        List<UUID> documentIds = new ArrayList<>();
        for (MultipartFile file : files) {
            ResponseEntity<ResponseDto<String>> uploadResponse = documentService.uploadDocument(
                    file,
                    DocumentType.OTHER.name(),
                    DocumentCategory.FINANCIAL_DOCUMENTS.name(),
                    DocumentEntityType.POLICY.name(),
                    String.valueOf(policy.getPolicyId()),
                    "CD balance transaction proof");
            ResponseDto<String> body = uploadResponse.getBody();
            if (body == null || body.getPayload() == null || body.getPayload().isBlank()) {
                throw new RuntimeException("Failed to upload CD proof document");
            }
            UUID documentId = UUID.fromString(body.getPayload());
            documentIds.add(documentId);

            CdTransactionDocument cdTransactionDocument = new CdTransactionDocument();
            cdTransactionDocument.setTransactionId(transaction.getTransactionId());
            cdTransactionDocument.setDocumentId(documentId);
            cdTransactionDocumentRepository.save(cdTransactionDocument);
        }
        return documentIds;
    }

    private Map<UUID, List<UUID>> batchLoadDocumentIds(List<UUID> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return cdTransactionDocumentRepository.findByTransactionIdIn(transactionIds).stream()
                .collect(Collectors.groupingBy(
                        CdTransactionDocument::getTransactionId,
                        Collectors.mapping(CdTransactionDocument::getDocumentId, Collectors.toList())));
    }

    private void validateRecalculateAccess() {
        UserRole role = jwtUserExtractor.getCurrentUserRole();
        if (role == null || (role != UserRole.SUPER_ADMIN && role != UserRole.VIMA_ADMIN)) {
            throw new RuntimeException("Only SUPER_ADMIN or VIMA_ADMIN can recalculate CD balance");
        }
    }
}
