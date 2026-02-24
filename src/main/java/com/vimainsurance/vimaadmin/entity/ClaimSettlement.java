package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claim_settlement", schema = "claims")
public class ClaimSettlement {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false, unique = true, referencedColumnName = "id")
    private Claim claim;

    @Column(name = "claimed_amount", precision = 18, scale = 2)
    private BigDecimal claimedAmount;

    @Column(name = "gross_sanctioned_amount", precision = 18, scale = 2)
    private BigDecimal grossSanctionedAmount;

    @Column(name = "net_sanctioned_amount", precision = 18, scale = 2)
    private BigDecimal netSanctionedAmount;

    @Column(name = "total_disallowed_amount", precision = 18, scale = 2)
    private BigDecimal totalDisallowedAmount;

    @Column(name = "deduction_amount", precision = 18, scale = 2)
    private BigDecimal deductionAmount;

    @Column(name = "copay_amount", precision = 18, scale = 2)
    private BigDecimal copayAmount;

    @Column(name = "amount_paid", precision = 18, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_mode", length = 50)
    private String paymentMode;

    @Column(name = "cheque_number", length = 100)
    private String chequeNumber;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
