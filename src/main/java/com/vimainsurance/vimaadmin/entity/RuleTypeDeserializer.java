package com.vimainsurance.vimaadmin.entity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class RuleTypeDeserializer extends JsonDeserializer<IncentiveRule.RuleType> {
    @Override
    public IncentiveRule.RuleType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        return IncentiveRule.RuleType.valueOf(value.toUpperCase());
    }
} 