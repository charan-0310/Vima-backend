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
public class FeatureFlagResponseDto {

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

    @JsonProperty("sub_features")
    private List<FeatureFlagResponseDto> subFeatures;

    @JsonProperty("companies")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<CompanyDto> companies;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompanyDto {
        @JsonProperty("id")
        private String id;

        @JsonProperty("flag_id")
        private String flagId;

        @JsonProperty("organization_id")
        private String organizationId;

        @JsonProperty("organization_name")
        private String organizationName;

        @JsonProperty("actions")
        private List<String> actions;
    }
}
