package com.vimainsurance.vimaadmin.repository;

import com.vimainsurance.vimaadmin.entity.InsuranceProvider;
import com.vimainsurance.vimaadmin.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for InsuranceProvider entity
 */
@Repository
public interface IInsuranceProviderRepository extends JpaRepository<InsuranceProvider, UUID> {

    /**
     * Find insurance provider by provider code
     */
    Optional<InsuranceProvider> findByProviderCode(String providerCode);

    /**
     * Find insurance providers by product type
     */
    List<InsuranceProvider> findByProductType(ProductType productType);

    /**
     * Find active insurance providers
     */
    List<InsuranceProvider> findByIsActiveTrue();

    /**
     * Find active insurance providers by product type
     */
    List<InsuranceProvider> findByIsActiveTrueAndProductType(ProductType productType);

    /**
     * Check if provider code exists
     */
    boolean existsByProviderCode(String providerCode);

    /**
     * Find insurance providers by name containing (case insensitive)
     */
    @Query("SELECT ip FROM InsuranceProvider ip WHERE LOWER(ip.providerName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<InsuranceProvider> findByProviderNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find active insurance providers by name containing (case insensitive)
     */
    @Query("SELECT ip FROM InsuranceProvider ip WHERE ip.isActive = true AND LOWER(ip.providerName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<InsuranceProvider> findActiveByProviderNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Count active providers by product type
     */
    long countByIsActiveTrueAndProductType(ProductType productType);

    /**
     * Count total active providers
     */
    long countByIsActiveTrue();
}
