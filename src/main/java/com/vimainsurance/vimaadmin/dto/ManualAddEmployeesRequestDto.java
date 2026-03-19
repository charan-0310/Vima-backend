package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for manual-add employees API.
 * Inserts employees directly into the Employees (Deals) table without file parsing or storage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAddEmployeesRequestDto {

    @Valid
    @NotEmpty(message = "At least one employee is required")
    private List<EmployeeUploadDto> employees;

    @NotEmpty(message = "At least one policy must be selected")
    private List<Long> policyIds;
}
