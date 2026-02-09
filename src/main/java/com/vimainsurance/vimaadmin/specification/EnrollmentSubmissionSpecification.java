package com.vimainsurance.vimaadmin.specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

import jakarta.persistence.criteria.Predicate;

public class EnrollmentSubmissionSpecification {

    /**
     * Builds a Specification for filtering enrollment submissions by organization, status, search, and date range.
     * Organization filter is via employee.organization (submission -> employee -> organization).
     */
    public static Specification<EnrollmentSubmission> withFilters(
            List<UUID> organizationIds,
            String status,
            String search,
            LocalDateTime submittedFrom,
            LocalDateTime submittedTo) {

        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            if (organizationIds != null && !organizationIds.isEmpty()) {
                if (organizationIds.size() == 1) {
                    predicate = criteriaBuilder.and(
                            predicate,
                            criteriaBuilder.equal(
                                    root.get("employee").get("organization").get("organizationId"),
                                    organizationIds.get(0)));
                } else {
                    predicate = criteriaBuilder.and(
                            predicate,
                            root.get("employee").get("organization").get("organizationId").in(organizationIds));
                }
            }

            if (status != null && !status.trim().isEmpty()) {
                try {
                    EnrollementStatus statusEnum = EnrollementStatus.fromValue(status.trim().toUpperCase());
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException ignored) {
                    // invalid status ignored
                }
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchTerm = "%" + search.trim().toLowerCase() + "%";
                Predicate refPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("referenceNumber")),
                        searchTerm);
                Predicate empNamePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("employee").get("fullName")),
                        searchTerm);
                Predicate empEmailPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("employee").get("email")),
                        searchTerm);
                Predicate empNumberPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("employee").get("employeeNumber")),
                        searchTerm);
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.or(refPredicate, empNamePredicate, empEmailPredicate, empNumberPredicate));
            }

            if (submittedFrom != null) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("submittedAt"), submittedFrom));
            }

            if (submittedTo != null) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.lessThanOrEqualTo(root.get("submittedAt"), submittedTo));
            }

            return predicate;
        };
    }
}
