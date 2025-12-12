package com.vimainsurance.vimaadmin.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

public class ConverterUtils {

    private static final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.ALWAYS); // Keep nulls

    public static Map<String, Object> convertToMap(Object obj) {
        Map<String, Object> rawMap = mapper.convertValue(obj, new TypeReference<>() {});
        return (Map<String, Object>) sanitize(rawMap);
    }

    // Recursively replaces null with "", keeps booleans and numbers as-is
    private static Object sanitize(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(entry.getKey().toString(), sanitize(entry.getValue()));
            }
            return result;
        } else if (obj instanceof List<?> list) {
            List<Object> newList = new ArrayList<>();
            for (Object item : list) {
                newList.add(sanitize(item));
            }
            return newList;
        } else if (obj == null) {
            return ""; // Replace nulls with empty string
        } else {
            return obj; // Keep existing values
        }
    }

}
