package com.vimainsurance.vimaadmin.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.vimainsurance.vimaadmin.entity.Claim;

/**
 * Custom claim repository methods for performance (fetch joins to avoid N+1).
 */
public interface IClaimRepositoryCustom {

    /**
     * Find claims matching the specification with organization and employee
     * fetch-joined in one query to avoid N+1 when mapping to list DTOs.
     */
    Page<Claim> findAllWithOrganizationAndEmployee(Specification<Claim> spec, Pageable pageable);
}
