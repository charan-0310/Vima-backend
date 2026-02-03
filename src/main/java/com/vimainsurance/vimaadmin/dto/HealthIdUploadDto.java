package com.vimainsurance.vimaadmin.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthIdUploadDto {
    private String employeeId;
    private String relationship;
    private String name;
    private LocalDate dateOfBirth;
    private String gender;
    private String email;
    private String mobile;
    private LocalDate dateOfJoining;
    private String designation;
    private String department;
    private String maritalStatus;
    private String sumInsured;
    private String healthId;
    /** Specific failure reason when record fails validation (populated in error response only) */
    private String errorReason;
}