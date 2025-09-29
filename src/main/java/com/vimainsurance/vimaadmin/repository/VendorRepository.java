package com.vimainsurance.vimaadmin.repository;

import com.vimainsurance.vimaadmin.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Integer> {
    Optional<Vendor> findByNameAndActiveTrue(String name);
} 