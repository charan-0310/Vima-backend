package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDateTime;
import java.util.*;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for admin.feature_flags
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "feature_flags", schema = "admin")
public class FeatureFlag {

    @Id
    @Column(name = "flag_id", columnDefinition = "UUID")
    private UUID flagId;

    @Column(name = "flag_key", length = 150, unique = true, nullable = false)
    private String flagKey;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_flag_id", referencedColumnName = "flag_id", columnDefinition = "UUID")
    private FeatureFlag parentFeatureFlag;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "featureFlag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeatureFlagRole> roles = new ArrayList<>();

    @OneToMany(mappedBy = "featureFlag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeatureFlagCompany> companies = new ArrayList<>();

}

