package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for manual-delete employees API.
 * Soft-deletes (sets status to Inactive) employees by their employee IDs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualDeleteEmployeesRequestDto {

    @NotEmpty(message = "At least one employee ID is required")
    private List<String> employeeIds;
}
