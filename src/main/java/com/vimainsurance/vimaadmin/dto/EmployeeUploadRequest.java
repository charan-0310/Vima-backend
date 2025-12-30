package com.vimainsurance.vimaadmin.dto;
import java.util.List;

import lombok.Data;

@Data
public class EmployeeUploadRequest {

    String uploadType;
    List<EmployeeUploadDto> employeeUploadDtoList;
}
