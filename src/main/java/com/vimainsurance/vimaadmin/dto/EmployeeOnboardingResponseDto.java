package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.Data;

@Data
public class EmployeeOnboardingResponseDto {

    private List<String> successUsers;
    private List<String> failedUsers;

}
