package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Customer;

public interface ICustomerService {
    ResponseEntity<ResponseDto<String>> create(CustomerRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> update(CustomerRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> delete(CustomerRequestDto requestDto);
    ResponseEntity<ResponseDto<List<Customer>>> findByAgent(String username);
    ResponseEntity<ResponseDto<List<Customer>>> getAllCustomers(int page, int rec);

}
