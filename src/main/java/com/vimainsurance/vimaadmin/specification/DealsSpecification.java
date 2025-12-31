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
}
