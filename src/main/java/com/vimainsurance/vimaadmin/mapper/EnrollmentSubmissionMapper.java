package com.vimainsurance.vimaadmin.mapper;

import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

public final class EnrollmentSubmissionMapper {

    private EnrollmentSubmissionMapper() {
    }

    /**
     * Maps request DTO to new entity (for insert).
     */
    public static EnrollmentSubmission mapToEntity(
            EnrollmentSubmissionRequestDto dto,
            Deals employee,
            EnrollmentWindows enrollmentWindow,
            EnrollmentInvitation invitation,
            Endorsement endorsement,
            AdminUser reviewedBy) {
        EnrollmentSubmission entity = new EnrollmentSubmission();
        entity.setEmployee(employee);
        entity.setEnrollmentWindow(enrollmentWindow);
        entity.setInvitation(invitation);
        entity.setEndorsement(endorsement);
        if (dto.getReferenceNumber() != null) {
            entity.setReferenceNumber(dto.getReferenceNumber());
        }
        entity.setStatus(dto.getStatus() != null && !dto.getStatus().isBlank()
            ? EnrollementStatus.fromValue(dto.getStatus().trim().toUpperCase())
            : EnrollementStatus.DRAFT);
        entity.setPlanSelections(dto.getPlanSelections() != null ? dto.getPlanSelections() : "[]");
        entity.setNomineeData(dto.getNomineeData() != null ? dto.getNomineeData() : "{}");
        entity.setPersonalDetails(dto.getPersonalDetails() != null ? dto.getPersonalDetails() : "{}");
        entity.setDependents(dto.getDependents() != null ? dto.getDependents() : "{}");
        entity.setStage(dto.getStage() != null ? dto.getStage() : "");
        entity.setPremiumBreakdown(dto.getPremiumBreakdown() != null ? dto.getPremiumBreakdown() : "{}");
        entity.setSubmittedAt(dto.getSubmittedAt());
        entity.setReviewedBy(reviewedBy);
        entity.setReviewedAt(dto.getReviewedAt());
        entity.setRejectionReason(dto.getRejectionReason());
        entity.setDeclarationAccepted(Boolean.TRUE.equals(dto.getDeclarationAccepted()));
        entity.setDeclarationTimestamp(dto.getDeclarationTimestamp());
        entity.setDeclarationIpAddress(dto.getDeclarationIpAddress());
        if (dto.getIdempotencyKey() != null) {
            entity.setIdempotencyKey(dto.getIdempotencyKey());
        }
        return entity;
    }

    /**
     * Updates existing entity from DTO (for update).
     */
    public static void updateEntityFromDto(
            EnrollmentSubmission entity,
            EnrollmentSubmissionRequestDto dto,
            Deals employee,
            EnrollmentWindows enrollmentWindow,
            EnrollmentInvitation invitation,
            Endorsement endorsement,
            AdminUser reviewedBy) {
        if (employee != null) {
            entity.setEmployee(employee);
        }
        if (enrollmentWindow != null) {
            entity.setEnrollmentWindow(enrollmentWindow);
        }
        if (invitation != null) {
            entity.setInvitation(invitation);
        }
        if (endorsement != null) {
            entity.setEndorsement(endorsement);
        }
        if (dto.getReferenceNumber() != null) {
            entity.setReferenceNumber(dto.getReferenceNumber());
        }
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            entity.setStatus(EnrollementStatus.fromValue(dto.getStatus().trim().toUpperCase()));
        }
        if (dto.getPlanSelections() != null) {
            entity.setPlanSelections(dto.getPlanSelections());
        }
        if (dto.getNomineeData() != null) {
            entity.setNomineeData(dto.getNomineeData());
        }
        if (dto.getPersonalDetails() != null) {
            entity.setPersonalDetails(dto.getPersonalDetails());
        }
        if (dto.getDependents() != null) {
            entity.setDependents(dto.getDependents());
        }
        if (dto.getStage() != null) {
            entity.setStage(dto.getStage());
        }
        if (dto.getPremiumBreakdown() != null) {
            entity.setPremiumBreakdown(dto.getPremiumBreakdown());
        }
        if (dto.getSubmittedAt() != null) {
            entity.setSubmittedAt(dto.getSubmittedAt());
        }
        entity.setReviewedBy(reviewedBy);
        if (dto.getReviewedAt() != null) {
            entity.setReviewedAt(dto.getReviewedAt());
        }
        if (dto.getRejectionReason() != null) {
            entity.setRejectionReason(dto.getRejectionReason());
        }
        if (dto.getDeclarationAccepted() != null) {
            entity.setDeclarationAccepted(dto.getDeclarationAccepted());
        }
        if (dto.getDeclarationTimestamp() != null) {
            entity.setDeclarationTimestamp(dto.getDeclarationTimestamp());
        }
        if (dto.getDeclarationIpAddress() != null) {
            entity.setDeclarationIpAddress(dto.getDeclarationIpAddress());
        }
        if (dto.getIdempotencyKey() != null) {
            entity.setIdempotencyKey(dto.getIdempotencyKey());
        }
    }

    /**
     * Maps entity to response DTO.
     */
    public static EnrollmentSubmissionResponseDto mapToResponseDto(EnrollmentSubmission entity) {
        EnrollmentSubmissionResponseDto dto = new EnrollmentSubmissionResponseDto();
        dto.setId(entity.getId());
        if (entity.getEmployee() != null) {
            dto.setEmployeeId(entity.getEmployee().getIndividualId());
        }
        if (entity.getEnrollmentWindow() != null) {
            dto.setEnrollmentWindowId(entity.getEnrollmentWindow().getId());
        }
        if (entity.getInvitation() != null) {
            dto.setInvitationId(entity.getInvitation().getId());
        }
        if (entity.getEndorsement() != null) {
            dto.setEndorsementId(entity.getEndorsement().getEndorsementId());
        }
        dto.setReferenceNumber(entity.getReferenceNumber());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().getValue() : null);
        dto.setPlanSelections(entity.getPlanSelections());
        dto.setNomineeData(entity.getNomineeData());
        dto.setPersonalDetails(entity.getPersonalDetails());
        dto.setDependents(entity.getDependents());
        dto.setStage(entity.getStage());
        dto.setPremiumBreakdown(entity.getPremiumBreakdown());
        dto.setSubmittedAt(entity.getSubmittedAt());
        if (entity.getReviewedBy() != null) {
            dto.setReviewedById(entity.getReviewedBy().getId());
        }
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setDeclarationAccepted(entity.getDeclarationAccepted());
        dto.setDeclarationTimestamp(entity.getDeclarationTimestamp());
        dto.setDeclarationIpAddress(entity.getDeclarationIpAddress());
        dto.setVersion(entity.getVersion());
        dto.setIdempotencyKey(entity.getIdempotencyKey());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
