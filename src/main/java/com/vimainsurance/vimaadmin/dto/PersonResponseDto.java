package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonResponseDto {

    private UUID id;
    private String name;
    private Integer age;
    private String city;

}
