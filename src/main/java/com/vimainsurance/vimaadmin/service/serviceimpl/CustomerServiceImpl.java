package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.CustomerPipelineRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerBulkDeleteRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Customer;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.vimainsurance.vimaadmin.service.ICustomerService;
import com.vimainsurance.vimaadmin.service.IZohoCRMService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.ConverterUtils;
import com.vimainsurance.vimaadmin.util.IdGenerator;

@Service
public class CustomerServiceImpl implements ICustomerService{

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceImpl.class);
    private static final String DEFAULT_SORT_FIELD = "updatedAt";
    private static final String PREMIUM_SORT_FIELD = "premium";

    @Autowired
    private ICustomerRepository customerRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private IdGenerator customerIdGenerator;

    @Autowired
    private IZohoCRMService zohoCRMService;
   

    @Override
    public ResponseEntity<ResponseDto<String>> create(CustomerRequestDto requestDto, String username) {
        logger.info("[correlationId:{}] create called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Customer> existByPhonenumber = customerRepository.findByPhoneNumber(requestDto.getPhoneNumber());
            if(existByPhonenumber.isPresent()){
                return responseObj.render(responseObj.formErrorResponse( "Already Existed"));
            }
            Customer customer = new Customer();
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);
            if(adminUser.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            customer.setCustId(customerIdGenerator.generateCustomerId());
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
            // customer.setZohoCrmId(requestDto.getZohoCrmId());
            customer.setStatus(requestDto.getStatus());
            customer.setCreatedAt(requestDto.getCreatedAt());
            customer.setUpdatedAt(requestDto.getUpdatedAt());
            customer.setCreatedBy(adminUser.get());
            customer.setOwner(adminUser.get());
            customer.setNotes(requestDto.getNotes());
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
                customer.setNotes(requestDto.getNotes());
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

    @Override
    public ResponseEntity<ResponseDto<String>> updatePipelineStatus(String username, String customerId, CustomerPipelineRequestDto requestDto) {
        Optional<AdminUser> agentOpt = adminUserRepository.findByUsername(username);
        if (agentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseDto<String>("Agent not found", null));
        }

        Optional<Customer> customerOpt = customerRepository.findByCustId(customerId);
        if (customerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseDto<String>("Customer not found", null));
        }

        Customer customer = customerOpt.get();
        AdminUser agent = agentOpt.get();

        // Verify the customer belongs to the agent
        if (
                !customer.getOwner().getId().equals(agent.getId()) && 
                (customer.getOwner().getReportingTo() == null || 
                !customer.getOwner().getReportingTo().getId().equals(agent.getId()))
            ){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<String>("You don't have permission to update this customer", null));
        }

        // Validate the status
        String status = requestDto.getStatus().toUpperCase();
        if (!isValidStatus(status)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<String>("Invalid status. Valid statuses are: NEW_LEAD, PRE_FOLLOW_UP, QUOTE_SENT, POST_FOLLOW_UP, APPLICATION, POLICY_ISSUED, NOT_INTERESTED, LEAD_LOST", null));
        }

        // Update the status
        customer.setStatus(status);
        customer.setUpdatedAt(LocalDateTime.now());
        customerRepository.save(customer);

        return ResponseEntity.ok(new ResponseDto<String>("Pipeline status updated successfully", null));
    }

    private boolean isValidStatus(String status) {
        return List.of(
            "NEW_LEAD",
            "PRE_FOLLOW_UP",
            "QUOTE_SENT",
            "POST_FOLLOW_UP",
            "APPLICATION",
            "POLICY_ISSUED",
            "NOT_INTERESTED",
            "LEAD_LOST"
        ).contains(status);
    }


    @Override
    public ResponseEntity<ResponseDto<List<CustomerResponseDto>>> findByAgent(
            String username, String search, int page, int rec, String sortBy, String sortDirection) {
        logger.info("[correlationId:{}] findByAgent called with filters - sortBy: {}, sortDirection: {}", 
                   MDC.get("correlationId"), sortBy, sortDirection);
        BaseResponse<List<CustomerResponseDto>> responseObj = new BaseResponse<>();
        try {
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);
            if(adminUser.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            // Determine which query method to use based on sorting
            Page<Customer> customerList;
            if (PREMIUM_SORT_FIELD.equalsIgnoreCase(sortBy)) {
                // For premium sorting, we need to handle it separately
                customerList = getCustomersWithPremiumSorting(adminUser.get(), search, page, rec, sortDirection);
            } else if (search != null && !search.trim().isEmpty()) {
                // Use dedicated search method when search is provided
                Sort sort = createSort(sortBy, sortDirection);
                PageRequest pageRequest = PageRequest.of(page, rec, sort);
                customerList = customerRepository.searchCustomersByCreatedBy(adminUser.get(), search, pageRequest);
            } else {
                // Use basic method when no search is applied
                Sort sort = createSort(sortBy, sortDirection);
                PageRequest pageRequest = PageRequest.of(page, rec, sort);
                customerList = customerRepository.findActiveByCreatedBy(adminUser.get(), pageRequest);
            }

            LinkedHashSet<CustomerResponseDto> customerResponseSet = new LinkedHashSet<>();
            if(customerList.isEmpty()){
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new ArrayList<>(), 0));
            }
            
            for(Customer customer : customerList){
                CustomerResponseDto responseDto = new CustomerResponseDto();
                responseDto.setId(customer.getId().toString());
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
                responseDto.setNotes(customer.getNotes());
                responseDto.setQuotes(customer.getQuotes());
                responseDto.setOwner(adminUser.get().getUsername());
                customerResponseSet.add(responseDto);
            }
            
            List<CustomerResponseDto> uniqueList = new ArrayList<>(customerResponseSet);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, uniqueList, customerList.getTotalElements()));
        } catch (Exception e) {
            logger.error("Exception in findByAgent", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
    
    private Sort createSort(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD); // Default sort
        }
        
        // Map frontend field names to entity field names
        String entityField = mapSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        return Sort.by(direction, entityField);
    }
    
    private String mapSortField(String frontendField) {
        return switch (frontendField.toLowerCase()) {
            case "pipelinestage", "pipeline_stage", "status" -> "status";
            case "fullname", "full_name", "name" -> "fullName";
            case "phonenumber", "phone_number", "phone" -> "phoneNumber";
            case "createdat", "created_at", "created" -> "createdAt";
            case "updatedat", "updated_at", "updated", "lastactivity", "last_activity" -> DEFAULT_SORT_FIELD;
            case "city" -> "city";
            case "state" -> "state";
            case "email" -> "email";
            case PREMIUM_SORT_FIELD -> PREMIUM_SORT_FIELD; // Special case - handled separately
            default -> DEFAULT_SORT_FIELD; // Default fallback
        };
    }
    
    private Page<Customer> getCustomersWithPremiumSorting(AdminUser owner, String search, 
                                                         int page, int rec, String sortDirection) {
        // Get all customers without pagination first for premium sorting
        Page<Customer> allCustomers;
        if (search != null && !search.trim().isEmpty()) {
            // Use dedicated search method when search is provided
            allCustomers = customerRepository.searchCustomersByCreatedBy(
                owner, search, PageRequest.of(0, Integer.MAX_VALUE));
        } else {
            // Use basic method when no search is applied
            allCustomers = customerRepository.findActiveByCreatedBy(
                owner, PageRequest.of(0, Integer.MAX_VALUE));
        }
        
        // Sort by premium (best premium from quotes)
        List<Customer> sortedCustomers = allCustomers.getContent().stream()
            .sorted((c1, c2) -> {
                BigDecimal premium1 = getHighestPremium(c1);
                BigDecimal premium2 = getHighestPremium(c2);
                
                int comparison = premium1.compareTo(premium2);
                return "desc".equalsIgnoreCase(sortDirection) ? -comparison : comparison;
            })
            .collect(Collectors.toList());
        
        // Apply pagination manually
        int start = page * rec;
        int end = Math.min(start + rec, sortedCustomers.size());
        List<Customer> paginatedCustomers = sortedCustomers.subList(start, end);
        
        return new PageImpl<>(paginatedCustomers, PageRequest.of(page, rec), sortedCustomers.size());
    }
    
    private BigDecimal getHighestPremium(Customer customer) {
        if (customer.getQuotes() == null || customer.getQuotes().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return customer.getQuotes().stream()
            .map(quote -> {
                try {
                    return new BigDecimal(quote.getBestPremium() != null ? quote.getBestPremium() : "0");
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            })
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }

    @Override
    public ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getAllCustomers(int page, int rec) {
        logger.info("[correlationId:{}] getAllCustomers called", MDC.get("correlationId"));
        BaseResponse<List<CustomerResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<CustomerResponseDto> responseList = new ArrayList<>();
            if (page == -1 && rec == -1) {
                List<Customer> customerList = customerRepository.findAll();
                for (Customer customer : customerList) {
                    responseList.add(mapToResponseDto(customer));
                }
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseList, responseList.size()));
            } else {
                Pageable pageable = PageRequest.of(page, rec);
                Page<Customer> customerPage = customerRepository.findAll(pageable);
                for (Customer customer : customerPage.getContent()) {
                    responseList.add(mapToResponseDto(customer));
                }
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseList, customerPage.getTotalPages()));
            }
        } catch (Exception e) {
            logger.error("Exception in getAllCustomers", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    private CustomerResponseDto mapToResponseDto(Customer customer) {
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
        responseDto.setNotes(customer.getNotes());
        responseDto.setQuotes(customer.getQuotes());
        responseDto.setOwner(customer.getOwner() != null ? customer.getOwner().getUsername() : null);
        return responseDto;
    }

    @Override
    public ResponseEntity<ResponseDto<CustomerResponseDto>> getByCustId(String custId) {
        logger.info("[correlationId:{}] getByCustId called", MDC.get("correlationId"));
        BaseResponse<CustomerResponseDto> responseObj = new BaseResponse<>();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            
            Optional<Customer> optionalCustomer = customerRepository.findByCustId(custId);
            if(optionalCustomer.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            Customer customer = optionalCustomer.get();
            if (customer.getOwner() == null 
                || ( !currentUsername.equals(customer.getOwner().getUsername()) 
                    && (customer.getOwner().getReportingTo() == null 
                        || !currentUsername.equals(customer.getOwner().getReportingTo().getUsername()))
                )) {
                logger.warn("[correlationId:{}] Access denied: User {} tried to access customer {} owned by {}", 
                    MDC.get("correlationId"), currentUsername, custId, 
                    customer.getOwner() != null ? customer.getOwner().getUsername() : "null");
                return responseObj.render(responseObj.formErrorResponse("Access denied"));
            }
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
            responseDto.setNotes(customer.getNotes());
            responseDto.setQuotes(customer.getQuotes());
            responseDto.setOwner(customer.getOwner().getUsername() + " (" + customer.getOwner().getAgentId() + ")");
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto,1));
        } catch (Exception e) {
            logger.error("Exception in getByCustId", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> bulkDelete(CustomerBulkDeleteRequestDto requestDto) {
        logger.info("[correlationId:{}] bulkDelete called for username: {} with customerIds: {}", 
            MDC.get("correlationId"), requestDto.getUsername(), String.join(",", requestDto.getCustomerIds()));
        BaseResponse<String> responseObj = new BaseResponse<>();

        try {
            Optional<AdminUser> agentOpt = adminUserRepository.findByUsername(requestDto.getUsername());
            if (agentOpt.isEmpty()) {
                logger.warn("[correlationId:{}] Agent not found with username: {}", 
                    MDC.get("correlationId"), requestDto.getUsername());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseDto<String>("Agent not found", null));
            }

            AdminUser agent = agentOpt.get();
            List<Customer> customersToDelete = customerRepository.findAllByCustIdIn(requestDto.getCustomerIds());
            
            // Verify all customers exist
            if (customersToDelete.size() != requestDto.getCustomerIds().size()) {
                logger.warn("[correlationId:{}] Some customers not found. Requested: {}, Found: {}", 
                    MDC.get("correlationId"), requestDto.getCustomerIds().size(), customersToDelete.size());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseDto<String>("One or more customers not found", null));
            }

            // Verify all customers belong to the agent
            boolean hasUnauthorizedAccess = customersToDelete.stream()
                    .anyMatch(customer -> !customer.getOwner().getId().equals(agent.getId()));
            
            if (hasUnauthorizedAccess) {
                logger.warn("[correlationId:{}] Unauthorized access attempt by agent: {} for customers: {}", 
                    MDC.get("correlationId"), agent.getUsername(), String.join(",", requestDto.getCustomerIds()));
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ResponseDto<String>("You don't have permission to delete one or more customers", null));
            }

            // Soft Delete all customers
            for(Customer customer : customersToDelete) {
                customer.setStatus("INACTIVE");
                customer.setUpdatedAt(LocalDateTime.now());
                customerRepository.save(customer);
                logger.info("[correlationId:{}] Customer {} soft deleted successfully", 
                    MDC.get("correlationId"), customer.getCustId());
            }

            logger.info("[correlationId:{}] Bulk delete completed successfully for {} customers", 
                MDC.get("correlationId"), customersToDelete.size());
            return ResponseEntity.ok(new ResponseDto<String>("Customers deleted successfully", null));

        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in bulkDelete: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
}
