package com.vimainsurance.vimaadmin.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AuthentikGroupCreationDto {
    private String name;

    @JsonProperty(value = "is_superuser")
    private Boolean isSuperUser = false;

    private String parent =null;

    private Map<String, String> attributes;
}
