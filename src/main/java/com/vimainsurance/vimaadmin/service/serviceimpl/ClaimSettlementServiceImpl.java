package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDeductionDto;
import com.vimainsurance.vimaadmin.dto.claim.DeductionRequest;
import com.vimainsurance.vimaadmin.dto.claim.SettlementRequest;
import com.vimainsurance.vimaadmin.dto.claim.SettlementResponse;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.ClaimDeduction;
import com.vimainsurance.vimaadmin.entity.ClaimSettlement;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.repository.IClaimDeductionRepository;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.repository.IClaimSettlementRepository;
import com.vimainsurance.vimaadmin.service.IClaimSettlementService;
import com.vimainsurance.vimaadmin.service.IClaimsDocumentService;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.service.claim.ClaimAuditService;
import com.vimainsurance.vimaadmin.service.claim.ClaimStatusTransitionValidator;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimSettlementServiceImpl implements IClaimSettlementService {

    private static final java.util.Set<ClaimStatus> ALLOWED_STATUSES_FOR_SETTLEMENT =
            java.util.EnumSet.of(ClaimStatus.APPROVED, ClaimStatus.PAYMENT_PENDING);

    private final IClaimRepository claimRepository;
    private final IClaimSettlementRepository settlementRepository;
    private final IClaimDeductionRepository deductionRepository;
    private final ClaimStatusTransitionValidator statusValidator;
    private final ClaimAuditService auditService;
    private final IEmailService emailService;
    private final IClaimsDocumentService claimsDocumentService;
    private final JwtUserExtractor jwtUserExtractor;

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<SettlementResponse>> recordSettlement(UUID claimId, SettlementRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        if (!ALLOWED_STATUSES_FOR_SETTLEMENT.contains(claim.getInternalStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>("Claim must be APPROVED or PAYMENT_PENDING to record settlement", null));
        }
        BigDecimal amountPaid = request.getAmountPaid() != null ? request.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal claimAmount = claim.getClaimAmount() != null ? claim.getClaimAmount() : BigDecimal.ZERO;
        if (amountPaid.compareTo(claimAmount) > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>("amount_paid must not exceed claimed amount (BR-ST-002)", null));
        }

        ClaimSettlement settlement = new ClaimSettlement();
        settlement.setClaim(claim);
        settlement.setClaimedAmount(claim.getClaimAmount());
        settlement.setGrossSanctionedAmount(request.getGrossSanctionedAmount());
        settlement.setNetSanctionedAmount(request.getNetSanctionedAmount());
        settlement.setTotalDisallowedAmount(request.getTotalDisallowedAmount());
        settlement.setDeductionAmount(request.getDeductionAmount());
        settlement.setCopayAmount(request.getCopayAmount());
        settlement.setAmountPaid(amountPaid);
        settlement.setPaymentMode(request.getPaymentMode());
        settlement.setChequeNumber(request.getChequeNumber());
        settlement.setChequeDate(request.getChequeDate());
        settlement.setPaymentDate(request.getPaymentDate());
        settlement.setPaymentReference(request.getPaymentReference());
        settlement.setSettlementDate(LocalDate.now());
        settlement = settlementRepository.save(settlement);

        if (request.getInsurerRemarks() != null) {
            claim.setInsurerRemarks(request.getInsurerRemarks());
        }
        ClaimStatus oldStatus = claim.getInternalStatus();
        statusValidator.validateTransition(oldStatus, ClaimStatus.SETTLED);
        claim.setInternalStatus(ClaimStatus.SETTLED);
        claimRepository.save(claim);

        auditService.logAction(claimId, "SETTLEMENT_RECORDED", oldStatus.getValue(), ClaimStatus.SETTLED.getValue(),
                jwtUserExtractor.getCurrentUserId(), jwtUserExtractor.getCurrentUserRole() != null ? jwtUserExtractor.getCurrentUserRole().getValue() : "ADMIN",
                "Settlement amountPaid=" + amountPaid);

        sendSettlementEmail(claim, amountPaid);

        SettlementResponse response = SettlementResponse.builder()
                .id(settlement.getId())
                .claimStatus(ClaimStatus.SETTLED)
                .amountPaid(amountPaid)
                .build();
        return ResponseEntity.ok(new ResponseDto<>("Success", response));
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<SettlementResponse>> recordSettlementWithDocument(UUID claimId, SettlementRequest request, MultipartFile document) {
        ResponseEntity<ResponseDto<SettlementResponse>> result = recordSettlement(claimId, request);
        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null && result.getBody().getPayload() != null
                && document != null && !document.isEmpty()) {
            UUID adminId = jwtUserExtractor.getCurrentUserId();
            String role = jwtUserExtractor.getCurrentUserRole() != null ? jwtUserExtractor.getCurrentUserRole().getValue() : "ADMIN";
            claimsDocumentService.uploadClaimsDocuments(new MultipartFile[] { document }, claimId, DocumentType.SETTLEMENT_DOCUMENT, adminId, role);
        }
        return result;
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<SettlementResponse>> updateSettlement(UUID claimId, SettlementRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        if (claim.getInternalStatus() == ClaimStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>("Cannot update settlement for closed claim", null));
        }
        ClaimSettlement settlement = settlementRepository.findByClaim_Id(claimId)
                .orElseThrow(() -> new BadRequestException("Settlement not found for this claim"));
        if (request.getGrossSanctionedAmount() != null) settlement.setGrossSanctionedAmount(request.getGrossSanctionedAmount());
        if (request.getNetSanctionedAmount() != null) settlement.setNetSanctionedAmount(request.getNetSanctionedAmount());
        if (request.getTotalDisallowedAmount() != null) settlement.setTotalDisallowedAmount(request.getTotalDisallowedAmount());
        if (request.getDeductionAmount() != null) settlement.setDeductionAmount(request.getDeductionAmount());
        if (request.getCopayAmount() != null) settlement.setCopayAmount(request.getCopayAmount());
        if (request.getAmountPaid() != null) settlement.setAmountPaid(request.getAmountPaid());
        if (request.getPaymentMode() != null) settlement.setPaymentMode(request.getPaymentMode());
        if (request.getChequeNumber() != null) settlement.setChequeNumber(request.getChequeNumber());
        if (request.getChequeDate() != null) settlement.setChequeDate(request.getChequeDate());
        if (request.getPaymentDate() != null) settlement.setPaymentDate(request.getPaymentDate());
        if (request.getPaymentReference() != null) settlement.setPaymentReference(request.getPaymentReference());
        if (request.getInsurerRemarks() != null) claim.setInsurerRemarks(request.getInsurerRemarks());
        settlementRepository.save(settlement);
        claimRepository.save(claim);

        auditService.logAction(claimId, "SETTLEMENT_UPDATED", claim.getInternalStatus().getValue(), claim.getInternalStatus().getValue(),
                jwtUserExtractor.getCurrentUserId(), jwtUserExtractor.getCurrentUserRole() != null ? jwtUserExtractor.getCurrentUserRole().getValue() : "ADMIN",
                null);

        SettlementResponse response = SettlementResponse.builder()
                .id(settlement.getId())
                .claimStatus(claim.getInternalStatus())
                .amountPaid(settlement.getAmountPaid())
                .build();
        return ResponseEntity.ok(new ResponseDto<>("Success", response));
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<SettlementResponse>> updateSettlementWithDocument(UUID claimId, SettlementRequest request, MultipartFile document) {
        ResponseEntity<ResponseDto<SettlementResponse>> result = updateSettlement(claimId, request);
        if (result.getStatusCode().is2xxSuccessful() && document != null && !document.isEmpty()) {
            UUID adminId = jwtUserExtractor.getCurrentUserId();
            String role = jwtUserExtractor.getCurrentUserRole() != null ? jwtUserExtractor.getCurrentUserRole().getValue() : "ADMIN";
            claimsDocumentService.uploadClaimsDocuments(new MultipartFile[] { document }, claimId, DocumentType.SETTLEMENT_DOCUMENT, adminId, role);
        }
        return result;
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<ClaimDeductionDto>> addDeduction(UUID claimId, DeductionRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        ClaimDeduction deduction = new ClaimDeduction();
        deduction.setClaim(claim);
        deduction.setDeductionDetails(request.getDeductionDetails());
        deduction.setDeductionAmount(request.getDeductionAmount());
        deduction = deductionRepository.save(deduction);

        auditService.logAction(claimId, "DEDUCTION_ADDED", claim.getInternalStatus().getValue(), claim.getInternalStatus().getValue(),
                jwtUserExtractor.getCurrentUserId(), jwtUserExtractor.getCurrentUserRole() != null ? jwtUserExtractor.getCurrentUserRole().getValue() : "ADMIN",
                "Deduction: " + request.getDeductionDetails());

        ClaimDeductionDto dto = toDeductionDto(deduction);
        return ResponseEntity.ok(new ResponseDto<>("Success", dto));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<List<ClaimDeductionDto>>> listDeductions(UUID claimId) {
        List<ClaimDeduction> list = deductionRepository.findByClaim_Id(claimId);
        List<ClaimDeductionDto> dtos = list.stream().map(this::toDeductionDto).collect(Collectors.toList());
        return ResponseEntity.ok(new ResponseDto<>("Success", dtos));
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<Void>> removeDeduction(UUID claimId, UUID deductionId) {
        ClaimDeduction deduction = deductionRepository.findById(deductionId)
                .orElseThrow(() -> new BadRequestException("Deduction not found"));
        if (!deduction.getClaim().getId().equals(claimId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>("Deduction does not belong to this claim", null));
        }
        deductionRepository.delete(deduction);
        Claim claim = claimRepository.findById(claimId).orElse(null);
        if (claim != null) {
            auditService.logAction(claimId, "DEDUCTION_REMOVED", claim.getInternalStatus().getValue(), claim.getInternalStatus().getValue(),
                    jwtUserExtractor.getCurrentUserId(), jwtUserExtractor.getCurrentUserRole() != null ? jwtUserExtractor.getCurrentUserRole().getValue() : "ADMIN",
                    null);
        }
        return ResponseEntity.ok(new ResponseDto<>("Success", null));
    }

    private void sendSettlementEmail(Claim claim, BigDecimal amountPaid) {
        try {
            if (claim.getEmployee() == null || claim.getEmployee().getEmail() == null || claim.getEmployee().getEmail().isBlank()) {
                return;
            }
            String subject = "Claim settled – " + (claim.getClaimNumber() != null ? claim.getClaimNumber() : claim.getId());
            String body = "Your claim has been settled.\n\nClaim number: " + (claim.getClaimNumber() != null ? claim.getClaimNumber() : claim.getId())
                    + "\nAmount paid: " + (amountPaid != null ? amountPaid : "N/A")
                    + "\n\nThank you.";
            EmailRequest req = EmailRequest.builder()
                    .to(claim.getEmployee().getEmail())
                    .subject(subject)
                    .body(body)
                    .isHtml(false)
                    .build();
            emailService.sendSimpleEmail(req);
        } catch (Exception e) {
            log.warn("Failed to send settlement email for claim {}: {}", claim.getId(), e.getMessage());
        }
    }

    private ClaimDeductionDto toDeductionDto(ClaimDeduction d) {
        return ClaimDeductionDto.builder()
                .id(d.getId())
                .deductionDetails(d.getDeductionDetails())
                .deductionAmount(d.getDeductionAmount())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
