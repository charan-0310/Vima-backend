package com.vimainsurance.vimaadmin.dto;

public class PublicKeyResponseDto {
    private String publicKey;
    private String algorithm;
    private int keySize;

    public PublicKeyResponseDto() {}

    public PublicKeyResponseDto(String publicKey, String algorithm, int keySize) {
        this.publicKey = publicKey;
        this.algorithm = algorithm;
        this.keySize = keySize;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }
} 