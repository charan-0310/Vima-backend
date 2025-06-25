package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Customer;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.vimainsurance.vimaadmin.service.ICustomerService;
import com.vimainsurance.vimaadmin.service.IZohoCRMService;
import com.vimainsurance.vimaadmin.util.Constants;

@Service
public class CustomerServiceImpl implements ICustomerService{

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Autowired
    private ICustomerRepository customerRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private IZohoCRMService zohoCRMService;
   


    @Override
    public ResponseEntity<ResponseDto<String>> create(CustomerRequestDto requestDto) {
        logger.info("[correlationId:{}] create called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Customer> existByPhonenumber = customerRepository.findByPhoneNumber(requestDto.getPhoneNumber());
            if(existByPhonenumber.isPresent()){
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Already Existed"));
            }
            Customer customer = new Customer();
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername("charan-0310");
            customer.setCustId(generateCustomerId());
            customer.setFullName(requestDto.getFullName());
            customer.setDateOfBirth(requestDto.getDateOfBirth());
            customer.setGender(requestDto.getGender());
            customer.setPhoneNumber(requestDto.getPhoneNumber());
            customer.setEmail(requestDto.getEmail());
            customer.setCity(requestDto.getCity());
            customer.setState(requestDto.getState());
            customer.setOccupation(requestDto.getOccupation());
            customer.setAnnualIncome(requestDto.getAnnualIncome());
            customer.setDependentCount(requestDto.getDependentCount());
            customer.setZohoCrmId(requestDto.getZohoCrmId());
            customer.setStatus(requestDto.getStatus());
            customer.setCreatedAt(requestDto.getCreatedAt());
            customer.setUpdatedAt(requestDto.getUpdatedAt());
            customer.setCreatedBy(adminUser.get());
            customer.setOwner(adminUser.get());
            customerRepository.save(customer);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.SAVE_SUCCESS));
        } catch (Exception e) {
            logger.error("Exception in create", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> update(CustomerRequestDto requestDto) {
        logger.info("[correlationId:{}] update called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Customer> existCustomer = customerRepository.findByCustId(requestDto.getCustId());
            if(existCustomer.isPresent()){
                Customer customer = existCustomer.get();
                customer.setFullName(requestDto.getFullName());
                customer.setDateOfBirth(requestDto.getDateOfBirth());
                customer.setGender(requestDto.getGender());
                customer.setPhoneNumber(requestDto.getPhoneNumber());
                customer.setEmail(requestDto.getEmail());
                customer.setCity(requestDto.getCity());
                customer.setState(requestDto.getState());
                customer.setOccupation(requestDto.getOccupation());
                customer.setAnnualIncome(requestDto.getAnnualIncome());
                customer.setDependentCount(requestDto.getDependentCount());
                customer.setUpdatedAt(LocalDateTime.now());
                customer.setStatus(requestDto.getStatus());
                customerRepository.save(customer);
            } else {
                return responseObj.render(responseObj.formErrorResponse(Constants.UPDATE_FAILED));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.UPDATE_SUCCESS));
        } catch (Exception e) {
            logger.error("Exception in update", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<ResponseDto<String>> delete(CustomerRequestDto requestDto) {
        logger.info("[correlationId:{}] delete called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Customer> existCustomer = customerRepository.findByCustId(requestDto.getCustId());
            if(existCustomer.isPresent()){
                Customer customer = existCustomer.get();
                customer.setStatus("INACTIVE");
                customerRepository.save(customer);
            }
            else{
                return responseObj.render(responseObj.formErrorResponse(Constants.DELETE_FAILED));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.DELETE_MESSAGE));
        } catch (Exception e) {
            logger.error("Exception in delete", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }


    public String generateCustomerId() {
        String maxId = customerRepository.findMaxCustomerId(); 
        if (maxId == null) {
            return "C001";
        }
        int num = Integer.parseInt(maxId.substring(1));
        num++;
        return String.format("C%03d", num);
    }

    @Override
    public ResponseEntity<ResponseDto<List<CustomerResponseDto>>> findByAgent(String username) {
        logger.info("[correlationId:{}] findByAgent called", MDC.get("correlationId"));
        BaseResponse<List<CustomerResponseDto>> responseObj = new BaseResponse<>();
        try {
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);
            List<Customer> customerList = customerRepository.findByCreatedBy(adminUser.get());
            LinkedHashSet<CustomerResponseDto> customerResponseSet = new LinkedHashSet<>();
            if(customerList.isEmpty()){
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new ArrayList<>(), 0));
            }
            
            for(Customer customer : customerList){
                CustomerResponseDto responseDto = new CustomerResponseDto();
                responseDto.setCustId(customer.getCustId());
                responseDto.setFullName(customer.getFullName());
                responseDto.setDateOfBirth(customer.getDateOfBirth());
                responseDto.setGender(customer.getGender());
                responseDto.setPhoneNumber(customer.getPhoneNumber());
                responseDto.setEmail(customer.getEmail());
                responseDto.setCity(customer.getCity());
                responseDto.setState(customer.getState());
                responseDto.setOccupation(customer.getOccupation());
                responseDto.setAnnualIncome(customer.getAnnualIncome());
                responseDto.setDependentCount(customer.getDependentCount());
                responseDto.setUpdatedAt(LocalDateTime.now());
                responseDto.setStatus(customer.getStatus());
                responseDto.setQuotes(customer.getQuotes());
                responseDto.setOwner(adminUser.get().getUsername());
                customerResponseSet.add(responseDto);
            }
            
            // List<CustomerResponseDto> customerResponseDtos = (List<CustomerResponseDto>) zohoCRMService.getAllLeadsByAgents(1, 20, "884155000000351000").getBody().getPayload();
            // if (customerResponseDtos != null) {
            //     customerResponseSet.addAll(customerResponseDtos);
            // }
            
            List<CustomerResponseDto> uniqueList = new ArrayList<>(customerResponseSet);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, uniqueList, uniqueList.size()));
        } catch (Exception e) {
            logger.error("Exception in findByAgent", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<Customer>>> getAllCustomers(int page, int rec) {
        logger.info("[correlationId:{}] getAllCustomers called", MDC.get("correlationId"));
        BaseResponse<List<Customer>> responseObj = new BaseResponse<>();
        try {
            if (page == -1 && rec == -1) {
                List<Customer> customerList = customerRepository.findAll();
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, customerList, customerList.size()));
            } else {
                 Pageable pageable = PageRequest.of(page, rec);
                 Page<Customer> customerPage = customerRepository.findAll(pageable);
                 return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, customerPage.getContent(), customerPage.getTotalPages()));
            }
        } catch (Exception e) {
            logger.error("Exception in getAllCustomers", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<CustomerResponseDto>> getByCustId(String custId) {
        logger.info("[correlationId:{}] getByCustId called", MDC.get("correlationId"));
        BaseResponse<CustomerResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<Customer> optionalCustomer = customerRepository.findByCustId(custId);
            if(optionalCustomer.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            CustomerResponseDto responseDto = new CustomerResponseDto();
            Customer customer = optionalCustomer.get();
            responseDto.setCustId(customer.getCustId());
            responseDto.setFullName(customer.getFullName());
            responseDto.setDateOfBirth(customer.getDateOfBirth());
            responseDto.setGender(customer.getGender());
            responseDto.setPhoneNumber(customer.getPhoneNumber());
            responseDto.setEmail(customer.getEmail());
            responseDto.setCity(customer.getCity());
            responseDto.setState(customer.getState());
            responseDto.setOccupation(customer.getOccupation());
            responseDto.setAnnualIncome(customer.getAnnualIncome());
            responseDto.setDependentCount(customer.getDependentCount());
            responseDto.setUpdatedAt(LocalDateTime.now());
            responseDto.setStatus(customer.getStatus());
            responseDto.setQuotes(customer.getQuotes());
            responseDto.setOwner(customer.getOwner().getUsername());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto,1));
        } catch (Exception e) {
            logger.error("Exception in getByCustId", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
}
