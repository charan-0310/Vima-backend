package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureFlagUpdateItemDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("actions")
    private List<String> actions;
}
