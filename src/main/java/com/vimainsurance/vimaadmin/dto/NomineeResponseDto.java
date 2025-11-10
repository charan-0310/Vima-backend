package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Data;

@Data
public class NomineeResponseDto {
    private UUID nomineeId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String relationship;
}

