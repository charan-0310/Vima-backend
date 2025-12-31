package com.vimainsurance.vimaadmin.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for admin.feature_flag_companies
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "feature_flag_companies", schema = "admin")
public class FeatureFlagCompany {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flag_id")
    private FeatureFlag featureFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", columnDefinition = "UUID", nullable = false)
    private Organization organization;

    @Column(name = "actions", columnDefinition = "permission_action[]")
    private String[] actions;

}

