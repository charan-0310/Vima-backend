package com.vimainsurance.vimaadmin.entity;

import java.util.*;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;


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

   @Column(name = "is_active")
    private Boolean isActive = false;


    @Column(name = "actions", columnDefinition = "permission_action[]")
    @ColumnTransformer(write = "?::permission_action[]")
    private String[] actions;

}

