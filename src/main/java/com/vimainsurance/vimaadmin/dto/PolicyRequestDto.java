package com.vimainsurance.vimaadmin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Policy creation and update requests
 */
@Data
public class PolicyRequestDto {

    @NotBlank(message = "Policy number is required")
    @Size(max = 100, message = "Policy number must not exceed 100 characters")
    private String policyNumber;

    @NotNull(message = "Primary individual ID is required")
    private UUID primaryIndividualId;

    @NotNull(message = "Insurance provider ID is required")
    private UUID insuranceProviderId;

    private UUID insuranceProductId;

    private UUID organizationId;

    @NotNull(message = "Product type is required")
    private String productType;

    @NotNull(message = "Coverage type is required")
    private String coverageType;

    private String status;

    private String coveredIndividuals;

    @NotNull(message = "Sum insured is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Sum insured must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Sum insured must have at most 13 integer digits and 2 decimal places")
    private BigDecimal sumInsured;

    @NotNull(message = "Premium amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Premium amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Premium amount must have at most 13 integer digits and 2 decimal places")
    private BigDecimal premiumAmount;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate renewalDate;

    private UUID leadId;
}
