package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.vimainsurance.vimaadmin.enums.QueryStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claim_queries", schema = "claims")
public class ClaimQuery {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false, referencedColumnName = "id")
    private Claim claim;

    @Column(name = "insurer_sys_id", length = 100)
    private String insurerSysId;

    @Column(name = "query_text", columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "query_date")
    private LocalDate queryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_status", nullable = false, length = 50)
    private QueryStatus queryStatus = QueryStatus.OPEN;

    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;

    @Column(name = "response_date")
    private LocalDate responseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by", referencedColumnName = "id")
    private AdminUser respondedBy;

    @Column(name = "response_remark", columnDefinition = "TEXT")
    private String responseRemark;

    @Column(name = "courier_name", length = 255)
    private String courierName;

    @Column(name = "pod_number", length = 100)
    private String podNumber;

    @Column(name = "num_documents_attached")
    private Integer numDocumentsAttached;

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
