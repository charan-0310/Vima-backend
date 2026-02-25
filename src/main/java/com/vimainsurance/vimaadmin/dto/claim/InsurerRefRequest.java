package com.vimainsurance.vimaadmin.dto.claim;

import java.util.UUID;

import lombok.Data;

@Data
public class InsurerRefRequest {

    private UUID insurerId;
    private String insurerClaimRef;
    private String insurerClaimNumber;
    private String insurerInwardNumber;
    private String insurerStatus;
    private String insurerCurrentStatus;
    private String insurerRemarks;
}
