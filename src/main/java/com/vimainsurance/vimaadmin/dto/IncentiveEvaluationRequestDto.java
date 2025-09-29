package com.vimainsurance.vimaadmin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public class IncentiveEvaluationRequestDto {
    @NotNull(message = "Package ID is required")
    private Long packageId;
    
    @DecimalMin(value = "0.0", message = "Policy count must be non-negative")
    private BigDecimal policyCount;
    
    @DecimalMin(value = "0.0", message = "Premium value must be non-negative")
    private BigDecimal premiumValue;
    
    @DecimalMin(value = "0.0", message = "Other value must be non-negative")
    private BigDecimal otherValue;

    public IncentiveEvaluationRequestDto() {}

    public IncentiveEvaluationRequestDto(Long packageId, BigDecimal policyCount, BigDecimal premiumValue, BigDecimal otherValue) {
        this.packageId = packageId;
        this.policyCount = policyCount;
        this.premiumValue = premiumValue;
        this.otherValue = otherValue;
    }

    // Getters and setters
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public BigDecimal getPolicyCount() { return policyCount; }
    public void setPolicyCount(BigDecimal policyCount) { this.policyCount = policyCount; }
    public BigDecimal getPremiumValue() { return premiumValue; }
    public void setPremiumValue(BigDecimal premiumValue) { this.premiumValue = premiumValue; }
    public BigDecimal getOtherValue() { return otherValue; }
    public void setOtherValue(BigDecimal otherValue) { this.otherValue = otherValue; }
} 