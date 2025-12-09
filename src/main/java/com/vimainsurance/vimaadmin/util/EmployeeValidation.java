package com.vimainsurance.vimaadmin.util;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.EmployeeUploadDto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmployeeValidation {

    @Autowired
    private  Validator validator;


    public  boolean validateEmployee(List<EmployeeUploadDto> employeeUploadDtoList) {
        for (EmployeeUploadDto employeeUploadDto : employeeUploadDtoList) {
            Set<ConstraintViolation<EmployeeUploadDto>> violations =
            validator.validate(employeeUploadDto);            
            if (!violations.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
