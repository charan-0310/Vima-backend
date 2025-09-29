package com.vimainsurance.vimaadmin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.entity.IncentivePackage;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IncentivePackageDto {
    private Long id;
    private String name;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<IncentiveRuleDto> rules;

    public IncentivePackageDto(IncentivePackage entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        if (entity.getRules() != null) {
            this.rules = entity.getRules().stream().map(IncentiveRuleDto::new).collect(Collectors.toList());
        }
    }

    // Getters and setters
    // ...
} 