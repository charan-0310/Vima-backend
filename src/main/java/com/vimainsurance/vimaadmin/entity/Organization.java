package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "organizations", schema = "cpc")
public class Organization {

    @Id
    @GeneratedValue
    @Column(name = "organization_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID organizationId;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "primary_contact_email")
    private String primaryContactEmail;

    @Column(name = "primary_contact_phone", length = 20)
    private String primaryContactPhone;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
