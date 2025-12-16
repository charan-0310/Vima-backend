package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.ConfirmationMethod;
import com.vimainsurance.vimaadmin.enums.EndorsementType;
import com.vimainsurance.vimaadmin.enums.PremiumChangeType;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "endorsements", schema = "cpc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Endorsement {

    @Id
    @GeneratedValue
    @Column(name = "endorsement_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID endorsementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "endorsement_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EndorsementType endorsementType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountStatus status = AccountStatus.PENDING_APPROVAL;

    @Column(name = "total_employees", nullable = false)
    private Integer totalEmployees = 0;

    @Column(name = "total_dependents", nullable = false)
    private Integer totalDependents = 0;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", referencedColumnName = "id")
    private AdminUser approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", referencedColumnName = "id")
    private AdminUser uploadedBy;

    @Column(name = "confirmation_method")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ConfirmationMethod confirmationMethod;

    @Column(name = "insurer_ref_number", length = 100)
    private String insurerRefNumber;

    @Column(name = "premium_change_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PremiumChangeType premiumChangeType;

    @Column(name = "premium_amount", precision = 15, scale = 2)
    private BigDecimal premiumAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}

