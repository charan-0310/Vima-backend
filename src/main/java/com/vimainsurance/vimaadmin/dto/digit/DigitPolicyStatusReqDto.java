package com.vimainsurance.vimaadmin.dto.digit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DigitPolicyStatusReqDto {
private String applicationId;
private String policyNumber;
}
