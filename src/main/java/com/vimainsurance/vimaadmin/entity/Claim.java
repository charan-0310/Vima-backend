package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.vimainsurance.vimaadmin.enums.ClaimCategory;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.ClaimType;
import com.vimainsurance.vimaadmin.enums.MemberType;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.enums.Relationship;
import com.vimainsurance.vimaadmin.enums.SubmissionSource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claims", schema = "claims")
public class Claim {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "claim_number", nullable = false, unique = true, length = 100)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, referencedColumnName = "organization_id")
    private Organization organization;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, referencedColumnName = "individual_id")
    private Deals employee;

    @Column(name = "member_id", length = 100)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", length = 50)
    private MemberType memberType;

    @Column(name = "member_name", length = 255)
    private String memberName;

    @Column(name = "member_dob")
    private LocalDate memberDob;

    @Column(name = "member_uhid", length = 100)
    private String memberUhid;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", length = 50)
    private Relationship relationship;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", length = 100)
    private ClaimType claimType;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_category", length = 100)
    private ClaimCategory claimCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", length = 100)
    private ProductType productType;

    @Column(name = "reason_for_admission", columnDefinition = "TEXT")
    private String reasonForAdmission;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "claim_amount", precision = 18, scale = 2)
    private BigDecimal claimAmount;

    @Column(name = "hospital_name", length = 255)
    private String hospitalName;

    @Column(name = "hospital_city", length = 100)
    private String hospitalCity;

    @Column(name = "hospital_state", length = 100)
    private String hospitalState;

    @Column(name = "hospital_pincode", length = 20)
    private String hospitalPincode;

    @Column(name = "hospital_provider_code", length = 100)
    private String hospitalProviderCode;

    @Column(name = "is_network_hospital")
    private Boolean isNetworkHospital;

    @Column(name = "date_of_admission")
    private LocalDate dateOfAdmission;

    @Column(name = "date_of_discharge")
    private LocalDate dateOfDischarge;

    @Column(name = "date_of_submission")
    private LocalDate dateOfSubmission;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "account_holder_name", length = 255)
    private String accountHolderName;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "bank_branch_name", length = 255)
    private String bankBranchName;

    @Column(name = "insurer_id")
    private UUID insurerId;

    @Column(name = "insurer_claim_ref", length = 100)
    private String insurerClaimRef;

    @Column(name = "insurer_claim_number", length = 100)
    private String insurerClaimNumber;

    @Column(name = "insurer_inward_number", length = 100)
    private String insurerInwardNumber;

    @Column(name = "insurer_status", length = 100)
    private String insurerStatus;

    @Column(name = "insurer_current_status", length = 100)
    private String insurerCurrentStatus;

    @Column(name = "insurer_remarks", columnDefinition = "TEXT")
    private String insurerRemarks;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "internal_status", nullable = false, length = 50)
    private ClaimStatus internalStatus = ClaimStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_source", length = 100)
    private SubmissionSource submissionSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_claim_id", referencedColumnName = "id")
    private Claim parentClaim;

    @Column(name = "parent_insurer_claim_ref", length = 100)
    private String parentInsurerClaimRef;

    @Column(name = "abha_id", length = 100)
    private String abhaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", referencedColumnName = "id")
    private AdminUser submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", referencedColumnName = "id")
    private AdminUser reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", referencedColumnName = "id")
    private AdminUser approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Version
    @Column(name = "version")
    private Integer version = 1;

    @OneToOne(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ClaimSettlement settlement;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClaimQuery> queries = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClaimDeduction> deductions = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClaimAuditLog> auditLogs = new ArrayList<>();

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
