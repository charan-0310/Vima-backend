package com.vimainsurance.vimaadmin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
 
@Data
@NoArgsConstructor
public class GenericRequestDto {
    private Map<String, Object> payload;
} 