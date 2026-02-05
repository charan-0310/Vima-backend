package com.vimainsurance.vimaadmin.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class SendInvitationRequestDto {

    private UUID employeeId;
    private UUID windowId;
}
