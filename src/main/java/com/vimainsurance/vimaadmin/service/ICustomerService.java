package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;

import com.vimainsurance.vimaadmin.dto.CustomerBulkDeleteRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerPipelineRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.ConvertToDealRequestDto;

public interface ICustomerService {
    ResponseEntity<ResponseDto<String>> create(CustomerRequestDto requestDto, String username);
    ResponseEntity<ResponseDto<String>> update(CustomerRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> delete(CustomerRequestDto requestDto);
    ResponseEntity<ResponseDto<List<CustomerResponseDto>>> findByAgent(
        String username, String search, int page, int rec, String sortBy, String sortDirection);
    ResponseEntity<ResponseDto<CustomerResponseDto>> getByCustId(String custId);
    ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getAllCustomers(String search, int page, int rec, String sortBy, String sortDirection);
    ResponseEntity<ResponseDto<String>> updatePipelineStatus(String username, String customerId, CustomerPipelineRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> bulkDelete(CustomerBulkDeleteRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> uploadDocument(DocumentRequestDto requestDto, String customerId);
    ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(String customerId);
    ResponseEntity<Resource> downloadDocument(String documentId);
    ResponseEntity<ResponseDto<String>> deleteDocument(String documentId);
    ResponseEntity<ResponseDto<String>> customerToDeals(ConvertToDealRequestDto requestDto, String custId);
}
