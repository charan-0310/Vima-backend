package com.vimainsurance.vimaadmin.specification;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.EndorsementType;

import jakarta.persistence.criteria.Predicate;

/**
 * Specification class for dynamic Endorsement filtering
 */
public class EndorsementSpecification {

    /**
     * Builds a Specification for filtering endorsements based on provided criteria
     */
    public static Specification<Endorsement> withFilters(
            List<UUID> organizationIds,
            String organizationName,
            String status,
            EndorsementType endorsementType,
            String uploadedBy,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            
            // Filter by organization ID
            if (organizationIds != null && !organizationIds.isEmpty()) {
                if (organizationIds.size() == 1) {
                    predicate = criteriaBuilder.and(
                            predicate,
                            criteriaBuilder.equal(root.get("organization").get("organizationId"), organizationIds.get(0))
                    );
                } else {
                    predicate = criteriaBuilder.and(
                            predicate,
                            root.get("organization").get("organizationId").in(organizationIds)
                    );
                }
            }
            if(fromDate != null) {
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate)
                );
            }
            if(toDate != null) {
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate)
                );
            }
            
            // Filter by organization name (case-insensitive, partial match)
            if (organizationName != null && !organizationName.trim().isEmpty()) {
                String searchName = organizationName.trim().toLowerCase();
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("organization").get("organizationName")),
                        "%" + searchName + "%"
                    )
                );
            }
            if(uploadedBy != null && !uploadedBy.trim().isEmpty()) {
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("uploadedBy").get("username")),
                        "%" + uploadedBy + "%"
                    )                
                );
            }
            // Filter by status
            if (status != null) {
                if(status.contains("PENDING")){
                    predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("status"), AccountStatus.PENDING_APPROVAL),
                            criteriaBuilder.equal(root.get("status"), AccountStatus.PENDING_EXIT)
                        )
                    );
                }
                else if(status.contains("APPROVED")){
                    predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("status"), AccountStatus.APPROVED),
                            criteriaBuilder.equal(root.get("status"), AccountStatus.LEAVING)
                        )
                    );
                }
                else if(status.contains("COMPLETED")){
                    predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("status"), AccountStatus.COMPLETED)
                        )
                    );
                }
            }
            
            // Filter by endorsement type
            if (endorsementType != null) {
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.equal(root.get("endorsementType"), endorsementType)
                );
            }
            
            return predicate;
        };
    }

    /**
     * Count all endorsements for a given organization
     * 
     * @param organizationId The organization ID
     * @return Specification for counting all endorsements by organization ID
     */
    public static Specification<Endorsement> countByOrganizationId(UUID organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("organization").get("organizationId"), organizationId);
        };
    }

    /**
     * Count completed endorsements for a given organization
     * Completed endorsements are those with status ACTIVE or INACTIVE
     * 
     * @param organizationId The organization ID
     * @return Specification for counting completed endorsements by organization ID
     */
    public static Specification<Endorsement> countCompletedByOrganizationId(UUID organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get("organization").get("organizationId"), organizationId),
                criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("status"), AccountStatus.COMPLETED)                )
            );
        };
    }

    /**
     * Count pending endorsements for a given organization
     * Pending endorsements are those with status PENDING_APPROVAL or PENDING_EXIT
     * 
     * @param organizationId The organization ID
     * @return Specification for counting pending endorsements by organization ID
     */
    public static Specification<Endorsement> countPendingByOrganizationId(UUID organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get("organization").get("organizationId"), organizationId),
                criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("status"), AccountStatus.PENDING_APPROVAL),
                    criteriaBuilder.equal(root.get("status"), AccountStatus.PENDING_EXIT)
                )
            );
        };
    }

    /**
     * Builds a Specification for filtering endorsements by organization IDs
     * 
     * @param organizationIds List of organization IDs to filter by
     * @return Specification for filtering by organization IDs
     */
    public static Specification<Endorsement> byOrganizationIds(List<UUID> organizationIds) {
        return (root, query, criteriaBuilder) -> {
            if (organizationIds == null || organizationIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            if (organizationIds.size() == 1) {
                return criteriaBuilder.equal(root.get("organization").get("organizationId"), organizationIds.get(0));
            }
            return root.get("organization").get("organizationId").in(organizationIds);
        };
    }

    /**
     * Builds a Specification for filtering endorsements by date range on createdAt field
     * 
     * @param startDate Start date (inclusive), null means no lower bound
     * @param endDate End date (inclusive), null means no upper bound
     * @return Specification for filtering by createdAt date range
     */
    public static Specification<Endorsement> byCreatedAtDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (startDate != null) {
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate)
                );
            }
            if (endDate != null) {
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate)
                );
            }
            return predicate;
        };
    }

    /**
     * Builds a Specification for filtering endorsements by date range on updatedAt field
     * 
     * @param startDate Start date (inclusive), null means no lower bound
     * @param endDate End date (inclusive), null means no upper bound
     * @return Specification for filtering by updatedAt date range
     */
    public static Specification<Endorsement> byUpdatedAtDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (startDate != null) {
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), startDate)
                );
            }
            if (endDate != null) {
                predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), endDate)
                );
            }
            return predicate;
        };
    }

    /**
     * Builds a Specification for filtering endorsements by status
     * 
     * @param status The status to filter by
     * @return Specification for filtering by status
     */
    public static Specification<Endorsement> byStatus(AccountStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /* 
     * Count pending endorsements for given organizations
     * Pending endorsements are those with status PENDING_APPROVAL or PENDING_EXIT
     * If organizationIds is provided, filters by those organizations and pending status
     * If organizationIds is null/empty, returns all pending endorsements (no organization filter)
     * 
     * @param organizationIds List of organization IDs (can be null or empty)
     * @return Specification for counting pending endorsements
     */
    public static Specification<Endorsement> countPendingByOrganizationIds(List<UUID> organizationIds) {
        return (root, query, criteriaBuilder) -> {
            
            // Filter by pending status (PENDING_APPROVAL or PENDING_EXIT)
            Predicate pendingStatusPredicate = criteriaBuilder.or(
                criteriaBuilder.equal(root.get("status"), AccountStatus.PENDING_APPROVAL),
                criteriaBuilder.equal(root.get("status"), AccountStatus.PENDING_EXIT)
            );
            
            // If no organization IDs provided, return only pending status filter
            if(organizationIds == null || organizationIds.isEmpty()) {
                return pendingStatusPredicate;
            }
            
            // Filter by organization IDs
            Predicate orgPredicate;
            if(organizationIds.size() == 1) {
                orgPredicate = criteriaBuilder.equal(root.get("organization").get("organizationId"), organizationIds.get(0));
            } else {
                orgPredicate = root.get("organization").get("organizationId").in(organizationIds);
            }
            
            // Combine organization filter with pending status filter
            return criteriaBuilder.and(orgPredicate, pendingStatusPredicate);
            
        };
    }
}

