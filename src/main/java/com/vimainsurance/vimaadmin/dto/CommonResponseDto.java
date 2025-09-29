package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResponseDto {
    private Map<String, Object> payload;
    private String message;
    private Integer status;
} 