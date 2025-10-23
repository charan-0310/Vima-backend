package com.vimainsurance.vimaadmin.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vimainsurance.vimaadmin.entity.Deals;

public interface IDealsRepository extends JpaRepository<Deals, UUID> {

}
