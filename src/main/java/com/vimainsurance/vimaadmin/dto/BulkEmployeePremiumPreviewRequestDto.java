package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for premium preview in Bulk Upload / Manual Add review UI.
 * Premium is computed server-side; request does not accept any premium numbers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmployeePremiumPreviewRequestDto {

    /** Selected policy IDs for this upload (base policies + optional top-up policies if chosen). */
    @NotEmpty(message = "policyIds is required")
    private List<Long> policyIds;

    /**
     * Primary employee (SELF) individual UUID. When set, policyIds are intersected with active
     * employee_policy_map rows for this employee and family (endorsement / employee list review).
     */
    private UUID primaryIndividualId;

    /**
     * Employee group rows for a single employeeId (Self + dependents if provided in upload table).
     * Uses EmployeeUploadDto fields including optional cover SI columns.
     */
    @NotEmpty(message = "employees is required")
    private List<EmployeeUploadDto> employees;
}

