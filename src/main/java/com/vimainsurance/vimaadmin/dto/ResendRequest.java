package com.vimainsurance.vimaadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for resending a submitted enrollment back to draft and emailing the employee.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendRequest {

    @NotBlank(message = "Notes are required")
    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;
}
