package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class MotorPolicyDetailsRequestDto {
    private String vehicleRegistrationNumber;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleType;
    private Integer manufacturingYear;
    private LocalDate registrationDate;
    private BigDecimal idvValue;
}

