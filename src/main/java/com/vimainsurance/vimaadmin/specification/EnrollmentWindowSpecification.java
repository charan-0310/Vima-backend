package com.vimainsurance.vimaadmin.specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

import jakarta.persistence.criteria.Predicate;

public class EnrollmentWindowSpecification {

    /**
     * Builds a Specification for filtering enrollment windows (excludes soft-deleted / CANCELLED by default).
     */
    public static Specification<EnrollmentWindows> withFilters(
            List<UUID> organizationIds,
            String status,
            String name,
            LocalDate fromDate,
            LocalDate toDate,
            boolean excludeDeleted) {

        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            if (organizationIds != null && !organizationIds.isEmpty()) {
                if (organizationIds.size() == 1) {
                    predicate = criteriaBuilder.and(
                            predicate,
                            criteriaBuilder.equal(root.get("organization").get("organizationId"), organizationIds.get(0)));
                } else {
                    predicate = criteriaBuilder.and(
                            predicate,
                            root.get("organization").get("organizationId").in(organizationIds));
                }
            }

            if (excludeDeleted) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.notEqual(root.get("status"), EnrollementStatus.CANCELLED));
            }

            if (status != null && !status.trim().isEmpty()) {
                try {
                    EnrollementStatus statusEnum = EnrollementStatus.fromValue(status.trim().toUpperCase());
                    predicate = criteriaBuilder.and(
                            predicate,
                            criteriaBuilder.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException ignored) {
                    // invalid status ignored
                }
            }

            if (name != null && !name.trim().isEmpty()) {
                String searchName = "%" + name.trim().toLowerCase() + "%";
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                searchName));
            }

            if (fromDate != null) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), fromDate));
            }

            if (toDate != null) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), toDate));
            }

            return predicate;
        };
    }
}
