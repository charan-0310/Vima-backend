package com.vimainsurance.vimaadmin.dto.claim;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponseRequest {
    private String responseText;
    private LocalDate responseDate;
    private String courierName;
    private String podNumber;
    private Integer numDocumentsAttached;
}
