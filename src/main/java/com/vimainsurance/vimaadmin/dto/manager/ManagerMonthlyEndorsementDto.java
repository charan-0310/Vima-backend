package com.vimainsurance.vimaadmin.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerMonthlyEndorsementDto {
    private String month;
    private long additions;
    private long deletions;
    private long net;
}
