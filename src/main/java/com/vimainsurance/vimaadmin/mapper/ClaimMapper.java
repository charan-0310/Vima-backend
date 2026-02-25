package com.vimainsurance.vimaadmin.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimSummaryDto;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.ClaimAuditLog;
import com.vimainsurance.vimaadmin.service.claim.ClaimAuditDetailsJson;
import com.vimainsurance.vimaadmin.entity.ClaimDeduction;
import com.vimainsurance.vimaadmin.entity.ClaimQuery;
import com.vimainsurance.vimaadmin.entity.Document;

public final class ClaimMapper {

    private ClaimMapper() {
    }

    public static ClaimDetailsResponse toDetailsResponse(Claim claim, List<Document> documents) {
        if (claim == null) return null;
        ClaimDetailsResponse dto = new ClaimDetailsResponse();
        dto.setId(claim.getId());
        dto.setClaimNumber(claim.getClaimNumber());
        if (claim.getOrganization() != null) {
            dto.setOrganizationId(claim.getOrganization().getOrganizationId());
            dto.setOrganizationName(claim.getOrganization().getOrganizationName());
        }
        dto.setPolicyId(claim.getPolicyId());
        if (claim.getEmployee() != null) {
            dto.setEmployeeId(claim.getEmployee().getIndividualId());
            dto.setEmployeeName(claim.getEmployee().getFirstName() != null ? claim.getEmployee().getFirstName() + " " + (claim.getEmployee().getLastName() != null ? claim.getEmployee().getLastName() : "") : null);
        }
        dto.setMemberId(claim.getMemberId());
        dto.setMemberType(claim.getMemberType());
        dto.setMemberName(claim.getMemberName());
        dto.setMemberDob(claim.getMemberDob());
        dto.setMemberUhid(claim.getMemberUhid());
        dto.setRelationship(claim.getRelationship());
        dto.setClaimType(claim.getClaimType());
        dto.setClaimCategory(claim.getClaimCategory());
        dto.setProductType(claim.getProductType());
        dto.setReasonForAdmission(claim.getReasonForAdmission());
        dto.setDiagnosis(claim.getDiagnosis());
        dto.setClaimAmount(claim.getClaimAmount());
        dto.setHospitalName(claim.getHospitalName());
        dto.setHospitalCity(claim.getHospitalCity());
        dto.setHospitalState(claim.getHospitalState());
        dto.setHospitalPincode(claim.getHospitalPincode());
        dto.setHospitalProviderCode(claim.getHospitalProviderCode());
        dto.setIsNetworkHospital(claim.getIsNetworkHospital());
        dto.setDateOfAdmission(claim.getDateOfAdmission());
        dto.setDateOfDischarge(claim.getDateOfDischarge());
        dto.setDateOfSubmission(claim.getDateOfSubmission());
        dto.setBankAccountNumber(claim.getBankAccountNumber());
        dto.setAccountHolderName(claim.getAccountHolderName());
        dto.setIfscCode(claim.getIfscCode());
        dto.setBankBranchName(claim.getBankBranchName());
        dto.setInsurerId(claim.getInsurerId());
        dto.setInsurerClaimRef(claim.getInsurerClaimRef());
        dto.setInsurerClaimNumber(claim.getInsurerClaimNumber());
        dto.setInsurerInwardNumber(claim.getInsurerInwardNumber());
        dto.setInsurerStatus(claim.getInsurerStatus());
        dto.setInsurerCurrentStatus(claim.getInsurerCurrentStatus());
        dto.setInsurerRemarks(claim.getInsurerRemarks());
        dto.setRejectionReason(claim.getRejectionReason());
        dto.setInternalStatus(claim.getInternalStatus());
        dto.setDisplayStatus(toDisplayStatus(claim.getInternalStatus()));
        dto.setSubmissionSource(claim.getSubmissionSource());
        dto.setParentClaimId(claim.getParentClaim() != null ? claim.getParentClaim().getId() : null);
        dto.setParentInsurerClaimRef(claim.getParentInsurerClaimRef());
        dto.setAbhaId(claim.getAbhaId());
        dto.setSubmittedById(claim.getSubmittedBy() != null ? claim.getSubmittedBy().getId() : null);
        dto.setReviewedById(claim.getReviewedBy() != null ? claim.getReviewedBy().getId() : null);
        dto.setReviewedAt(claim.getReviewedAt());
        dto.setApprovedById(claim.getApprovedBy() != null ? claim.getApprovedBy().getId() : null);
        dto.setApprovedAt(claim.getApprovedAt());
        dto.setCreatedAt(claim.getCreatedAt());
        dto.setUpdatedAt(claim.getUpdatedAt());
        dto.setIsDeleted(claim.getIsDeleted());
        dto.setVersion(claim.getVersion());

        if (claim.getSettlement() != null) {
            ClaimDetailsResponse.ClaimSettlementDto s = new ClaimDetailsResponse.ClaimSettlementDto();
            s.setId(claim.getSettlement().getId());
            s.setClaimedAmount(claim.getSettlement().getClaimedAmount());
            s.setGrossSanctionedAmount(claim.getSettlement().getGrossSanctionedAmount());
            s.setNetSanctionedAmount(claim.getSettlement().getNetSanctionedAmount());
            s.setTotalDisallowedAmount(claim.getSettlement().getTotalDisallowedAmount());
            s.setDeductionAmount(claim.getSettlement().getDeductionAmount());
            s.setCopayAmount(claim.getSettlement().getCopayAmount());
            s.setAmountPaid(claim.getSettlement().getAmountPaid());
            s.setPaymentMode(claim.getSettlement().getPaymentMode());
            s.setChequeNumber(claim.getSettlement().getChequeNumber());
            s.setChequeDate(claim.getSettlement().getChequeDate());
            s.setPaymentDate(claim.getSettlement().getPaymentDate());
            s.setPaymentReference(claim.getSettlement().getPaymentReference());
            s.setSettlementDate(claim.getSettlement().getSettlementDate());
            dto.setSettlement(s);
        }
        if (claim.getQueries() != null) {
            dto.setQueries(claim.getQueries().stream().map(ClaimMapper::toQueryDto).collect(Collectors.toList()));
        }
        if (claim.getDeductions() != null) {
            dto.setDeductions(claim.getDeductions().stream().map(ClaimMapper::toDeductionDto).collect(Collectors.toList()));
        }
        if (claim.getAuditLogs() != null) {
            dto.setAuditLogs(claim.getAuditLogs().stream().map(ClaimMapper::toAuditLogDto).collect(Collectors.toList()));
        }
        if (documents != null) {
            dto.setDocuments(documents.stream().map(ClaimMapper::toDocumentSummaryDto).collect(Collectors.toList()));
            dto.setDocumentCount(documents.size());
        } else {
            dto.setDocumentCount(0);
        }
        return dto;
    }

