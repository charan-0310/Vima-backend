package com.vimainsurance.vimaadmin.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerUpcomingRenewalDto {
    private String id;
    private String organizationName;
    private String organizationDisplayName;
    private String organizationId;
    private String policyNumber;
    private String product;
    private double sumInsured;
    private String expiryDate;
    private int daysLeft;
}
