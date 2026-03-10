package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "company_enrollment_config", schema = "cpc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyEnrollmentConfig {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, unique = true)
    private UUID organizationId;

    @Column(name = "parent_coverage_enabled", nullable = false)
    @Builder.Default
    private Boolean parentCoverageEnabled = false;

    @Column(name = "in_law_coverage_enabled", nullable = false)
    @Builder.Default
    private Boolean inLawCoverageEnabled = false;

    @Column(name = "max_parents", nullable = false)
    @Builder.Default
    private Integer maxParents = 0;

    @Column(name = "max_in_laws", nullable = false)
    @Builder.Default
    private Integer maxInLaws = 0;

    @Column(name = "parent_age_limit")
    private Integer parentAgeLimit;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
