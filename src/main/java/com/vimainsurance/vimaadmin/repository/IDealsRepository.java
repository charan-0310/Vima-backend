package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vimainsurance.vimaadmin.entity.Deals;

public interface IDealsRepository extends JpaRepository<Deals, UUID> {

    /**
     * Find deals by multiple individual IDs (batch query for optimization)
     */
    List<Deals> findByIndividualIdIn(List<UUID> individualIds);

}
