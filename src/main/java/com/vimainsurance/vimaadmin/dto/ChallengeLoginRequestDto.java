package com.vimainsurance.vimaadmin.dto;

import lombok.Data;

@Data
public class ChallengeLoginRequestDto {
    private String username;
    private String hashedPassword;
    private String clientproof;
    private String recaptchaToken;

    public ChallengeLoginRequestDto() {}
} 