package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureFlagsOrganizationResponse {

    @JsonProperty("type")
    private String type;

    @JsonProperty("identifier")
    private String identifier;

    @JsonProperty("organization_id")
    private String organizationId;

    @JsonProperty("features")
    private List<FeatureFlagsOrganizationDto> features;
}

