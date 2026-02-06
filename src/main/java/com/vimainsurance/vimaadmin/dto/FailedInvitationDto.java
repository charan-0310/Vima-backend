package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Details for a single failed invitation (bulk send). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedInvitationDto {

    /** Employee number (e.g. EMP001) for display and reference. */
    private String employeeNumber;
    private String employeeName;
    private String error;
}
