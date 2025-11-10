package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class NomineeRequestDto {
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String relationship;
    private BigDecimal nomineePercentage;
    private Boolean isActive;
}

