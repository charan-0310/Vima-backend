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
                            criteriaBuilder.equal(root.get("status"), AccountStatus.ACTIVE),
                            criteriaBuilder.equal(root.get("status"), AccountStatus.INACTIVE)
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
}

