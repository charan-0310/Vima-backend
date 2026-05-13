package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.env.Environment;

import java.io.InputStream;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.ConvertToDealRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.DealsRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerBulkDeleteRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.PolicyRequestDto;
import com.vimainsurance.vimaadmin.dto.MotorPolicyDetailsRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Customer;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.entity.Nominee;
import com.vimainsurance.vimaadmin.entity.InsuranceProvider;
import com.vimainsurance.vimaadmin.entity.MotorPolicyDetails;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.repository.IMotorPolicyDetailsRepository;
import com.vimainsurance.vimaadmin.repository.INomineeRepository;
import com.vimainsurance.vimaadmin.service.ICustomerService;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IPolicyService;
import com.vimainsurance.vimaadmin.service.IS3Service;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.ConverterUtils;
import com.vimainsurance.vimaadmin.util.EnvironmentUtil;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.SlackNotificationUtil;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.dto.NomineeRequestDto;
import com.vimainsurance.vimaadmin.enums.Gender;
import com.vimainsurance.vimaadmin.enums.NomineeRelationship;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

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
    private IDocumentRepository documentRepository;

    @Autowired
    private IInsuranceProviderRepository insuranceProviderRepository;

    @Autowired
    private IDocumentService documentService;

    @Autowired
    private IS3Service s3Service;

    @Autowired
    private IDealsRepository dealsRepository;

    @Autowired
    private IPolicyService policyService;

    @Autowired
    private IPolicyRepository policyRepository;

    @Autowired
    private INomineeRepository nomineeRepository;

    @Autowired
    private IMotorPolicyDetailsRepository motorPolicyDetailsRepository;
    
    @Autowired
    private SlackNotificationUtil slackNotificationUtil;
    
    @Autowired
    private Environment environment;
   
    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @AuditedOperation(schemaName = "admin", tableName = "customers", entityType = "CUSTOMER", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> update(CustomerRequestDto requestDto) {
        logger.info("[correlationId:{}] update called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Customer> existCustomer = customerRepository.findByCustId(requestDto.getCustId());
            if(existCustomer.isPresent()){
                Customer customer = existCustomer.get();
                try {
                    AuditContextSupplier.setOldSnapshotJson(objectMapper.writeValueAsString(customer));
                } catch (Exception e) {
                    logger.warn("[correlationId:{}] Could not serialize customer for audit old snapshot: {}", MDC.get("correlationId"), e.getMessage());
                }
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
                com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setNewSnapshotEntity(customer);
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
    @AuditedOperation(schemaName = "admin", tableName = "customers", entityType = "CUSTOMER", action = "DELETE")
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
    
    private Page<Customer> getAllCustomersWithPremiumSorting(String search, int page, int rec, String sortDirection) {
        // WARNING: Premium sorting requires loading all customers into memory
        // Use a reasonable maximum limit to prevent memory issues
        int maxLimit = 10000; // Maximum records to fetch for sorting
        
        Page<Customer> allCustomers;
        if (search != null && !search.trim().isEmpty()) {
            // Use dedicated search method when search is provided
            allCustomers = customerRepository.searchAllCustomers(
                search, PageRequest.of(0, maxLimit));
        } else {
            // Use basic method when no search is applied
            allCustomers = customerRepository.findAll(
                PageRequest.of(0, maxLimit));
        }
        
        if (allCustomers.getTotalElements() > maxLimit) {
            logger.warn("[correlationId:{}] Total customers ({}) exceeds maximum limit ({}). Only sorting first {} records.", 
                MDC.get("correlationId"), allCustomers.getTotalElements(), maxLimit, maxLimit);
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
        
        return new PageImpl<>(paginatedCustomers, PageRequest.of(page, rec), allCustomers.getTotalElements());
    }

    @Override
    public ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getAllCustomers(String search, int page, int rec, String sortBy, String sortDirection) {
        logger.info("[correlationId:{}] getAllCustomers called with filters - sortBy: {}, sortDirection: {}", 
                   MDC.get("correlationId"), sortBy, sortDirection);
        BaseResponse<List<CustomerResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<CustomerResponseDto> responseList = new ArrayList<>();
            
            // Handle special case for getting all customers without pagination
            // WARNING: This can cause memory issues with large datasets - consider adding a maximum limit
            if (page == -1 && rec == -1) {
                // Use a reasonable maximum limit to prevent memory issues
                int maxLimit = 10000; // Maximum records to fetch
                PageRequest maxPageRequest = PageRequest.of(0, maxLimit);
                Page<Customer> customerPage = customerRepository.findAll(maxPageRequest);
                
                if (customerPage.getTotalElements() > maxLimit) {
                    logger.warn("[correlationId:{}] Total customers ({}) exceeds maximum limit ({}). Only returning first {} records.", 
                        MDC.get("correlationId"), customerPage.getTotalElements(), maxLimit, maxLimit);
                }
                
                for (Customer customer : customerPage.getContent()) {
                    responseList.add(mapToResponseDto(customer));
                }
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseList, customerPage.getTotalElements()));
            }
            
            // Determine which query method to use based on sorting
            Page<Customer> customerList;
            if (PREMIUM_SORT_FIELD.equalsIgnoreCase(sortBy)) {
                // For premium sorting, we need to handle it separately
                customerList = getAllCustomersWithPremiumSorting(search, page, rec, sortDirection);
            } else if (search != null && !search.trim().isEmpty()) {
                // Use dedicated search method when search is provided
                Sort sort = createSort(sortBy, sortDirection);
                PageRequest pageRequest = PageRequest.of(page, rec, sort);
                customerList = customerRepository.searchAllCustomers(search, pageRequest);
            } else {
                // Use basic method when no search is applied
                Sort sort = createSort(sortBy, sortDirection);
                PageRequest pageRequest = PageRequest.of(page, rec, sort);
                customerList = customerRepository.findAll(pageRequest);
            }

            LinkedHashSet<CustomerResponseDto> customerResponseSet = new LinkedHashSet<>();
            if(customerList.isEmpty()){
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new ArrayList<>(), 0));
            }
            
            for(Customer customer : customerList){
                customerResponseSet.add(mapToResponseDto(customer));
            }
            
            List<CustomerResponseDto> uniqueList = new ArrayList<>(customerResponseSet);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, uniqueList, customerList.getTotalElements()));
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
            // Extract username from JWT token with fallback logic
            final String currentUsername = jwtUserExtractor.extractCurrentUsername();
            
            if (currentUsername == null || currentUsername.isEmpty()) {
                logger.error("[correlationId:{}] Unable to determine current username from authentication", MDC.get("correlationId"));
                return responseObj.render(responseObj.formErrorResponse("Unable to determine current user"));
            }
            
            Optional<Customer> optionalCustomer = customerRepository.findByCustId(custId);
            if(optionalCustomer.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            
            // Resolve admin user from JWT with username/email + case-insensitive fallback.
            AdminUser adminUser = jwtUserExtractor.resolveCurrentAdminUser()
                .orElseThrow(() -> new RuntimeException("Admin user not found for current JWT user"));
            Customer customer = optionalCustomer.get();
            
            // Check if user has admin privileges (ADMIN or VIMA_ADMIN roles)
            boolean hasAdminAccess = "ADMIN".equals(adminUser.getRole()) || "VIMA_ADMIN".equals(adminUser.getRole());
            
            if (!hasAdminAccess && (customer.getOwner() == null 
                || ( !currentUsername.equals(customer.getOwner().getUsername()) 
                    && (customer.getOwner().getReportingTo() == null 
                        || !currentUsername.equals(customer.getOwner().getReportingTo().getUsername()))
                ))) {
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
            responseDto.setDocuments(documentRepository.findByEntityAndCategory(DocumentEntityType.CUSTOMER, customer.getCustId(), DocumentCategory.KYC_DOCUMENTS).stream().map(document -> {
                DocumentResponseDto documentResponseDto = new DocumentResponseDto();
                documentResponseDto.setDocumentType(document.getDocumentType());
                documentResponseDto.setUploadedAt(document.getUploadedAt());
                documentResponseDto.setDocumentMimeType(document.getMimeType());
                return documentResponseDto;
            }).collect(Collectors.toList()));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto,1));
        } catch (Exception e) {
            logger.error("Exception in getByCustId", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @AuditedOperation(schemaName = "admin", tableName = "customers", entityType = "CUSTOMER", action = "BULK_DELETE")
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

            // Check if agent has admin privileges (ADMIN or VIMA_ADMIN roles)
            boolean hasAdminAccess = "ADMIN".equals(agent.getRole()) || "VIMA_ADMIN".equals(agent.getRole());

            // Verify all customers belong to the agent (unless agent has admin access)
            if (!hasAdminAccess) {
                boolean hasUnauthorizedAccess = customersToDelete.stream()
                        .anyMatch(customer -> !customer.getOwner().getId().equals(agent.getId()));
                
                if (hasUnauthorizedAccess) {
                    logger.warn("[correlationId:{}] Unauthorized access attempt by agent: {} for customers: {}", 
                        MDC.get("correlationId"), agent.getUsername(), String.join(",", requestDto.getCustomerIds()));
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new ResponseDto<String>("You don't have permission to delete one or more customers", null));
                }
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


    @Override
    @AuditedOperation(schemaName = "document", tableName = "documents", entityType = "CUSTOMER_DOCUMENT", action = "CREATE")
    public ResponseEntity<ResponseDto<String>> uploadDocument(DocumentRequestDto requestDto, String customerId) {
        logger.info("[correlationId:{}] uploadDocument called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<AdminUser> adminUser = jwtUserExtractor.resolveCurrentAdminUser();
            if(adminUser.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            AdminUser agent = adminUser.get();
            requestDto.setUploadedBy(agent.getId());
            requestDto.setUploadedByRole(com.vimainsurance.vimaadmin.enums.UserRole.fromValue(agent.getRole()));
            ResponseEntity<ResponseDto<List<Document>>> response = documentService.uploadKYCDocuments(requestDto.getFiles(), customerId, DocumentEntityType.CUSTOMER, DocumentType.fromValue(requestDto.getDocumentType()), requestDto.getUploadedBy(), requestDto.getUploadedByRole(), requestDto.getNotes(), DocumentCategory.KYC_DOCUMENTS);
            if(response.getBody().getErrorCode() != null){
                return responseObj.render(responseObj.formErrorResponse(response.getBody().getMessage()));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Document uploaded successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in uploadDocument: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(String customerId) {
        logger.info("[correlationId:{}] uploadDocument called", MDC.get("correlationId"));
        BaseResponse<List<DocumentResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<DocumentResponseDto> responseDto = documentRepository.findByEntityAndCategory(DocumentEntityType.CUSTOMER, customerId, DocumentCategory.KYC_DOCUMENTS).stream().map(document -> {
                DocumentResponseDto documentResponseDto = new DocumentResponseDto();
                documentResponseDto.setDocumentType(document.getDocumentType());
                documentResponseDto.setUploadedAt(document.getUploadedAt());
                documentResponseDto.setDocumentMimeType(document.getMimeType());
                documentResponseDto.setNotes(document.getNotes());
                documentResponseDto.setDocumentId(document.getDocumentId().toString());
                documentResponseDto.setDocumentName(document.getOriginalFilename());
                documentResponseDto.setCategory(document.getDocumentCategory().getValue());
                return documentResponseDto;
            }).collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto,responseDto.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getDocuments: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<Resource> downloadDocument(String documentId) {
        logger.info("[correlationId:{}] getDocumentDownloadUrl called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(UUID.fromString(documentId));
            if(documentOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            Document document = documentOpt.get();
            InputStream downloadUrl = s3Service.downloadFile(document.getS3Key());
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFilename() + "\"").header("Access-Control-Expose-Headers", "content-disposition")
            .contentType(MediaType.parseMediaType(document.getMimeType())).body(new InputStreamResource(downloadUrl));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getDocumentDownloadUrl: {}", MDC.get("correlationId"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Override
    @AuditedOperation(schemaName = "document", tableName = "documents", entityType = "CUSTOMER_DOCUMENT", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> deleteDocument(String documentId) {
        logger.info("[correlationId:{}] deleteDocument called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(UUID.fromString(documentId));
            if(documentOpt.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            s3Service.deleteFile(documentOpt.get().getS3Key());
            if(documentOpt.get().getDocumentType().equals(DocumentType.PAN_CARD) || documentOpt.get().getDocumentType().equals(DocumentType.AADHAAR_CARD)){
                s3Service.deleteFile(documentOpt.get().getS3Key().replace("masked", "original"));
            }
            documentRepository.delete(documentOpt.get());
            logger.info("[correlationId:{}] Document deleted successfully", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Document deleted successfully"));
        }
        catch (Exception e) {
            logger.error("[correlationId:{}] Exception in deleteDocument: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "DEAL", action = "CREATE")
    public ResponseEntity<ResponseDto<String>> customerToDeals(ConvertToDealRequestDto requestDto, String custId) {
        logger.info("[correlationId:{}] customerToDeals called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Customer> customerOpt = customerRepository.findByCustId(custId);
            if(customerOpt.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            Customer customer = customerOpt.get();
            Optional<Deals> dealsOpt = dealsRepository.findByLeadId(customer.getId());

            if(dealsOpt.isPresent()){
                return responseObj.render(responseObj.formErrorResponse("Customer already converted!!!"));
            }
            Deals deals = new Deals();
            deals.setFirstName("");
            deals.setLastName(customer.getFullName());
            deals.setFullName(customer.getFullName());
            deals.setEmail(customer.getEmail());
            deals.setPhone(customer.getPhoneNumber());
            deals.setDateOfBirth(customer.getDateOfBirth());
            deals.setGender(customer.getGender());
            deals.setPanNumber(null);
            deals.setAadhaarNumber(null);
            deals.setAddress(null);
            deals.setCity(customer.getCity());
            deals.setState(customer.getState());
            deals.setPincode(null);
            deals.setAccountType(AccountType.RETAIL_PRIMARY);
            deals.setStatus(AccountStatus.ACTIVE);
            deals.setEmployeeNumber(null);
            deals.setRelationship(null);
            deals.setIsPrimaryMember(true);
            deals.setUsername(null);
            deals.setPasswordHash(null);
            deals.setPreferredLanguage(null);
            deals.setLeadId(customer.getId());
            deals.setCustId(customer.getCustId());
            deals.setCreatedAt(LocalDateTime.now());
            deals.setUpdatedAt(LocalDateTime.now());
            Deals savedDeals = dealsRepository.save(deals);
            
            // Create and save dependents if provided
            List<UUID> coveredIndividualIds = new ArrayList<>();
            coveredIndividualIds.add(savedDeals.getIndividualId()); // Add primary individual
            
            if (requestDto.getDependents() != null && !requestDto.getDependents().isEmpty()) {
                for (DealsRequestDto dependentDto : requestDto.getDependents()) {
                    // Create dependent as a new Deals entity
                    Deals dependent = new Deals();
                    dependent.setFirstName(dependentDto.getFirstName());
                    dependent.setLastName(dependentDto.getLastName());
                    dependent.setEmail(customer.getEmail()); // Use customer's email
                    dependent.setPhone(customer.getPhoneNumber()); // Use customer's phone
                    dependent.setDateOfBirth(dependentDto.getDateOfBirth());
                    dependent.setGender(dependentDto.getGender());
                    dependent.setPanNumber(dependentDto.getPanNumber());
                    dependent.setAadhaarNumber(dependentDto.getAadhaarNumber());
                    dependent.setAddress(customer.getCity() + ", " + customer.getState()); // Use customer's address info
                    dependent.setCity(customer.getCity());
                    dependent.setState(customer.getState());
                    dependent.setPincode(null); // Not available from customer
                    dependent.setAccountType(AccountType.RETAIL_DEPENDENT);
                    dependent.setStatus(AccountStatus.ACTIVE);
                    dependent.setEmployeeNumber(dependentDto.getEmployeeNumber());
                    dependent.setRelationship(dependentDto.getRelationship());
                    dependent.setIsPrimaryMember(false);
                    dependent.setPrimaryIndividual(savedDeals);
                    dependent.setCreatedAt(LocalDateTime.now());
                    dependent.setUpdatedAt(LocalDateTime.now());
                    dependent.setLeadId(customer.getId());
                    dependent.setCustId(customer.getCustId());
                    
                    // Save dependent
                    Deals savedDependent = dealsRepository.save(dependent);
                    coveredIndividualIds.add(savedDependent.getIndividualId());
                    logger.info("[correlationId:{}] Dependent saved with ID: {}", MDC.get("correlationId"), savedDependent.getIndividualId());
                }
            }
            
            // Create policy for the converted deal
            PolicyRequestDto policyRequest = new PolicyRequestDto();
            policyRequest.setPolicyNumber(requestDto.getPolicyNumber() != null ? requestDto.getPolicyNumber() : generatePolicyNumber());
            policyRequest.setPrimaryIndividualId(savedDeals.getIndividualId());
            
            // Get default insurance provider for the product type
            policyRequest.setInsuranceCompanyCode(requestDto.getProviderCode());
            policyRequest.setProductType(requestDto.getProductType());
            // Set coverage type based on whether dependents exist
            String coverageType = (requestDto.getDependents() != null && !requestDto.getDependents().isEmpty()) ? "FAMILY_FLOATER" : "INDIVIDUAL";
            policyRequest.setCoverageType(coverageType);
            policyRequest.setStatus(requestDto.getPolicyStatus() != null ? requestDto.getPolicyStatus() : "ACTIVE");
            policyRequest.setSumInsured(requestDto.getSumInsured());
            policyRequest.setPremiumAmount(requestDto.getPremiumAmount());
            policyRequest.setStartDate(requestDto.getPolicyStartDate() != null ? requestDto.getPolicyStartDate() : LocalDate.now());
            policyRequest.setEndDate(requestDto.getRenewalDate() != null ? requestDto.getRenewalDate() : LocalDate.now().plusYears(1));
            policyRequest.setLeadId(customer.getId());
            policyRequest.setNetAmount(requestDto.getNetAmount());
            policyRequest.setGst(requestDto.getGst());
            policyRequest.setRenewalDate(requestDto.getRenewalDate() != null ? requestDto.getRenewalDate() : LocalDate.now().plusYears(1));
            // Payment frequency
            policyRequest.setPaymentFrequency(requestDto.getPaymentFrequency() != null ? requestDto.getPaymentFrequency() : "YEARLY");
            
            // Set covered individuals (primary + dependents)
            // policyRequest.setCoveredIndividuals(coveredIndividualIds.stream()
            //     .map(UUID::toString)
            //     .collect(Collectors.joining(",")));
            
            policyRequest.setDependents(requestDto.getDependents());
            // Create the policy
            ResponseEntity<ResponseDto<String>> policyResponse = policyService.createPolicy(policyRequest);
            Policy savedPolicy = null;
            if (policyResponse.getBody() != null && policyResponse.getBody().getErrorCode() != null) {
                logger.warn("[correlationId:{}] Policy creation failed: {}", MDC.get("correlationId"), policyResponse.getBody().getMessage());
                throw new IllegalStateException(policyResponse.getBody().getMessage());
            } else {
                logger.info("[correlationId:{}] Policy created successfully for deal: {}", MDC.get("correlationId"), savedDeals.getIndividualId());
                savedPolicy = policyRepository.findByPolicyNumber(policyRequest.getPolicyNumber()).orElse(null);
            }

            if (savedPolicy != null && requestDto.getNominees() != null && !requestDto.getNominees().isEmpty()) {
                List<Nominee> nomineeEntities = new ArrayList<>();
                for (NomineeRequestDto nomineeDto : requestDto.getNominees()) {
                    try {
                        Nominee nominee = mapNomineeDtoToEntity(nomineeDto, savedPolicy);
                        if (nominee != null) {
                            nomineeEntities.add(nominee);
                        }
                    } catch (IllegalArgumentException ex) {
                        logger.warn("[correlationId:{}] Skipping nominee due to invalid data: {}", MDC.get("correlationId"), ex.getMessage());
                    }
                }
                if (!nomineeEntities.isEmpty()) {
                    nomineeRepository.saveAll(nomineeEntities);
                    savedPolicy.getNominees().addAll(nomineeEntities);
                    logger.info("[correlationId:{}] Saved {} nominee(s) for policy {}", MDC.get("correlationId"), nomineeEntities.size(), savedPolicy.getPolicyNumber());
                }
            }

            if (savedPolicy != null && savedPolicy.getProductType() == ProductType.MOTOR) {
                MotorPolicyDetailsRequestDto motorDetailsDto = requestDto.getMotorDetails();
                if (motorDetailsDto != null) {
                    try {
                        MotorPolicyDetails motorDetails = mapMotorPolicyDetailsDto(motorDetailsDto, savedPolicy);
                        if (motorDetails != null) {
                            motorPolicyDetailsRepository.save(motorDetails);
                            savedPolicy.setMotorPolicyDetails(motorDetails);
                        }
                    } catch (IllegalArgumentException ex) {
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        logger.warn("[correlationId:{}] Skipping motor policy details due to invalid data: {}", MDC.get("correlationId"), ex.getMessage());
                        return responseObj.render(responseObj.formErrorResponse("Invalid motor policy details"));
                    }
                }
            }
            customer.setStatus("POLICY_ISSUED");
            customer.setUpdatedAt(LocalDateTime.now());
            customerRepository.save(customer);
            Document document = documentRepository.findByEntityAndCategory(DocumentEntityType.CUSTOMER, customer.getCustId(), DocumentCategory.KYC_DOCUMENTS).stream().findFirst().orElse(null);
            if(document != null){
                document.setEntityType(DocumentEntityType.INDIVIDUAL);
                document.setEntityId(deals.getIndividualId().toString());
                documentRepository.save(document);
            }
            AdminUser adminUser = jwtUserExtractor.resolveCurrentAdminUser()
                    .orElseThrow(() -> new RuntimeException("Agent not found"));
            ResponseEntity<ResponseDto<List<Document>>> response = documentService.uploadKYCDocuments(requestDto.getDocument(), deals.getIndividualId().toString(), DocumentEntityType.POLICY, DocumentType.POLICY_CERTIFICATE, adminUser.getId(), UserRole.fromValue(adminUser.getRole()), "", DocumentCategory.POLICY_DOCUMENTS);
            if (response.getBody() == null || response.getBody().getErrorCode() != null) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw new IllegalStateException(response.getBody() != null ? response.getBody().getMessage() : "Policy document upload failed");
            }
            if (response.getBody().getPayload() == null || response.getBody().getPayload().isEmpty()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw new IllegalStateException("Policy document upload failed: empty payload");
            }
            savedPolicy.setDocument(response.getBody().getPayload().get(0));
            policyRepository.save(savedPolicy);
            
            // Send Slack notification only in production
            if (EnvironmentUtil.isProductionEnvironment(environment)) {
                try {
                    String slackMessage = buildCustomerToDealSlackMessage(customer, savedDeals, policyRequest, adminUser);
                    slackNotificationUtil.sendSlackMessage("New Policy Issued!", slackMessage, true);
                } catch (Exception slackException) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    logger.warn("[correlationId:{}] Failed to send Slack notification: {}", MDC.get("correlationId"), slackException.getMessage());
                    // Don't fail the request if Slack notification fails
                }
            } else {
                logger.debug("[correlationId:{}] Skipping Slack notification (not in production environment)", MDC.get("correlationId"));
            }
            
            logger.info("[correlationId:{}] Customer converted to Deals successfully", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Customer converted to Deals successfully"));
        }
        catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("[correlationId:{}] Exception in customerToDeals: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
    
    private Nominee mapNomineeDtoToEntity(NomineeRequestDto dto, Policy policy) {
        if (dto == null) {
            throw new IllegalArgumentException("Nominee details cannot be null");
        }
        if (dto.getFirstName() == null || dto.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Nominee first name is required");
        }
        if (dto.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Nominee date of birth is required");
        }
        if (dto.getGender() == null || dto.getGender().isBlank()) {
            throw new IllegalArgumentException("Nominee gender is required");
        }
        if (dto.getRelationship() == null || dto.getRelationship().isBlank()) {
            throw new IllegalArgumentException("Nominee relationship is required");
        }

        Nominee nominee = new Nominee();
        nominee.setPolicy(policy);
        nominee.setFirstName(dto.getFirstName());
        nominee.setLastName(dto.getLastName());
        nominee.setDateOfBirth(dto.getDateOfBirth());
        nominee.setGender(dto.getGender());
        nominee.setRelationship(dto.getRelationship());
        nominee.setNomineePercentage(dto.getNomineePercentage() != null ? dto.getNomineePercentage() : BigDecimal.valueOf(100.00));
        nominee.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE);
        return nominee;
    }

    private MotorPolicyDetails mapMotorPolicyDetailsDto(MotorPolicyDetailsRequestDto dto, Policy policy) {
        if (dto == null) {
            throw new IllegalArgumentException("Motor policy details cannot be null");
        }
        if (dto.getVehicleRegistrationNumber() == null || dto.getVehicleRegistrationNumber().isBlank()) {
            throw new IllegalArgumentException("Vehicle registration number is required");
        }
        MotorPolicyDetails details = new MotorPolicyDetails();
        details.setPolicy(policy);
        details.setVehicleRegistrationNumber(dto.getVehicleRegistrationNumber());
        details.setVehicleMake(dto.getVehicleMake());
        details.setVehicleModel(dto.getVehicleModel());
        details.setVehicleType(dto.getVehicleType());
        details.setManufacturingYear(dto.getManufacturingYear());
        details.setRegistrationDate(dto.getRegistrationDate());
        details.setIdvValue(dto.getIdvValue());
        return details;
    }

    private String normalizePhoneNumber(String value) {
        return value != null ? value.replaceAll("[^0-9]", "") : null;
    }
    
    private String buildCustomerToDealSlackMessage(Customer customer, Deals deals, PolicyRequestDto policyRequest, AdminUser agent) {
        StringBuilder message = new StringBuilder();
        
        // Get insurance provider name
        String insurerName = "Unknown";
        try {
            if (policyRequest.getInsuranceCompanyCode() != null) {
                insurerName = insuranceProviderRepository.findByProviderCode(policyRequest.getInsuranceCompanyCode())
                        .map(InsuranceProvider::getProviderName)
                        .orElse("Unknown");
            } else {
                insurerName = "Unknown";
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch insurance provider name: {}", e.getMessage());
        }
        
        // Format product type to readable text
        String productTypeDisplay = formatProductType(policyRequest.getProductType());
        
        // Client name
        String clientName = customer.getFullName();
        
        // Build message with emojis
        message.append(":adult::skin-tone-4: Client: ").append(clientName).append("\n");
        message.append(":package: Policy Type: ").append(productTypeDisplay).append("\n");
        message.append(":office: Insurer: ").append(insurerName);
        
        return message.toString();
    }
    
    private String formatProductType(String productType) {
        // Convert enum names to readable format
        // e.g., TERM_LIFE -> Term Life Insurance, HEALTH -> Health Insurance
        if (productType == null || productType.isEmpty()) {
            return "Insurance";
        }
        
        String formatted = productType.replace("_", " ");
        String[] words = formatted.toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
        }
        
        // Add "Insurance" suffix if not already present
        if (!result.toString().toLowerCase().contains("insurance")) {
            result.append(" Insurance");
        }
        
        return result.toString();
    }

    /**
     * Generate a unique policy number
     */
    private String generatePolicyNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomSuffix = String.valueOf((int) (Math.random() * 1000));
        return "POL-" + timestamp.substring(timestamp.length() - 8) + "-" + randomSuffix;
    }

    /**
     * Get default insurance provider ID for the given product type
     * If no provider is found, creates a default one
     */
    private UUID getDefaultInsuranceProviderId(String productType) {
        try {
            ProductType productTypeEnum = ProductType.fromValue(productType);
            
            // Try to find an active provider for the product type
            List<InsuranceProvider> providers = insuranceProviderRepository.findByIsActiveTrueAndProductType(productTypeEnum);
            
            if (!providers.isEmpty()) {
                return providers.get(0).getProviderId();
            }
            
            // If no provider found, create a default one
            InsuranceProvider defaultProvider = new InsuranceProvider();
            defaultProvider.setProviderName("Default " + productType + " Provider");
            defaultProvider.setProviderCode("DEFAULT_" + productType.toUpperCase());
            defaultProvider.setProductType(productTypeEnum);
            defaultProvider.setIsActive(true);
            
            InsuranceProvider savedProvider = insuranceProviderRepository.save(defaultProvider);
            logger.info("[correlationId:{}] Created default insurance provider: {}", MDC.get("correlationId"), savedProvider.getProviderId());
            
            return savedProvider.getProviderId();
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error getting default insurance provider: {}", MDC.get("correlationId"), e.getMessage(), e);
            // Fallback to a random UUID if everything fails
            return UUID.randomUUID();
        }
    }
    
}

