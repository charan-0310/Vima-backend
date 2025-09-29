package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerBulkDeleteRequestDto {
    private List<String> customerIds;
    private String username;
} 