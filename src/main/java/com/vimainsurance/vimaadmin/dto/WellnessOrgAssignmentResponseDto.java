package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessOrgAssignmentResponseDto {
    private List<WellnessPartnerOrgResponseDto> assignedPartners;
    private List<WellnessPartnerResponseDto> availablePartners;
}
