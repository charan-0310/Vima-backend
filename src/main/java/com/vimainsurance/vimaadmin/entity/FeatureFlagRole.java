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
 * JPA entity for admin.feature_flag_roles
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "feature_flag_roles", schema = "admin")
public class FeatureFlagRole {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flag_id")
    private FeatureFlag featureFlag;

    @Column(name = "role_name", length = 50, nullable = false)
    private String roleName;

    /**
     * Postgres enum array column. Use columnDefinition so Hibernate doesn't try to map to a non-existent SQL type.
     * We'll map this to String[] in the entity and let Hibernate pass-through.
     */
    @Column(name = "actions", columnDefinition = "permission_action[]")
    private String[] actions;

}

