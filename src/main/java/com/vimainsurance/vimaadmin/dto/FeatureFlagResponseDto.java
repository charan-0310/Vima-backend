package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagResponseDto {

    @JsonProperty("flag_id")
    private String flagId;

    @JsonProperty("flag_key")
    private String flagKey;

    private String description;

    @JsonProperty("is_active")
    private Boolean isActive;

    private List<String> actions;

    private List<CompanyDto> companies;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyDto {
        private String id;

        @JsonProperty("flag_id")
        private String flagId;

        @JsonProperty("organization_id")
        private String organizationId;

        @JsonProperty("organization_name")
        private String organizationName;

        private List<String> actions;
    }
}
