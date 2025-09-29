package com.vimainsurance.vimaadmin.repository;

import com.vimainsurance.vimaadmin.entity.VendorApiHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VendorApiHeaderRepository extends JpaRepository<VendorApiHeader, Integer> {
    List<VendorApiHeader> findByEndpointId(Integer endpointId);
} 