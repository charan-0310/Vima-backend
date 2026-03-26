package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.vimainsurance.vimaadmin.enums.CdAccountStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "cd_accounts", schema = "cpc")
@Data
@EqualsAndHashCode(callSuper = false)
public class CdAccount {

    @Id
    @GeneratedValue
    @Column(name = "cd_account_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID cdAccountId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "insurer_name", nullable = false, length = 255)
    private String insurerName;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "cd_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal cdBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CdAccountStatus status = CdAccountStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
