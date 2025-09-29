package com.vimainsurance.vimaadmin.repository;

import com.vimainsurance.vimaadmin.entity.VendorApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VendorApiEndpointRepository extends JpaRepository<VendorApiEndpoint, Integer> {
    Optional<VendorApiEndpoint> findByVendorIdAndApiKeyAndActiveTrue(Integer vendorId, String apiKey);
} 