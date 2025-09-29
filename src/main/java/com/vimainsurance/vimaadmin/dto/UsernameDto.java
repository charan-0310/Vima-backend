package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UsernameDto {
    private UUID id;
    private String username;
    private String email;
} 