package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.vimainsurance.vimaadmin.entity.Quotes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerResponseDto {
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

    private String zohoCrmId;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<Quotes> quotes; 
}
