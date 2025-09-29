package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.Data;

@Data
public class QuickQuoteRequestDto {
    private String dateOfBirth;
    private String dobDay;
    private String dobMonth;
    private String dobYear;
    private String gender;
    private boolean isSmoker;
    private boolean isAlcoholic;
    private String coverageEndAge;
    private List<Long> sumAssured;
    private String paymentFrequency;
    private String name;
    private String occupation;
    private String incomeRange;
    private String incomeProofFlag;
    private String highestEducation;
    private String state;
    private String city;
    private String pincode;
    private String mobileNumber;
} 