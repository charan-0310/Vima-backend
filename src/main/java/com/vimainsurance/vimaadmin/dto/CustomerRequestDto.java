package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CustomerRequestDto {
    
    private String custId;

    private String fullName;

    private LocalDate dateOfBirth;

    private String gender;

    private String phoneNumber;

    private String email;

    private String city;

    private String state;

    private String occupation;

    private BigDecimal annualIncome;

    private Integer dependentCount;

    private String status;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
