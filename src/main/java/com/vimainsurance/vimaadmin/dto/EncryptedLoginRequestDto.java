package com.vimainsurance.vimaadmin.dto;

public class EncryptedLoginRequestDto {
    private String hashedUsername;
    private String encryptedPassword;
    private String challenge;
    private String recaptchaToken;

    public EncryptedLoginRequestDto() {}

    public EncryptedLoginRequestDto(String hashedUsername, String encryptedPassword, String challenge, String recaptchaToken) {
        this.hashedUsername = hashedUsername;
        this.encryptedPassword = encryptedPassword;
        this.challenge = challenge;
        this.recaptchaToken = recaptchaToken;
    }

    public String getHashedUsername() {
        return hashedUsername;
    }

    public void setHashedUsername(String hashedUsername) {
        this.hashedUsername = hashedUsername;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getRecaptchaToken() {
        return recaptchaToken;
    }

    public void setRecaptchaToken(String recaptchaToken) {
        this.recaptchaToken = recaptchaToken;
    }
} 