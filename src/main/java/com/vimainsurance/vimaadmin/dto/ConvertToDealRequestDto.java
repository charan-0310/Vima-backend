package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ConvertToDealRequestDto {
    private MultipartFile[] document;
    private String policyNumber;
    private String insuranceCompany;
    private BigDecimal sumInsured;
    private BigDecimal premiumAmount;
    private LocalDate policyStartDate;
    private LocalDate policyEndDate;
    private LocalDate renewalDate;
    private String policyStatus;
    private String notes;
    private String productType;
    private String providerCode;
}
