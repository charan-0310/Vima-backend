package com.vimainsurance.vimaadmin.dto;

public class ChallengeRequestDto {
    private String hashedUsername;

    public ChallengeRequestDto() {}

    public ChallengeRequestDto(String hashedUsername) {
        this.hashedUsername = hashedUsername;
    }

    public String getHashedUsername() {
        return hashedUsername;
    }

    public void setHashedUsername(String hashedUsername) {
        this.hashedUsername = hashedUsername;
    }
} 