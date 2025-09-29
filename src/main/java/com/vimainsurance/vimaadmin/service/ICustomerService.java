package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.CustomerBulkDeleteRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerPipelineRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface ICustomerService {
    ResponseEntity<ResponseDto<String>> create(CustomerRequestDto requestDto, String username);
    ResponseEntity<ResponseDto<String>> update(CustomerRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> delete(CustomerRequestDto requestDto);
    ResponseEntity<ResponseDto<List<CustomerResponseDto>>> findByAgent(
        String username, String search, int page, int rec, String sortBy, String sortDirection);
    ResponseEntity<ResponseDto<CustomerResponseDto>> getByCustId(String custId);
    ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getAllCustomers(int page, int rec);
    ResponseEntity<ResponseDto<String>> updatePipelineStatus(String username, String customerId, CustomerPipelineRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> bulkDelete(CustomerBulkDeleteRequestDto requestDto);
}
