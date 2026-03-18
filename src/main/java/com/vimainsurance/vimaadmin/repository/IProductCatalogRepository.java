package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.ProductCatalog;

@Repository
public interface IProductCatalogRepository
        extends JpaRepository<ProductCatalog, UUID>, JpaSpecificationExecutor<ProductCatalog> {

    List<ProductCatalog> findByOrganizationId(UUID organizationId);

    List<ProductCatalog> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);

    Optional<ProductCatalog> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ProductCatalog> findByOrganizationIdOrderByDisplayOrderAsc(UUID organizationId);

    @Modifying
    @Query(value = "UPDATE cpc.product_catalog SET display_order = :displayOrder, updated_at = NOW() WHERE id = :id", nativeQuery = true)
    int updateDisplayOrder(@Param("id") UUID id, @Param("displayOrder") Integer displayOrder);
}
