package com.vimainsurance.vimaadmin.specification;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.vimainsurance.vimaadmin.entity.CdBalanceTransaction;
import com.vimainsurance.vimaadmin.enums.CdTransactionType;

import jakarta.persistence.criteria.Predicate;

public final class CdBalanceTransactionSpecification {

    private CdBalanceTransactionSpecification() {
    }

    public static Specification<CdBalanceTransaction> ledgerByPolicy(
            Long policyId,
            CdTransactionType type,
            LocalDateTime from,
            LocalDateTime to) {
        return (root, query, cb) -> {
            Predicate predicate = cb.equal(root.get("policy").get("policyId"), policyId);
            if (type != null) {
                predicate = cb.and(predicate, cb.equal(root.get("transactionType"), type));
            }
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return predicate;
        };
    }
}
