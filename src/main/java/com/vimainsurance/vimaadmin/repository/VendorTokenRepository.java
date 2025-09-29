package com.vimainsurance.vimaadmin.repository;

import com.vimainsurance.vimaadmin.entity.VendorToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface VendorTokenRepository extends JpaRepository<VendorToken, Integer> {
    Optional<VendorToken> findTopByVendorIdAndExpiresAtAfterOrderByExpiresAtDesc(Integer vendorId, LocalDateTime now);
} 