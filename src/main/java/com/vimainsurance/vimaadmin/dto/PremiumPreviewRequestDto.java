package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPreviewRequestDto {

    private UUID companyId;
    private String planType;
    private BigDecimal sumInsured;
    private String coverageTier;
    private List<MemberAgeDto> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberAgeDto {
        private String memberType;
        private Integer age;
        private LocalDate dateOfBirth;
    }
}
