package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal context for premium calculation built from enrollment token validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumCalculationContext {

    private UUID companyId;
    private UUID employeeId;
    private LocalDate employeeDateOfBirth;
    private LocalDate windowStartDate;
    private LocalDate windowEndDate;
}
