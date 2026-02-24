package com.vimainsurance.vimaadmin.dto.claim;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.QueryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimQueryDto {
    private UUID id;
    private String insurerSysId;
    private String queryText;
    private LocalDate queryDate;
    private QueryStatus queryStatus;
    private String responseText;
    private LocalDate responseDate;
    private String responseRemark;
    private String courierName;
    private String podNumber;
    private Integer numDocumentsAttached;
    private String employeeRemarks;
    private LocalDateTime employeeResponseAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
