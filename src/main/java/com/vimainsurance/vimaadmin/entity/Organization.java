package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vimainsurance.vimaadmin.enums.Industry;

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

    @Column(name = "primary_contact_name", length = 20)
    private String primaryContactName;

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

    @Column(name = "registered_address", length = 250, nullable = false)
    private String registeredAddress;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "industry")
    private Industry industry;
}
