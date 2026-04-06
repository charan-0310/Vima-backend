package com.vimainsurance.vimaadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One dependent row for HR PATCH enrollment (synced to submission dependents JSON).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependentEnrollmentUpdateDto {

    private String firstName;
    private String lastName;
    /** If set, used with or without first/last */
    private String fullName;

    @NotBlank(message = "Dependent relationship is required")
    @Size(max = 50)
    private String relationship;

    @Size(max = 50)
    private String actualRelationship;

    @NotBlank(message = "Dependent date of birth is required")
    private String dateOfBirth;

    @Size(max = 20)
    private String gender;

    private String dateOfJoining;

    private Boolean prefilledByHr;
    private Boolean alreadyCoveredElsewhere;
}
