package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "enrollment_invitations", schema = "cpc")
public class EnrollmentInvitation {

    @Id
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_window_id", nullable = false)
    private EnrollmentWindows enrollmentWindow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, referencedColumnName = "individual_id")
    private Deals employee;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    /**
     * Backed by Postgres named enum: cpc.endorsement_status_enum
     * Values: pending, sent, opened, in_progress, completed, expired
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EnrollementStatus status = EnrollementStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "reminder_count")
    @Builder.Default
    private Integer reminderCount = 0;

    @Column(name = "last_reminder_at")
    private LocalDateTime lastReminderAt;

    /**
     * When true, token is derived from invitation id; reminder emails resend the same link (valid until window end).
     * When false or null (legacy), reminder generates a new token.
     */
    @Column(name = "token_deterministic")
    private Boolean tokenDeterministic;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
