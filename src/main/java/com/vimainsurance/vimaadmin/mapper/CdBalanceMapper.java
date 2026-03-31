package com.vimainsurance.vimaadmin.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.CdBalanceResponseDto;
import com.vimainsurance.vimaadmin.dto.CdBalanceTransactionResponseDto;
import com.vimainsurance.vimaadmin.entity.CdBalanceTransaction;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Policy;

public final class CdBalanceMapper {

    private CdBalanceMapper() {
    }

    public static CdBalanceResponseDto toBalanceResponseDto(Policy policy) {
        CdBalanceResponseDto dto = new CdBalanceResponseDto();
        dto.setPolicyId(policy.getPolicyId());
        dto.setOrganizationId(policy.getOrganizationId());
        dto.setCdBalance(policy.getCdBalance() == null ? BigDecimal.ZERO : policy.getCdBalance());
        dto.setUpdatedAt(policy.getUpdatedAt());
        return dto;
    }

    public static CdBalanceTransactionResponseDto toTransactionResponseDto(
            CdBalanceTransaction transaction,
            List<UUID> documentIds) {
        CdBalanceTransactionResponseDto dto = new CdBalanceTransactionResponseDto();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setCdAccountId(transaction.getCdAccountId());
        dto.setPolicyId(transaction.getPolicy() != null ? transaction.getPolicy().getPolicyId() : null);
        dto.setOrganizationId(transaction.getOrganizationId());
        Endorsement endorsement = transaction.getEndorsement();
        if (endorsement != null) {
            dto.setEndorsementId(endorsement.getEndorsementId());
            if (endorsement.getOrganization() != null) {
                dto.setEndorsementOrganizationName(endorsement.getOrganization().getOrganizationName());
            }
            if (endorsement.getEndorsementType() != null) {
                dto.setEndorsementType(endorsement.getEndorsementType().getValue());
            }
            dto.setEndorsementInsurerRefNumber(endorsement.getInsurerRefNumber());
            if (endorsement.getEnrollmentWindow() != null) {
                dto.setEndorsementEnrollmentWindowName(endorsement.getEnrollmentWindow().getName());
            }
        } else {
            dto.setEndorsementId(null);
        }
        dto.setTransactionType(transaction.getTransactionType() != null ? transaction.getTransactionType().name() : null);
        dto.setAmount(transaction.getAmount());
        dto.setRunningBalance(transaction.getRunningBalance());
        dto.setDescription(transaction.getDescription());
        dto.setNotes(transaction.getNotes());
        dto.setReferenceNumber(transaction.getReferenceNumber());
        dto.setSource(transaction.getSource() != null ? transaction.getSource().name() : null);
        dto.setPerformedBy(transaction.getPerformedBy());
        dto.setDocumentIds(documentIds);
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());
        return dto;
    }
}
