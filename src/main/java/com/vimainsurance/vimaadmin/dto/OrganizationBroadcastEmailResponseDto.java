package com.vimainsurance.vimaadmin.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationBroadcastEmailResponseDto {
    private int totalRecipients;
    private int sentCount;
    private int failedCount;
    private List<OrganizationBroadcastFailedRecipientDto> failedRecipients = new ArrayList<>();
}
