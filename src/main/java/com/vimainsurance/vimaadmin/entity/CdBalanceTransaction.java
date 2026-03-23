package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.vimainsurance.vimaadmin.enums.CdTransactionSource;
import com.vimainsurance.vimaadmin.enums.CdTransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "cd_balance_transactions", schema = "cpc")
@Data
@EqualsAndHashCode(callSuper = false)
public class CdBalanceTransaction {

    @Id
    @GeneratedValue
    @Column(name = "transaction_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endorsement_id")
    private Endorsement endorsement;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "transaction_type", nullable = false)
    private CdTransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "running_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal runningBalance;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "source", nullable = false)
    private CdTransactionSource source;

    @Column(name = "performed_by", length = 255)
    private String performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
