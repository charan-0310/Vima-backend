package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Customer;
import com.vimainsurance.vimaadmin.service.ICustomerService;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private ICustomerService iCustomerService;

    @PostMapping("/agent/{username}/customer")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<String>> create(@RequestBody CustomerRequestDto requestDto, @PathVariable String username){
        logger.info("[correlationId:{}] /agent/{}/customer endpoint called", MDC.get("correlationId"), username);
        return iCustomerService.create(requestDto);
    }

    @PutMapping("/customer")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<String>> update(@RequestBody CustomerRequestDto requestDto){
        logger.info("[correlationId:{}] /customer (PUT) endpoint called", MDC.get("correlationId"));
        return iCustomerService.update(requestDto);
    }

    @DeleteMapping("/customer")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<String>> delete(@RequestBody CustomerRequestDto requestDto){
        logger.info("[correlationId:{}] /customer (DELETE) endpoint called", MDC.get("correlationId"));
        return iCustomerService.delete(requestDto);
    }

    @GetMapping("/agent/{username}/customers")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getCustomerByAgent(@PathVariable String username){
        logger.info("[correlationId:{}] /agent/{}/customers endpoint called", MDC.get("correlationId"), username);
        return iCustomerService.findByAgent(username);
    }

    @GetMapping("/customers/{custid}")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<CustomerResponseDto>> getCustomerById(@PathVariable String custid){
        logger.info("[correlationId:{}] /customers/{} endpoint called", MDC.get("correlationId"), custid);
        return iCustomerService.getByCustId(custid);
    }

}
