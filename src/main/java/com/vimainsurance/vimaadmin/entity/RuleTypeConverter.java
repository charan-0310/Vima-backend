package com.vimainsurance.vimaadmin.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RuleTypeConverter implements AttributeConverter<IncentiveRule.RuleType, String> {
    @Override
    public String convertToDatabaseColumn(IncentiveRule.RuleType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public IncentiveRule.RuleType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : IncentiveRule.RuleType.valueOf(dbData.toUpperCase());
    }
} 