package com.vimainsurance.vimaadmin.util;

import com.vimainsurance.vimaadmin.dto.PolicyRequestDto;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.exception.BadRequestException;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating policy business rules
 */
public class PolicyValidationUtil {

    /**
     * Validate policy request based on policy type.
     * <p>GMC: TPA, coverage type, sum insured, etc. For {@code isUpdate == true}, both TPA fields may be
     * omitted together to leave existing database values unchanged.</p>
     *
     * @param isUpdate when true, GMC policies may omit both TPA fields to leave existing DB values unchanged
     */
    public static void validatePolicyRequest(PolicyRequestDto request, boolean isUpdate) {
        List<String> errors = new ArrayList<>();

        if (request.getProductType() == null || request.getProductType().trim().isEmpty()) {
            errors.add("Policy type is required");
            throw new BadRequestException("Validation failed: " + String.join(", ", errors));
        }

        ProductType policyType;
        try {
            policyType = ProductType.fromValue(request.getProductType());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid policy type: " + request.getProductType() + ". Valid values are: GMC, GPA, GTL, PARENT_GMC, TOP_UP, SUPER_TOP_UP");
        }

        switch (policyType) {
            case GMC:
                validateGMCPolicy(request, errors, isUpdate);
                break;
            case GPA:
            case GTL:
                validateGPAGTLPolicy(request, errors);
                break;
            case PARENT_GMC:
                validateParentGMCPolicy(request, errors);
                break;
            case TOP_UP:
            case SUPER_TOP_UP:
                validateTopupPolicy(request, errors);
                break;
            default:
                break;
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException("Validation failed: " + String.join(", ", errors));
        }
    }

    /** Create / full validation: TPA required for GMC. */
    public static void validatePolicyRequest(PolicyRequestDto request) {
        validatePolicyRequest(request, false);
    }

    private static void validateGMCPolicy(PolicyRequestDto request, List<String> errors, boolean isUpdate) {
        boolean namePresent = request.getTpaOrganizationName() != null && !request.getTpaOrganizationName().trim().isEmpty();
        boolean contactPresent = request.getTpaContactInfo() != null && !request.getTpaContactInfo().trim().isEmpty();
        // Updates: omitting both TPA fields means "leave existing values" (legacy rows may have null TPA)
        boolean skipTpaRequired = isUpdate && !namePresent && !contactPresent;
        if (!skipTpaRequired) {
            if (!namePresent) {
                errors.add("TPA Organization Name is required for GMC policies");
            }
            if (!contactPresent) {
                errors.add("TPA Contact Info is required for GMC policies");
            }
        }

        // Coverage Type is required for GMC
        if (request.getCoverageType() == null || request.getCoverageType().trim().isEmpty()) {
            errors.add("Coverage Type is required for GMC policies (E, ES, ESC, or ESCP)");
        }
        CoverageType coverageType = null;
        if (request.getCoverageType() != null && !request.getCoverageType().trim().isEmpty()) {
            try {
                coverageType = CoverageType.fromValue(request.getCoverageType());
            } catch (Exception e) {
                errors.add("Invalid coverage type for GMC policy: " + request.getCoverageType());
            }
        }

        // Sum Insured is required for GMC
        if (request.getSumInsured() == null || request.getSumInsured().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Sum Insured is required for GMC policies and must be greater than 0");
        }

        // Sum Insured Multiplier should not be provided for GMC
        if (request.getSumInsuredMultiplier() != null) {
            errors.add("Sum Insured Multiplier should not be provided for GMC policies. It's only applicable for GPA/GTL");
        }

        if (coverageType == CoverageType.ESC || coverageType == CoverageType.ESCP) {
            Integer maxChildrenAllowed = request.getMaxChildrenAllowed() != null ? request.getMaxChildrenAllowed() : 4;
            if (maxChildrenAllowed < 1 || maxChildrenAllowed > 4) {
                errors.add("maxChildrenAllowed must be between 1 and 4 for GMC policies with ESC/ESCP coverage");
            }
        }
    }

    /**
     * PARENT_GMC (Parent/In-Law coverage) requires a sum insured amount, same as GMC.
     * TPA and coverage type are not required for parent coverage.
     */
    private static void validateParentGMCPolicy(PolicyRequestDto request, List<String> errors) {
        if (request.getSumInsured() == null || request.getSumInsured().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Sum Insured is required for Parent/In-Law Coverage (PARENT_GMC) and must be greater than 0");
        }
        if (request.getSumInsuredMultiplier() != null) {
            errors.add("Sum Insured Multiplier should not be provided for PARENT_GMC policies. It's only applicable for GPA/GTL");
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

    private static void validateTopupPolicy(PolicyRequestDto request, List<String> errors) {
        if (request.getDeductibleAmount() == null || request.getDeductibleAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            errors.add("Deductible amount is required for Top-Up / Super Top-Up and must be zero or greater");
        }
        if (request.getEffectiveFrom() == null && request.getStartDate() == null) {
            errors.add("Effective From or Start Date is required for Top-Up / Super Top-Up");
        }
        if (request.getSumInsuredOptions() == null || request.getSumInsuredOptions().trim().isEmpty()) {
            errors.add("Sum insured options are required for Top-Up / Super Top-Up");
        }
        if (request.getTopupPremiumOptions() == null || request.getTopupPremiumOptions().trim().isEmpty()) {
            errors.add("Premium amounts are required for Top-Up / Super Top-Up");
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
            case PARENT_GMC -> "Parent/In-Law Coverage";
            case TOP_UP -> "Top-up";
            case SUPER_TOP_UP -> "Super Top-up";
            default -> policyType.getValue();
        };
    }
}
