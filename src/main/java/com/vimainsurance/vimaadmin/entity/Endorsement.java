package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.javers.core.metamodel.annotation.DiffIgnore;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.ConfirmationMethod;
import com.vimainsurance.vimaadmin.enums.EndorsementSource;
import com.vimainsurance.vimaadmin.enums.EndorsementType;
import com.vimainsurance.vimaadmin.enums.PremiumChangeType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

    // Endorsement source tracking (added in V13.1)
    @Column(name = "source")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EndorsementSource source = EndorsementSource.CSV_UPLOAD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_window_id")
    private EnrollmentWindows enrollmentWindow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_endorsement_id")
    private Endorsement parentEndorsement;

    @Column(name = "split_group_id")
    private UUID splitGroupId;

    @Column(name = "submission_count")
    private Integer submissionCount = 0;

    @Column(name = "total_employees", nullable = false)
    private Integer totalEmployees = 0;

    @Column(name = "total_dependents", nullable = false)
    private Integer totalDependents = 0;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

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

    /** Life event type for mid-year endorsements (e.g. MARRIAGE, BIRTH, ADOPTION, DIVORCE, DEATH). */
    @Column(name = "life_event_type", length = 50)
    private String lifeEventType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Many-to-Many relationship with Deals through DealEndorsement join table
    // Note: No orphanRemoval to preserve endorsement history - relationships are managed manually
    @DiffIgnore
    @JsonManagedReference
    @OneToMany(mappedBy = "endorsement", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<DealEndorsement> dealEndorsements = new ArrayList<>();
}

