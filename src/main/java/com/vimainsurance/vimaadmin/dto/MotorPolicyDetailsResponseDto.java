package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class MotorPolicyDetailsResponseDto {
    private UUID motorPolicyId;
    private String vehicleRegistrationNumber;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleType;
    private Integer manufacturingYear;
    private LocalDate registrationDate;
    private BigDecimal idvValue;

    public UUID getMotorPolicyId() {
        return motorPolicyId;
    }

    public void setMotorPolicyId(UUID motorPolicyId) {
        this.motorPolicyId = motorPolicyId;
    }

    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    public void setVehicleRegistrationNumber(String vehicleRegistrationNumber) {
        this.vehicleRegistrationNumber = vehicleRegistrationNumber;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public void setVehicleMake(String vehicleMake) {
        this.vehicleMake = vehicleMake;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Integer getManufacturingYear() {
        return manufacturingYear;
    }

    public void setManufacturingYear(Integer manufacturingYear) {
        this.manufacturingYear = manufacturingYear;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public BigDecimal getIdvValue() {
        return idvValue;
    }

    public void setIdvValue(BigDecimal idvValue) {
        this.idvValue = idvValue;
    }
}