    private static String toDisplayStatus(com.vimainsurance.vimaadmin.enums.ClaimStatus status) {
        if (status == null) return null;
        String v = status.getValue();
        if (v == null) return null;
        String withSpaces = v.replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : withSpaces.toCharArray()) {
            if (cap && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else if (Character.isWhitespace(c)) {
                sb.append(c);
                cap = true;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public static ClaimDetailsResponse toDetailsResponse(Claim claim) {
        return toDetailsResponse(claim, null);
    }

    private static ClaimDetailsResponse.ClaimQueryDto toQueryDto(ClaimQuery q) {
        ClaimDetailsResponse.ClaimQueryDto dto = new ClaimDetailsResponse.ClaimQueryDto();
        dto.setId(q.getId());
        dto.setInsurerSysId(q.getInsurerSysId());
        dto.setQueryText(q.getQueryText());
        dto.setQueryDate(q.getQueryDate());
        dto.setQueryStatus(q.getQueryStatus() != null ? q.getQueryStatus().getValue() : null);
        dto.setResponseText(q.getResponseText());
        dto.setResponseDate(q.getResponseDate());
        dto.setResponseRemark(q.getResponseRemark());
        dto.setCourierName(q.getCourierName());
        dto.setPodNumber(q.getPodNumber());
        dto.setNumDocumentsAttached(q.getNumDocumentsAttached());
        dto.setEmployeeRemarks(q.getEmployeeRemarks());
        dto.setEmployeeResponseAt(q.getEmployeeResponseAt());
        return dto;
    }

    private static ClaimDetailsResponse.ClaimDeductionDto toDeductionDto(ClaimDeduction d) {
        ClaimDetailsResponse.ClaimDeductionDto dto = new ClaimDetailsResponse.ClaimDeductionDto();
        dto.setId(d.getId());
        dto.setInsurerSysId(d.getInsurerSysId());
        dto.setDeductionDetails(d.getDeductionDetails());
        dto.setDeductionAmount(d.getDeductionAmount());
        return dto;
    }

    private static ClaimDetailsResponse.ClaimAuditLogDto toAuditLogDto(ClaimAuditLog a) {
        ClaimDetailsResponse.ClaimAuditLogDto dto = new ClaimDetailsResponse.ClaimAuditLogDto();
        dto.setId(a.getId());
        dto.setAction(a.getAction());
        dto.setOldStatus(a.getOldStatus());
        dto.setNewStatus(a.getNewStatus());
        dto.setActorRole(a.getActorRole());
        dto.setDetails(ClaimAuditDetailsJson.fromJsonMessage(a.getDetails()));
        dto.setCorrelationId(a.getCorrelationId());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

    private static ClaimDetailsResponse.ClaimDocumentSummaryDto toDocumentSummaryDto(Document doc) {
        ClaimDetailsResponse.ClaimDocumentSummaryDto dto = new ClaimDetailsResponse.ClaimDocumentSummaryDto();
        dto.setDocumentId(doc.getDocumentId());
        dto.setDocumentType(doc.getDocumentType() != null ? doc.getDocumentType().getValue() : null);
        dto.setOriginalFilename(doc.getOriginalFilename());
        dto.setUploadedAt(doc.getUploadedAt());
        return dto;
    }

    /** Full list response (used when associations are already loaded to avoid N+1). */
    public static ClaimDetailsResponse toListResponse(Claim claim) {
        return toDetailsResponse(claim, new ArrayList<>());
    }

    /**
     * Lightweight list response: only maps fields needed for list view and only accesses
     * claim, organization, employee (no settlement, queries, deductions, auditLogs).
     * Use with fetch-joined claims to avoid N+1 and avoid loading heavy associations.
     */
    public static ClaimDetailsResponse toListResponseLight(Claim claim) {
        if (claim == null) return null;
        ClaimDetailsResponse dto = new ClaimDetailsResponse();
        dto.setId(claim.getId());
        dto.setClaimNumber(claim.getClaimNumber());
        if (claim.getOrganization() != null) {
            dto.setOrganizationId(claim.getOrganization().getOrganizationId());
            dto.setOrganizationName(claim.getOrganization().getOrganizationName());
        }
        dto.setPolicyId(claim.getPolicyId());
        if (claim.getEmployee() != null) {
            dto.setEmployeeId(claim.getEmployee().getIndividualId());
            dto.setEmployeeName(claim.getEmployee().getFirstName() != null ? claim.getEmployee().getFirstName() + " " + (claim.getEmployee().getLastName() != null ? claim.getEmployee().getLastName() : "") : null);
        }
        dto.setMemberName(claim.getMemberName());
        dto.setMemberType(claim.getMemberType());
        dto.setRelationship(claim.getRelationship());
        dto.setClaimType(claim.getClaimType());
        dto.setClaimCategory(claim.getClaimCategory());
        dto.setClaimAmount(claim.getClaimAmount());
        dto.setHospitalName(claim.getHospitalName());
        dto.setHospitalCity(claim.getHospitalCity());
        dto.setDateOfSubmission(claim.getDateOfSubmission());
        dto.setInternalStatus(claim.getInternalStatus());
        dto.setDisplayStatus(toDisplayStatus(claim.getInternalStatus()));
        dto.setCreatedAt(claim.getCreatedAt());
        dto.setUpdatedAt(claim.getUpdatedAt());
        return dto;
    }

    public static ClaimSummaryDto toSummaryDto(Claim claim) {
        if (claim == null) return null;
        ClaimSummaryDto dto = new ClaimSummaryDto();
        dto.setId(claim.getId());
        dto.setClaimNumber(claim.getClaimNumber());
        dto.setMemberName(claim.getMemberName());
        dto.setHospitalName(claim.getHospitalName());
        dto.setClaimAmount(claim.getClaimAmount());
        dto.setStatus(claim.getInternalStatus());
        dto.setDateOfSubmission(claim.getDateOfSubmission());
        return dto;
    }

    public static ClaimSummaryDto toSummaryFromDetails(ClaimDetailsResponse d) {
        if (d == null) return null;
        ClaimSummaryDto dto = new ClaimSummaryDto();
        dto.setId(d.getId());
        dto.setClaimNumber(d.getClaimNumber());
        dto.setMemberName(d.getMemberName());
        dto.setHospitalName(d.getHospitalName());
        dto.setClaimAmount(d.getClaimAmount());
        dto.setStatus(d.getInternalStatus());
        dto.setDateOfSubmission(d.getDateOfSubmission());
        return dto;
    }

    /** Map audit log entities to DTOs for admin audit trail API. */
    public static List<ClaimDetailsResponse.ClaimAuditLogDto> toAuditLogDtos(List<ClaimAuditLog> logs) {
        if (logs == null) return new ArrayList<>();
        return logs.stream().map(ClaimMapper::toAuditLogDto).collect(Collectors.toList());
    }
}
