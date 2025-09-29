package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncentivePackageFullRequestDto {
    @NotBlank(message = "Package name is required")
    @Size(max = 255, message = "Package name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private List<IncentiveRuleFullRequestDto> rules;
} 