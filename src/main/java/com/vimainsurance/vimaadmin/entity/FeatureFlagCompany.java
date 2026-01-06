package com.vimainsurance.vimaadmin.entity;

import java.util.UUID;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

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

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "actions", columnDefinition = "permission_action[]")
    @ColumnTransformer(write = "?::permission_action[]")
    private String[] actions;
}

