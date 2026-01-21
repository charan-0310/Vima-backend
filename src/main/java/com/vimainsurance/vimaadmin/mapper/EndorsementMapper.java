package com.vimainsurance.vimaadmin.mapper;

import java.time.LocalDateTime;

import com.vimainsurance.vimaadmin.dto.EndorsementRequestDto;
import com.vimainsurance.vimaadmin.dto.EndorsementResponseDto;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.ConfirmationMethod;
import com.vimainsurance.vimaadmin.enums.EndorsementType;
import com.vimainsurance.vimaadmin.enums.PremiumChangeType;

public class EndorsementMapper {

    /**
     * Maps EndorsementRequestDto to Endorsement entity
     */
    public static Endorsement mapToEntity(EndorsementRequestDto dto, Organization organization, Document document, 
                                          AdminUser uploadedBy) {
        Endorsement endorsement = new Endorsement();
        
        if (dto.getEndorsementId() != null) {
            endorsement.setEndorsementId(dto.getEndorsementId());
        }
        
        endorsement.setOrganization(organization);
        endorsement.setDocument(document);
        
        if (dto.getEndorsementType() != null) {
            endorsement.setEndorsementType(EndorsementType.fromValue(dto.getEndorsementType()));
        }
        
        if (dto.getStatus() != null) {
            endorsement.setStatus(AccountStatus.fromValue(dto.getStatus()));
        } else {
            endorsement.setStatus(AccountStatus.PENDING_APPROVAL);
        }
        
        endorsement.setTotalEmployees(dto.getTotalEmployees() != null ? dto.getTotalEmployees() : 0);
        endorsement.setTotalDependents(dto.getTotalDependents() != null ? dto.getTotalDependents() : 0);
        endorsement.setApprovedBy(dto.getApprovedBy());
        endorsement.setUploadedBy(uploadedBy);
        
        if (dto.getConfirmationMethod() != null) {
            endorsement.setConfirmationMethod(ConfirmationMethod.fromValue(dto.getConfirmationMethod()));
        }
        
        endorsement.setInsurerRefNumber(dto.getInsurerRefNumber());
        
        if (dto.getPremiumChangeType() != null) {
            endorsement.setPremiumChangeType(PremiumChangeType.fromValue(dto.getPremiumChangeType()));
        }
        
        endorsement.setPremiumAmount(dto.getPremiumAmount());
        endorsement.setUpdatedAt(LocalDateTime.now());
        
        return endorsement;
    }

    /**
     * Maps Endorsement entity to EndorsementResponseDto
     */
    public static EndorsementResponseDto mapToResponseDto(Endorsement endorsement) {
        EndorsementResponseDto dto = new EndorsementResponseDto();
        
        dto.setEndorsementId(endorsement.getEndorsementId());
        
        if (endorsement.getOrganization() != null) {
            dto.setOrganizationId(endorsement.getOrganization().getOrganizationId());
            dto.setOrganizationName(endorsement.getOrganization().getOrganizationName());
        }
        
        if (endorsement.getDocument() != null) {
            dto.setDocumentId(endorsement.getDocument().getDocumentId());
        }
        
        if (endorsement.getEndorsementType() != null) {
            dto.setEndorsementType(endorsement.getEndorsementType().getValue());
        }
        
        if (endorsement.getStatus() != null) {
            dto.setStatus(endorsement.getStatus().getValue());
        }
        
        dto.setTotalEmployees(endorsement.getTotalEmployees());
        dto.setTotalDependents(endorsement.getTotalDependents());
        dto.setApprovedAt(endorsement.getApprovedAt());
        
        if (endorsement.getApprovedBy() != null) {
            dto.setApprovedBy(endorsement.getApprovedBy());
        }
        
        if (endorsement.getUploadedBy() != null) {
            dto.setUploadedBy(endorsement.getUploadedBy().getId());
            dto.setUploadedByName(endorsement.getUploadedBy().getFullName());
        }
        
        if (endorsement.getConfirmationMethod() != null) {
            dto.setConfirmationMethod(endorsement.getConfirmationMethod().getValue());
        }
        
        dto.setInsurerRefNumber(endorsement.getInsurerRefNumber());
        
        if (endorsement.getPremiumChangeType() != null) {
            dto.setPremiumChangeType(endorsement.getPremiumChangeType().getValue());
        }
        
        dto.setPremiumAmount(endorsement.getPremiumAmount());
        dto.setCreatedAt(endorsement.getCreatedAt());
        dto.setUpdatedAt(endorsement.getUpdatedAt());
        
        return dto;
    }

    /**
     * Updates an existing Endorsement entity from EndorsementRequestDto
     */
    public static void updateEntityFromDto(Endorsement endorsement, EndorsementRequestDto dto, 
                                           Organization organization, Document document,
                                           AdminUser uploadedBy) {
        if (organization != null) {
            endorsement.setOrganization(organization);
        }
        
        if (document != null) {
            endorsement.setDocument(document);
        }
        
        if (dto.getEndorsementType() != null) {
            endorsement.setEndorsementType(EndorsementType.fromValue(dto.getEndorsementType()));
        }
        
        if (dto.getStatus() != null) {
            endorsement.setStatus(AccountStatus.fromValue(dto.getStatus()));
        }
        
        if (dto.getTotalEmployees() != null) {
            endorsement.setTotalEmployees(dto.getTotalEmployees());
        }
        
        if (dto.getTotalDependents() != null) {
            endorsement.setTotalDependents(dto.getTotalDependents());
        }
        
        if (dto.getApprovedBy() != null) {
            endorsement.setApprovedBy(dto.getApprovedBy().trim());
        }
        
        if (uploadedBy != null) {
            endorsement.setUploadedBy(uploadedBy);
        }
        
        if (dto.getConfirmationMethod() != null) {
            endorsement.setConfirmationMethod(ConfirmationMethod.fromValue(dto.getConfirmationMethod()));
        }
        
        if (dto.getInsurerRefNumber() != null) {
            endorsement.setInsurerRefNumber(dto.getInsurerRefNumber());
        }
        
        if (dto.getPremiumChangeType() != null) {
            endorsement.setPremiumChangeType(PremiumChangeType.fromValue(dto.getPremiumChangeType()));
        }
        
        if (dto.getPremiumAmount() != null) {
            endorsement.setPremiumAmount(dto.getPremiumAmount());
        }
        
        endorsement.setUpdatedAt(LocalDateTime.now());
    }
}

