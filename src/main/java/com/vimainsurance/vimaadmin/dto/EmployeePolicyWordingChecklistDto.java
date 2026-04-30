package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePolicyWordingChecklistDto {
    private Long policyId;
    private String policyNumber;
    private String productType;
    private String policyWording;
    private String claimChecklist;
    private LocalDateTime updatedAt;
}
