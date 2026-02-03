package com.vimainsurance.vimaadmin.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class PersonRequestDto {

    private String name;

    @NotNull(message = "Age is required")
    private Integer age;

    private String city;

}
