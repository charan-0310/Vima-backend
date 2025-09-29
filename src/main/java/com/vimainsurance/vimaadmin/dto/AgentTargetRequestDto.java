package com.vimainsurance.vimaadmin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class AgentTargetRequestDto {
    private List<String> agentUsernames;
    private Integer month;
    private Integer year;
    private Long packageId;
} 