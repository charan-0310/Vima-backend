package com.vimainsurance.vimaadmin.service.serviceimpl;


import com.vimainsurance.vimaadmin.service.IManagerService;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Customer;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.vimainsurance.vimaadmin.util.Constants;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.vimainsurance.vimaadmin.dto.ManagerDashboardResponseDto;
@Service

public class ManagerServiceImpl implements IManagerService {
    private static final Logger logger = LoggerFactory.getLogger(ManagerServiceImpl.class);
    private static final String DEFAULT_SORT_FIELD = "updatedAt";
    private static final String PREMIUM_SORT_FIELD = "premium";


    @Autowired
    private IAdminUserRepository adminUserRepository;
    

    @Override
    public ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getCustomersByManagerAndAgents(
            String username, String search, int page, int rec, String owner, String sortBy, String sortDirection) {
        logger.info("[correlationId:{}] getCustomersByManagerAndAgents called with filters - owner: {}, sortBy: {}, sortDirection: {}", 
                   MDC.get("correlationId"), owner, sortBy, sortDirection);
        BaseResponse<List<CustomerResponseDto>> responseObj = new BaseResponse<>();
        try {
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);
            if(adminUser.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            
            // Determine which query method to use based on filters and sorting
            Page<Customer> customers;
            if (PREMIUM_SORT_FIELD.equalsIgnoreCase(sortBy)) {
                // For premium sorting, we need to handle it separately
                customers = getCustomersWithPremiumSorting(adminUser.get().getId(), owner, search, page, rec, sortDirection);
            } else if (owner != null && !owner.trim().isEmpty()) {
                // Use comprehensive filter method when owner filter is specified
                Sort sort = createSort(sortBy, sortDirection);
                PageRequest pageRequest = PageRequest.of(page, rec, sort);
                customers = adminUserRepository.findLeadsForManagerAndAgentsWithFilters(
                    adminUser.get().getId(), owner, search, pageRequest);
            } else if (search != null && !search.trim().isEmpty()) {
                // Use dedicated search method when only search is provided
                Sort sort = createSort(sortBy, sortDirection);
                PageRequest pageRequest = PageRequest.of(page, rec, sort);
                customers = adminUserRepository.searchLeadsForManagerAndAgents(
                    adminUser.get().getId(), search, pageRequest);
            } else {
                // Use basic method when no filters are applied
                Sort sort = createSort(sortBy, sortDirection);
                PageRequest pageRequest = PageRequest.of(page, rec, sort);
                customers = adminUserRepository.findLeadsForManagerAndAgents(
                    adminUser.get().getId(), pageRequest);
            }
            
            List<CustomerResponseDto> dtos = new ArrayList<>();
            for (Customer customer : customers) {
                dtos.add(mapToResponseDto(customer));
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dtos, customers.getTotalElements()));
        } catch (Exception e) {
            logger.error("Error fetching customers by manager and agents", e);
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
            case "owner" -> "owner.username";
            case "city" -> "city";
            case "state" -> "state";
            case "email" -> "email";
            case PREMIUM_SORT_FIELD -> PREMIUM_SORT_FIELD; // Special case - handled separately
            default -> DEFAULT_SORT_FIELD; // Default fallback
        };
    }
    
    private Page<Customer> getCustomersWithPremiumSorting(UUID managerId, String owner, String search, 
                                                         int page, int rec, String sortDirection) {
        // Get all customers without pagination first for premium sorting
        Page<Customer> allCustomers;
        if (owner != null && !owner.trim().isEmpty()) {
            // Use comprehensive filter method when owner filter is specified
            allCustomers = adminUserRepository.findLeadsForManagerAndAgentsWithFilters(
                managerId, owner, search, PageRequest.of(0, Integer.MAX_VALUE));
        } else if (search != null && !search.trim().isEmpty()) {
            // Use dedicated search method when only search is provided
            allCustomers = adminUserRepository.searchLeadsForManagerAndAgents(
                managerId, search, PageRequest.of(0, Integer.MAX_VALUE));
        } else {
            // Use basic method when no filters are applied
            allCustomers = adminUserRepository.findLeadsForManagerAndAgents(
                managerId, PageRequest.of(0, Integer.MAX_VALUE));
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
    public ResponseEntity<ResponseDto<ManagerDashboardResponseDto>> getManagerDashboard(String username) {
        logger.info("[correlationId:{}] getManagerDashboard called", MDC.get("correlationId"));
        BaseResponse<ManagerDashboardResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);
            if(adminUser.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            ManagerDashboardResponseDto responseDto = new ManagerDashboardResponseDto();
            responseDto.setTotalQuoteSent(adminUserRepository.countByQuoteSent(adminUser.get().getId()));
            responseDto.setAgentNames(adminUserRepository.findByReportingTo(adminUser.get().getId()));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto));
        } catch (Exception e) {
            logger.error("Error fetching manager dashboard", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

        private CustomerResponseDto mapToResponseDto(Customer customer) {
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
            responseDto.setOwner(customer.getOwner().getUsername());
            return responseDto;
    }

}
