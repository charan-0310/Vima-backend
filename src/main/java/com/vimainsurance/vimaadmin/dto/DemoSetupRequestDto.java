package com.vimainsurance.vimaadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DemoSetupRequestDto {

    @NotBlank(message = "Demo org name is required")
    private String orgName;

    @NotBlank(message = "Client email is required")
    @Email(message = "Invalid email format")
    private String clientEmail;

    private boolean forceNew = false;
}
