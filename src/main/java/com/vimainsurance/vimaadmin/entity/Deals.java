package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.javers.core.metamodel.annotation.DiffIgnore;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

@Table(name = "customers", schema = "cpc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Deals {

    @Id
    @GeneratedValue
    @Column(name = "individual_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID individualId;

    // Personal Information
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "full_name", length = 255, unique = true)
    private String fullName;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    // Identity
    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;

    // Address
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 6)
    private String pincode;

    // Account Type and Status
    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountType accountType = AccountType.RETAIL_PRIMARY;

    @DiffIgnore
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountStatus status = AccountStatus.ACTIVE;

    // Self-enrollment tracking (added in V13.1)
    @Convert(disableConversion = true)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "enrollment_status")
    private EnrollementStatus enrollmentStatus = EnrollementStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_window_id")
    private EnrollmentWindows enrollmentWindow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_submission_id")
    private EnrollmentSubmission enrollmentSubmission;

    // Future B2B fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "employee_number", length = 50)
    private String employeeNumber;

    // Family Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_individual_id")
    private Deals primaryIndividual;

    @Column(name = "relationship", length = 50)
    private String relationship;

    /** Original relationship label (e.g. Son, Daughter) when relationship is CHILD1–CHILD4; for display/reporting. */
    @Column(name = "actual_relationship", length = 50)
    private String actualRelationship;

    @Column(name = "is_primary_member")
    private Boolean isPrimaryMember = true;

    @Column(name = "designation", length = 50)
    private String designation;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "reason_for_exit", length = 100)
    private String reasonForExit;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "date_of_exit")
    private LocalDate dateOfExit;

    // Portal Access (only for primary members)
    @Column(name = "username", length = 100, unique = true)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "en";

    // Origin
    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "cust_id", length = 50)
    private String custId;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "sum_insured")
    private String sumInsured;

    @Column(name = "ctc", precision = 15, scale = 2)
    private BigDecimal ctc;

    @Column(name = "endorsement_id")
    private UUID endorsementId;

    @Column(name = "health_id", length = 100)
    private String healthId;

    /** Stable UUID string for wellness partner JWT {@code user_identifier} (e.g. MantraCare). */
    @Column(name = "wellness_user_id", length = 36)
    private String wellnessUserId;

    // Many-to-Many relationship with Endorsement through DealEndorsement join table
    // Note: No orphanRemoval to preserve endorsement history - relationships are managed manually
    @DiffIgnore
    @JsonManagedReference
    @OneToMany(mappedBy = "deal", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<DealEndorsement> dealEndorsements = new ArrayList<>();

}
