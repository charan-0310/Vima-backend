package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.EmployeeInsuranceResponseDto;

import java.util.UUID;

/**
 * Service interface for employee insurance operations
 */
public interface IEmployeeInsuranceService {

    /**
     * Get employee insurance details including policy and covered members (dependents)
     *
     * @param employeeId the employee's individual ID
     * @return EmployeeInsuranceResponseDto containing employee, policy, and dependents data
     */
    EmployeeInsuranceResponseDto getEmployeeInsuranceDetails(UUID employeeId);
}
