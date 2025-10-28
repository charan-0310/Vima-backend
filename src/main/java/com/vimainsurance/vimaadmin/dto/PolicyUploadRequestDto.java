package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class PolicyUploadRequestDto {
    private MultipartFile[] files;
    private String documentType;
    private String notes;
    private UUID individualId;
    
    // Policy Details
    private String policyNumber;
    private String insuranceCompany;
    private String productType;
    private String providerCode;
    private String coverageType;
    private String status;
    private BigDecimal sumInsured;
    private BigDecimal premiumAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate renewalDate;
    private List<DealsRequestDto> dependents;
}
