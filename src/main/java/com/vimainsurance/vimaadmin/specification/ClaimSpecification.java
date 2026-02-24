package com.vimainsurance.vimaadmin.specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.ClaimType;

import jakarta.persistence.criteria.Predicate;

public final class ClaimSpecification {

    private ClaimSpecification() {
    }

    public static Specification<Claim> withFilters(ClaimListFilters filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filters == null) {
                predicates.add(cb.equal(root.get("isDeleted"), false));
                return cb.and(predicates.toArray(new Predicate[0]));
            }
            if (filters.getOrganizationIds() != null && !filters.getOrganizationIds().isEmpty()) {
                predicates.add(root.get("organization").get("organizationId").in(filters.getOrganizationIds()));
            } else if (filters.getOrganizationId() != null) {
                predicates.add(cb.equal(root.get("organization").get("organizationId"), filters.getOrganizationId()));
            }
            if (filters.getEmployeeId() != null) {
                predicates.add(cb.equal(root.get("employee").get("individualId"), filters.getEmployeeId()));
            }
            if (filters.getPolicyId() != null) {
                predicates.add(cb.equal(root.get("policyId"), filters.getPolicyId()));
            }
            if (filters.getInternalStatusIn() != null && !filters.getInternalStatusIn().isEmpty()) {
                predicates.add(root.get("internalStatus").in(filters.getInternalStatusIn()));
            } else if (filters.getInternalStatus() != null) {
                predicates.add(cb.equal(root.get("internalStatus"), filters.getInternalStatus()));
            }
            if (filters.getClaimType() != null) {
                predicates.add(cb.equal(root.get("claimType"), filters.getClaimType()));
            }
            if (filters.getClaimNumber() != null && !filters.getClaimNumber().isBlank()) {
                predicates.add(cb.equal(root.get("claimNumber"), filters.getClaimNumber()));
            }
            if (filters.getSearch() != null && !filters.getSearch().isBlank()) {
                String search = "%" + filters.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("claimNumber")), search),
                        cb.like(cb.lower(root.get("memberName")), search)));
            }
            if (filters.getDateOfSubmissionFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfSubmission"), filters.getDateOfSubmissionFrom()));
            }
            if (filters.getDateOfSubmissionTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfSubmission"), filters.getDateOfSubmissionTo()));
            }
            if (filters.getInsurerId() != null) {
                predicates.add(cb.equal(root.get("insurerId"), filters.getInsurerId()));
            }
            if (filters.getInsurerClaimRef() != null && !filters.getInsurerClaimRef().isBlank()) {
                predicates.add(cb.equal(root.get("insurerClaimRef"), filters.getInsurerClaimRef()));
            }
            if (filters.getIsDeleted() != null) {
                predicates.add(cb.equal(root.get("isDeleted"), filters.getIsDeleted()));
            } else {
                predicates.add(cb.equal(root.get("isDeleted"), false));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Same as withFilters but optionally restrict to one status (for summary counts). */
    public static Specification<Claim> withFiltersAndStatus(ClaimListFilters filters, ClaimStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filters == null) {
                predicates.add(cb.equal(root.get("isDeleted"), false));
                if (status != null) predicates.add(cb.equal(root.get("internalStatus"), status));
                return cb.and(predicates.toArray(new Predicate[0]));
            }
            if (filters.getOrganizationIds() != null && !filters.getOrganizationIds().isEmpty()) {
                predicates.add(root.get("organization").get("organizationId").in(filters.getOrganizationIds()));
            } else if (filters.getOrganizationId() != null) {
                predicates.add(cb.equal(root.get("organization").get("organizationId"), filters.getOrganizationId()));
            }
            if (filters.getEmployeeId() != null) {
                predicates.add(cb.equal(root.get("employee").get("individualId"), filters.getEmployeeId()));
            }
            if (filters.getPolicyId() != null) {
                predicates.add(cb.equal(root.get("policyId"), filters.getPolicyId()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("internalStatus"), status));
            } else if (filters.getInternalStatus() != null) {
                predicates.add(cb.equal(root.get("internalStatus"), filters.getInternalStatus()));
            }
            if (filters.getClaimType() != null) {
                predicates.add(cb.equal(root.get("claimType"), filters.getClaimType()));
            }
            if (filters.getClaimNumber() != null && !filters.getClaimNumber().isBlank()) {
                predicates.add(cb.equal(root.get("claimNumber"), filters.getClaimNumber()));
            }
            if (filters.getSearch() != null && !filters.getSearch().isBlank()) {
                String search = "%" + filters.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("claimNumber")), search),
                        cb.like(cb.lower(root.get("memberName")), search)));
            }
            if (filters.getDateOfSubmissionFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfSubmission"), filters.getDateOfSubmissionFrom()));
            }
            if (filters.getDateOfSubmissionTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfSubmission"), filters.getDateOfSubmissionTo()));
            }
            if (filters.getInsurerId() != null) {
                predicates.add(cb.equal(root.get("insurerId"), filters.getInsurerId()));
            }
            if (filters.getInsurerClaimRef() != null && !filters.getInsurerClaimRef().isBlank()) {
                predicates.add(cb.equal(root.get("insurerClaimRef"), filters.getInsurerClaimRef()));
            }
            if (filters.getIsDeleted() != null) {
                predicates.add(cb.equal(root.get("isDeleted"), filters.getIsDeleted()));
            } else {
                predicates.add(cb.equal(root.get("isDeleted"), false));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Same as withFilters but restrict to internalStatus IN (statuses). Used for summary "rejected"
     * count (REJECTED + REJECTED_BY_ADMIN + INTIMATION_REJECTED). Ensures DB enum comparison is
     * consistent and all rejected-type statuses are included.
     */
    public static Specification<Claim> withFiltersAndStatusIn(ClaimListFilters filters, List<ClaimStatus> statuses) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filters == null) {
                predicates.add(cb.equal(root.get("isDeleted"), false));
                if (statuses != null && !statuses.isEmpty()) {
                    predicates.add(root.get("internalStatus").in(statuses));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            }
            if (filters.getOrganizationIds() != null && !filters.getOrganizationIds().isEmpty()) {
                predicates.add(root.get("organization").get("organizationId").in(filters.getOrganizationIds()));
            } else if (filters.getOrganizationId() != null) {
                predicates.add(cb.equal(root.get("organization").get("organizationId"), filters.getOrganizationId()));
            }
            if (filters.getEmployeeId() != null) {
                predicates.add(cb.equal(root.get("employee").get("individualId"), filters.getEmployeeId()));
            }
            if (filters.getPolicyId() != null) {
                predicates.add(cb.equal(root.get("policyId"), filters.getPolicyId()));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("internalStatus").in(statuses));
            } else if (filters.getInternalStatus() != null) {
                predicates.add(cb.equal(root.get("internalStatus"), filters.getInternalStatus()));
            }
            if (filters.getClaimType() != null) {
                predicates.add(cb.equal(root.get("claimType"), filters.getClaimType()));
            }
            if (filters.getClaimNumber() != null && !filters.getClaimNumber().isBlank()) {
                predicates.add(cb.equal(root.get("claimNumber"), filters.getClaimNumber()));
            }
            if (filters.getSearch() != null && !filters.getSearch().isBlank()) {
                String search = "%" + filters.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("claimNumber")), search),
                        cb.like(cb.lower(root.get("memberName")), search)));
            }
            if (filters.getDateOfSubmissionFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfSubmission"), filters.getDateOfSubmissionFrom()));
            }
            if (filters.getDateOfSubmissionTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfSubmission"), filters.getDateOfSubmissionTo()));
            }
            if (filters.getInsurerId() != null) {
                predicates.add(cb.equal(root.get("insurerId"), filters.getInsurerId()));
            }
            if (filters.getInsurerClaimRef() != null && !filters.getInsurerClaimRef().isBlank()) {
                predicates.add(cb.equal(root.get("insurerClaimRef"), filters.getInsurerClaimRef()));
            }
            if (filters.getIsDeleted() != null) {
                predicates.add(cb.equal(root.get("isDeleted"), filters.getIsDeleted()));
            } else {
                predicates.add(cb.equal(root.get("isDeleted"), false));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
