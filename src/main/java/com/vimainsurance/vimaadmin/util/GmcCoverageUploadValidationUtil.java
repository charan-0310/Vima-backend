package com.vimainsurance.vimaadmin.util;

import com.vimainsurance.vimaadmin.dto.EmployeeUploadDto;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Validates relationship eligibility for GMC/GHI uploads based on coverage tier.
 */
public final class GmcCoverageUploadValidationUtil {

    private GmcCoverageUploadValidationUtil() {}

    public static List<String> validateBulkUploadRows(List<EmployeeUploadDto> rows, List<Policy> policies) {
        List<String> errors = new ArrayList<>();
        CoverageRule rule = resolveCoverageRule(policies, errors);
        if (rule == null || rows == null || rows.isEmpty()) {
            return errors;
        }

        LinkedHashMap<String, Integer> childCountByEmployeeId = new LinkedHashMap<>();
        for (EmployeeUploadDto row : rows) {
            if (row == null) {
                continue;
            }
            String relationship = row.getRelationship();
            RelationshipBucket bucket = toBucket(relationship);
            if (!isAllowedByTier(rule.coverageType(), bucket)) {
                String employeeId = row.getEmployeeId() != null ? row.getEmployeeId().trim() : "";
                errors.add("employeeId: " + employeeId + " - Relationship '" + relationship
                        + "' is not allowed for GMC/GHI coverageType " + rule.coverageType().getValue());
            }
            if (bucket == RelationshipBucket.CHILD) {
                String employeeId = row.getEmployeeId() != null ? row.getEmployeeId().trim() : "";
                childCountByEmployeeId.merge(employeeId, 1, Integer::sum);
            }
        }
        childCountByEmployeeId.forEach((employeeId, childCount) -> {
            if (childCount > rule.maxChildrenAllowed()) {
                errors.add("employeeId: " + employeeId + " - Child count " + childCount
                        + " exceeds allowed limit " + rule.maxChildrenAllowed()
                        + " for GMC/GHI coverageType " + rule.coverageType().getValue());
            }
        });
        return errors;
    }

    public static List<String> validateEnrollmentUploadRows(
            EnrollmentUploadParserUtil.EnrollmentParseResult parseResult,
            List<Policy> policies) {
        List<String> errors = new ArrayList<>();
        CoverageRule rule = resolveCoverageRule(policies, errors);
        if (rule == null || parseResult == null || parseResult.getDependentRowsByEmployeeId() == null) {
            return errors;
        }

        parseResult.getDependentRowsByEmployeeId().forEach((employeeId, dependentRows) -> {
            if (dependentRows == null) {
                return;
            }
            int childCount = 0;
            for (EnrollmentUploadParserUtil.DependentRow row : dependentRows) {
                if (row == null) {
                    continue;
                }
                RelationshipBucket bucket = toBucket(row.getRelationship());
                if (!isAllowedByTier(rule.coverageType(), bucket)) {
                    errors.add("Row " + row.getRowNumber() + " (employeeId: " + employeeId + ") - Relationship '"
                            + row.getRelationship() + "' is not allowed for GMC/GHI coverageType " + rule.coverageType().getValue());
                }
                if (bucket == RelationshipBucket.CHILD) {
                    childCount++;
                }
            }
            if (childCount > rule.maxChildrenAllowed()) {
                errors.add("employeeId: " + employeeId + " - Child count " + childCount
                        + " exceeds allowed limit " + rule.maxChildrenAllowed()
                        + " for GMC/GHI coverageType " + rule.coverageType().getValue());
            }
        });

        return errors;
    }

    private static CoverageRule resolveCoverageRule(List<Policy> policies, List<String> errors) {
        if (policies == null || policies.isEmpty()) {
            return null;
        }

        List<Policy> activeHealthPolicies = policies.stream()
                .filter(p -> p != null
                        && p.getStatus() == PolicyStatus.ACTIVE
                        && (p.getProductType() == ProductType.GMC || p.getProductType() == ProductType.GHI)
                        && p.getCoverageType() != null
                        && p.getCoverageType().isGMCCoverageType())
                .toList();

        if (activeHealthPolicies.isEmpty()) {
            return null;
        }

        if (activeHealthPolicies.size() > 1) {
            String policyIds = activeHealthPolicies.stream()
                    .map(p -> p.getPolicyId() != null ? p.getPolicyId().toString() : "unknown")
                    .collect(Collectors.joining(", "));
            errors.add("Ambiguous active GMC/GHI policies found for upload (policyIds: [" + policyIds
                    + "]). Keep only one active GMC/GHI policy before upload.");
            return null;
        }

        Policy selected = activeHealthPolicies.get(0);
        int maxChildrenAllowed = 4;
        if (selected.getCoverageType() == CoverageType.ESC || selected.getCoverageType() == CoverageType.ESCP) {
            maxChildrenAllowed = selected.getMaxChildrenAllowed() != null ? selected.getMaxChildrenAllowed() : 4;
        }
        return new CoverageRule(selected.getCoverageType(), maxChildrenAllowed);
    }

    private static boolean isAllowedByTier(CoverageType tier, RelationshipBucket bucket) {
        if (bucket == RelationshipBucket.OTHER) {
            return true;
        }
        return switch (tier) {
            case E -> bucket == RelationshipBucket.SELF;
            case ES -> bucket == RelationshipBucket.SELF || bucket == RelationshipBucket.SPOUSE;
            case ESC -> bucket == RelationshipBucket.SELF || bucket == RelationshipBucket.SPOUSE || bucket == RelationshipBucket.CHILD;
            case ESCP -> bucket == RelationshipBucket.SELF || bucket == RelationshipBucket.SPOUSE
                    || bucket == RelationshipBucket.CHILD || bucket == RelationshipBucket.PARENT;
            default -> true;
        };
    }

    private static RelationshipBucket toBucket(String relationship) {
        if (relationship == null || relationship.isBlank()) {
            return RelationshipBucket.OTHER;
        }

        String r = relationship.trim().toLowerCase(Locale.ROOT)
                .replace("_", " ")
                .replace("-", " ");

        if ("self".equals(r) || "employee".equals(r)) return RelationshipBucket.SELF;
        if ("spouse".equals(r)) return RelationshipBucket.SPOUSE;
        if (r.startsWith("child") || "son".equals(r) || "daughter".equals(r)) return RelationshipBucket.CHILD;

        if ("father".equals(r) || "mother".equals(r) || "parent".equals(r)
                || "father in law".equals(r) || "mother in law".equals(r)
                || "parent in law".equals(r)) {
            return RelationshipBucket.PARENT;
        }

        return RelationshipBucket.OTHER;
    }

    private enum RelationshipBucket {
        SELF, SPOUSE, CHILD, PARENT, OTHER
    }

    private record CoverageRule(CoverageType coverageType, int maxChildrenAllowed) {}
}
