package com.vimainsurance.vimaadmin.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

import com.vimainsurance.vimaadmin.entity.Claim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Custom implementation: list claims with organization and employee fetch-joined
 * to avoid N+1 when mapping to list DTOs.
 */
@Repository
@RequiredArgsConstructor
public class IClaimRepositoryCustomImpl implements IClaimRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Page<Claim> findAllWithOrganizationAndEmployee(Specification<Claim> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Claim> query = cb.createQuery(Claim.class);
        Root<Claim> root = query.from(Claim.class);
        root.fetch("organization", JoinType.LEFT);
        root.fetch("employee", JoinType.LEFT);
        query.distinct(true);

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));
        TypedQuery<Claim> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<Claim> content = typedQuery.getResultList();

        long total = countWithSpec(spec);
        return new PageImpl<>(content, pageable, total);
    }

    private long countWithSpec(Specification<Claim> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Claim> root = countQuery.from(Claim.class);
        countQuery.select(cb.count(root));
        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, countQuery, cb);
            if (predicate != null) {
                countQuery.where(predicate);
            }
        }
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
