package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for validate-employees API (no window created, no employees persisted).
 * Optional {@code dependentsByEmployeeId}: parent employee ID → dependents (same shape as enrollment dependents JSON rows).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateEmployeesRequestDto {

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @Valid
    private List<SelfEmployeeEnrollmentRequestDto> selfEmployeeEnrollmentRequestDtos;

    /**
     * Dependents grouped by primary employee ID (corporate employee number). Keys are employee IDs; values match
     * HR enrollment / submission dependent objects (relationship, dateOfBirth, etc.).
     */
    private Map<String, List<DependentEnrollmentUpdateDto>> dependentsByEmployeeId;
}
