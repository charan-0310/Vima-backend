package com.vimainsurance.vimaadmin.specification;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.vimainsurance.vimaadmin.entity.Organization;

import jakarta.persistence.criteria.Predicate;

/**
 * Specification class for dynamic Organization filtering
 */
public class OrganizationSpecification {

    /**
     * Builds a Specification for filtering organizations based on provided criteria
     * 
     * @param search Search term to match against organization name, GSTIN, PAN, contact name, email, or phone
     * @param status Organization status filter
     * @return Specification for filtering organizations
     */
    public static Specification<Organization> withFilters(List<UUID> organizationIds, String search, String status) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            //
            // Filter by organization IDs
            if (organizationIds != null && !organizationIds.isEmpty()) {
                predicate = criteriaBuilder.and(
                    predicate,
                    root.get("organizationId").in(organizationIds)
                );
            }
            
            // Filter by search term (case-insensitive, partial match across multiple fields)
            if (search != null && !search.trim().isEmpty()) {
                String searchTerm = search.trim().toLowerCase();
                Predicate searchPredicate = criteriaBuilder.or(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("organizationName")),
                        "%" + searchTerm + "%"
                    ),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("organizationDisplayName")),
                        "%" + searchTerm + "%"
                    ),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("gstin")),
                        "%" + searchTerm + "%"
                    ),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("panNumber")),
                        "%" + searchTerm + "%"
                    ),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("primaryContactName")),
                        "%" + searchTerm + "%"
                    ),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("primaryContactEmail")),
                        "%" + searchTerm + "%"
                    ),
                    criteriaBuilder.like(
                        root.get("primaryContactPhone"),
                        "%" + searchTerm + "%"
                    )
                );
                predicate = criteriaBuilder.and(predicate, searchPredicate);
            }
            
            // Filter by status
            if (status != null && !status.trim().isEmpty()) {
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.equal(root.get("status"), status.trim())
                );
            }
            
            return predicate;
        };
    }
}

