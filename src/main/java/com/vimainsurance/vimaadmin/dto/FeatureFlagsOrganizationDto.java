package com.vimainsurance.vimaadmin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureFlagsOrganizationDto {

    @JsonProperty("flag_id")
    private String flagId;

    @JsonProperty("flag_key")
    private String flagKey;

    @JsonProperty("description")
    private String description;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("is_enabled")
    private Boolean isEnabled;

    @JsonProperty("actions")
    private List<String> actions;

}
