package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal policy info returned in enrollment context so the employee portal
 * can show only the plans (GMC/GHI, GPA, GTL) that the organization has.
 * Not the full policy — only type and coverage for UI display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentOrganizationPolicyDto {

    private Long policyId;
    private String policyNumber;
    /** Product type: GHI, GMC, GPA, GTL */
    private String productType;
    private BigDecimal sumInsured;
    /** Same as sumInsured for compatibility with frontend; GMC may use coverage amount. */
    private BigDecimal coverageAmount;

    private String coverageType;

    /** For GPA/GTL: CTC multiplier (e.g. 3 for 3x CTC). When set and sumInsured is null/zero, UI shows "Nx CTC". */
    private Integer sumInsuredMultiplier;

    private String insurerName;

    /** yyyy-MM-dd — policy effective_from, or start_date when effective_from is null (same basis as dependent DOJ default). */
    private String effectiveFrom;

    /** Policy lifecycle status, e.g. ACTIVE (for filtering GMC default-DOJ sources). */
    private String policyStatus;
}
