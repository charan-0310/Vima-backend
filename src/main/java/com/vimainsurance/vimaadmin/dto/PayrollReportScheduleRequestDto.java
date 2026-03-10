package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollReportScheduleRequestDto {

    @NotNull
    private UUID companyId;
    private UUID windowId;
    @Builder.Default
    private String frequency = "MONTHLY";
    private List<String> recipientEmails;
}
