package com.vimainsurance.vimaadmin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vimainsurance.vimaadmin.enums.ProductType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Insurance Provider entity
 */
@Entity
@Table(name = "insurance_providers", schema = "admin")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceProvider {

    @Id
    @GeneratedValue
    @Column(name = "provider_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID providerId;

    @Column(name = "provider_name", length = 255, nullable = false)
    private String providerName;

    @Column(name = "provider_code", length = 50, unique = true, nullable = false)
    private String providerCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "product_type", nullable = false)
    private ProductType productType = ProductType.HEALTH;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
