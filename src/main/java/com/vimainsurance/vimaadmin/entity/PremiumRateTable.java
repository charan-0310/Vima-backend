package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import com.vimainsurance.vimaadmin.enums.PricingModel;
import com.vimainsurance.vimaadmin.enums.RateSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "premium_rate_tables", schema = "cpc")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumRateTable {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "product_type", nullable = false, length = 50)
    private String productType;

    @Column(name = "member_type", length = 50)
    private String memberType;

    @Column(name = "age_band_min")
    private Integer ageBandMin;

    @Column(name = "age_band_max")
    private Integer ageBandMax;

    @Column(name = "rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", length = 30)
    private PricingModel pricingModel;

    @Column(name = "sum_insured_amount", precision = 15, scale = 2)
    private BigDecimal sumInsuredAmount;

    @Column(name = "family_size_min")
    private Integer familySizeMin;

    @Column(name = "family_size_max")
    private Integer familySizeMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_source", length = 50)
    private RateSource rateSource;

    @Column(name = "gst_inclusive", nullable = false)
    @Builder.Default
    private Boolean gstInclusive = false;

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
