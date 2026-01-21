package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.javers.core.metamodel.annotation.DiffIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Join table entity for many-to-many relationship between Deals and Endorsement
 * Represents the association between a Deal (individual) and an Endorsement
 */
@Table(name = "deal_endorsements", schema = "cpc", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"individual_id", "endorsement_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@DiffIgnore
public class DealEndorsement {

    @Id
    @GeneratedValue
    @Column(name = "deal_endorsement_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID dealEndorsementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "individual_id", nullable = false)
    private Deals deal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endorsement_id", nullable = false)
    private Endorsement endorsement;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}

