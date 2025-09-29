package com.vimainsurance.vimaadmin.dto;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class ManagerDashboardResponseDto {

    List<String> agentNames;
    Integer totalPolicyIssued;
}
