package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vimainsurance.vimaadmin.entity.Quotes;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerResponseDto {

    private String id;

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
    
    @JsonIgnore
    private String zohoCrmId;

    private String status;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<Quotes> quotes;

    private List<DocumentResponseDto> documents;

    private String owner;
    
    // Additional fields for manager dashboard
    private Integer quotesCount;
    private BigDecimal totalPremium;
    private BigDecimal totalCoverage;
    private BigDecimal premiumAmount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerResponseDto that = (CustomerResponseDto) o;
        // Use either custId or zohoCrmId for equality
        if (custId != null && that.custId != null) {
            return custId.equals(that.custId);
        }
        if (zohoCrmId != null && that.zohoCrmId != null) {
            return zohoCrmId.equals(that.zohoCrmId);
        }
        // If neither ID is available, use phone number as it's unique
        return phoneNumber != null && phoneNumber.equals(that.phoneNumber);
    }

    @Override
    public int hashCode() {
        // Use the same fields as in equals
        if (custId != null) return custId.hashCode();
        if (zohoCrmId != null) return zohoCrmId.hashCode();
        return phoneNumber != null ? phoneNumber.hashCode() : 0;
    }
}
