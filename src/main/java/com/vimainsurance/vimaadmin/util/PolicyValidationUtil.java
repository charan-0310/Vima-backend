package com.vimainsurance.vimaadmin.util;

import com.vimainsurance.vimaadmin.dto.PolicyRequestDto;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.exception.BadRequestException;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating policy business rules
 */
public class PolicyValidationUtil {

    /**
     * Validate policy request based on policy type
     *
     * GMC Policies must have:
     * - TPA Name (required)
     * - TPA Contact Info (required)
     * - Coverage Type (required: E, ES, ESC, ESCP)
     * - Fixed sum insured amount (required)
     * - Cannot have sum_insured_multiplier
     *
     * GPA/GTL Policies must have:
     * - Sum Insured Multiplier (required: 1-5)
     * - Cannot have TPA fields
     * - Cannot have coverage type
     * - Cannot have dependents (enforced at enrollment level)
     */
    public static void validatePolicyRequest(PolicyRequestDto request) {
        List<String> errors = new ArrayList<>();

        if (request.getProductType() == null || request.getProductType().trim().isEmpty()) {
            errors.add("Policy type is required");
            throw new BadRequestException("Validation failed: " + String.join(", ", errors));
        }

        ProductType policyType;
        try {
            policyType = ProductType.fromValue(request.getProductType());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid policy type: " + request.getProductType() + ". Valid values are: GMC, GPA, GTL");
        }

        switch (policyType) {
            case GMC:
                validateGMCPolicy(request, errors);
                break;
            case GPA:
            case GTL:
                validateGPAGTLPolicy(request, errors);
                break;
            default:
                // For other product types, no special validation
                break;
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException("Validation failed: " + String.join(", ", errors));
        }
    }

    private static void validateGMCPolicy(PolicyRequestDto request, List<String> errors) {
        // TPA Name is required for GMC
        if (request.getTpaOrganizationName() == null || request.getTpaOrganizationName().trim().isEmpty()) {
            errors.add("TPA Organization Name is required for GMC policies");
        }

        // TPA Contact Info is required for GMC
        if (request.getTpaContactInfo() == null || request.getTpaContactInfo().trim().isEmpty()) {
            errors.add("TPA Contact Info is required for GMC policies");
        }

        // Coverage Type is required for GMC
        if (request.getCoverageType() == null || request.getCoverageType().trim().isEmpty()) {
            errors.add("Coverage Type is required for GMC policies (E, ES, ESC, or ESCP)");
        }

        // Sum Insured is required for GMC
        if (request.getSumInsured() == null || request.getSumInsured().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Sum Insured is required for GMC policies and must be greater than 0");
        }

        // Sum Insured Multiplier should not be provided for GMC
        if (request.getSumInsuredMultiplier() != null) {
            errors.add("Sum Insured Multiplier should not be provided for GMC policies. It's only applicable for GPA/GTL");
        }
    }

    private static void validateGPAGTLPolicy(PolicyRequestDto request, List<String> errors) {
        // Sum Insured Multiplier is required for GPA/GTL
        if (request.getSumInsuredMultiplier() == null) {
            errors.add("Sum Insured Multiplier is required for GPA/GTL policies (1-5)");
        } else if (request.getSumInsuredMultiplier() < 1 || request.getSumInsuredMultiplier() > 5) {
            errors.add("Sum Insured Multiplier must be between 1 and 5 for GPA/GTL policies");
        }

        // TPA fields should not be provided for GPA/GTL
        if (request.getTpaOrganizationName() != null && !request.getTpaOrganizationName().trim().isEmpty()) {
            errors.add("TPA Organization Name should not be provided for GPA/GTL policies. It's only applicable for GMC");
        }

        if (request.getTpaContactInfo() != null && !request.getTpaContactInfo().trim().isEmpty()) {
            errors.add("TPA Contact Info should not be provided for GPA/GTL policies. It's only applicable for GMC");
        }

        // Coverage Type should not be provided for GPA/GTL
        if (request.getCoverageType() != null && !request.getCoverageType().trim().isEmpty()) {
            errors.add("Coverage Type should not be provided for GPA/GTL policies. It's only applicable for GMC");
        }

        // Dependents validation (warning - actual enforcement at enrollment level)
        if (request.getDependents() != null && !request.getDependents().isEmpty()) {
            errors.add("GPA/GTL policies cover employees only. Dependents should not be added for GPA/GTL policies");
        }
    }

    /**
     * Get user-friendly policy type name
     */
    public static String getPolicyTypeName(ProductType policyType) {
        return switch (policyType) {
            case GMC -> "Group Medical Coverage (Health Insurance)";
            case GPA -> "Group Personal Accident";
            case GTL -> "Group Term Life";
            default -> policyType.getValue();
        };
    }
}
