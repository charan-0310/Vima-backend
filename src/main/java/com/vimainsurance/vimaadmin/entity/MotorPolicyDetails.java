package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "motor_policy_details", schema = "cpc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MotorPolicyDetails {

    @Id
    @GeneratedValue
    @Column(name = "motor_policy_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID motorPolicyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnore
    private Policy policy;

    @Column(name = "vehicle_registration_number", length = 20, nullable = false)
    private String vehicleRegistrationNumber;

    @Column(name = "vehicle_make", length = 50)
    private String vehicleMake;

    @Column(name = "vehicle_model", length = 50)
    private String vehicleModel;

    @Column(name = "vehicle_type", length = 20)
    private String vehicleType;

    @Column(name = "manufacturing_year")
    private Integer manufacturingYear;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "idv_value", precision = 12, scale = 2)
    private BigDecimal idvValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

