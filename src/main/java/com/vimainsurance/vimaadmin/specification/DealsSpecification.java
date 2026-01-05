package com.vimainsurance.vimaadmin.specification;

import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import com.vimainsurance.vimaadmin.entity.Deals;
import jakarta.persistence.criteria.Predicate;
public class DealsSpecification {
    /**
     * Count deals by relationship type for a given endorsement
     * @param endorsementId The endorsement ID
     * @param isSelf true to count "SELF", false to count "non-SELF"
     */
    public static Specification<Deals> countByRelationship(UUID endorsementId, boolean isSelf) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            
            if (endorsementId != null) {
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.equal(root.get("endorsementId"), endorsementId)
                );
            }
            
            if (isSelf) {
                // Count SELF
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.equal(root.get("relationship"), "SELF")
                );
            } else {
                // Count non-SELF
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.or(
                        criteriaBuilder.notEqual(root.get("relationship"), "SELF"),
                        criteriaBuilder.isNull(root.get("relationship"))
                    )
                );
            }
            
            return predicate;
        };
    }

    /**
     * Count employees for a given organization
     * Employees are deals that belong to an organization and are primary members
     * @param organizationId The organization ID
     * @return Specification for counting employees by organization ID
     */
    public static Specification<Deals> countEmployeesByOrganizationId(UUID organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get("organization").get("organizationId"), organizationId),
                criteriaBuilder.equal(root.get("isPrimaryMember"), true)
            );
        };
    }

    /**
     * Count dependents for a given organization
     * Dependents are deals that have a primaryIndividual belonging to the organization
     * and relationship is not "SELF"
     * @param organizationId The organization ID
     * @return Specification for counting dependents by organization ID
     */
    public static Specification<Deals> countDependentsByOrganizationId(UUID organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(
                criteriaBuilder.isNotNull(root.get("primaryIndividual")),
                criteriaBuilder.equal(
                    root.get("primaryIndividual").get("organization").get("organizationId"), 
                    organizationId
                ),
                criteriaBuilder.or(
                    criteriaBuilder.notEqual(root.get("relationship"), "SELF"),
                    criteriaBuilder.isNull(root.get("relationship"))
                )
            );
        };
    }
}
