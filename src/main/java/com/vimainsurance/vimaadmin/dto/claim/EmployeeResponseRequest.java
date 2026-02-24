package com.vimainsurance.vimaadmin.dto.claim;

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
public class EmployeeResponseRequest {
    private String remarks;
    private List<UUID> documentIds;
}
