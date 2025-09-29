package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "incentive_rules", schema = "admin")
public class IncentiveRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private IncentivePackage packageEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", columnDefinition = "admin.rule_type_enum", nullable = false)
    private RuleType ruleType;

    @Column(name = "fixed_payout")
    private BigDecimal fixedPayout;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IncentiveRuleSlab> slabs;

    public enum RuleType {
        SLAB_BASED,
        FIXED_PER_POLICY,
        PREMIUM_BASED
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public IncentivePackage getPackageEntity() { return packageEntity; }
    public void setPackageEntity(IncentivePackage packageEntity) { this.packageEntity = packageEntity; }
    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }
    public BigDecimal getFixedPayout() { return fixedPayout; }
    public void setFixedPayout(BigDecimal fixedPayout) { this.fixedPayout = fixedPayout; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public List<IncentiveRuleSlab> getSlabs() { return slabs; }
    public void setSlabs(List<IncentiveRuleSlab> slabs) { this.slabs = slabs; }
} 