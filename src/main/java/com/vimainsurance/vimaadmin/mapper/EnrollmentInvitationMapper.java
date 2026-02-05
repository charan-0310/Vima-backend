package com.vimainsurance.vimaadmin.mapper;

import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.dto.EnrollmentInvitationRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentInvitationResponseDto;

import java.util.List;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Deals;

public class EnrollmentInvitationMapper {

    public static EnrollmentInvitation mapToEntity(EnrollmentInvitationRequestDto dto, EnrollmentWindows enrollmentWindow, Deals employee) {
        EnrollmentInvitation enrollmentInvitation = new EnrollmentInvitation();
        enrollmentInvitation.setEnrollmentWindow(enrollmentWindow);
        enrollmentInvitation.setEmployee(employee);
        enrollmentInvitation.setTokenHash(dto.getTokenHash());
        enrollmentInvitation.setStatus(EnrollementStatus.valueOf(dto.getStatus()));
        enrollmentInvitation.setSentAt(dto.getSentAt());
        enrollmentInvitation.setOpenedAt(dto.getOpenedAt());
        enrollmentInvitation.setCompletedAt(dto.getCompletedAt());
        enrollmentInvitation.setExpiresAt(dto.getExpiresAt());
        enrollmentInvitation.setReminderCount(dto.getReminderCount());
        enrollmentInvitation.setLastReminderAt(dto.getLastReminderAt());
        return enrollmentInvitation;
    }

    public static EnrollmentInvitationResponseDto toDto(EnrollmentInvitation entity) {
        return EnrollmentInvitationResponseDto.builder()
            .id(entity.getId())
            .enrollmentWindowId(entity.getEnrollmentWindow().getId())
            .employeeId(entity.getEmployee().getIndividualId())
            .tokenHash(entity.getTokenHash())
            .status(entity.getStatus().name())
            .sentAt(entity.getSentAt())
            .openedAt(entity.getOpenedAt())
            .completedAt(entity.getCompletedAt())
            .expiresAt(entity.getExpiresAt())
            .reminderCount(entity.getReminderCount())
            .lastReminderAt(entity.getLastReminderAt())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    public static List<EnrollmentInvitationResponseDto> toDtoList(List<EnrollmentInvitation> entities) {
        return entities.stream()
            .map(EnrollmentInvitationMapper::toDto)
            .collect(Collectors.toList());
    }
}
