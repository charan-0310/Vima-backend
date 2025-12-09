package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for bulk employee deletion request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmployeeDeletionRequestDto {
    private List<String> employeeIds;
}




