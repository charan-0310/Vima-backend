package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_catalog", schema = "cpc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCatalog {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "product_type", nullable = false, length = 50)
    private String productType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = false;

    @Column(name = "pricing_model", length = 30)
    private String pricingModel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coverage_options", columnDefinition = "jsonb")
    private String coverageOptions;

    /** JSON object map: {"sumInsured":"premium"} for TOP_UP/SUPER_TOP_UP. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "premium_preview_options", columnDefinition = "jsonb")
    private String premiumPreviewOptions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "covered_relationships", columnDefinition = "jsonb")
    private String coveredRelationships;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "policy_id")
    private Long policyId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grade_filter", columnDefinition = "jsonb")
    private String gradeFilter;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
