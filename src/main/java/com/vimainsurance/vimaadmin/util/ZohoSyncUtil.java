package com.vimainsurance.vimaadmin.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.configuration.ZohoConfig;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.entity.Customer;
import com.vimainsurance.vimaadmin.exception.ZohoSyncException;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.zoho.crm.api.HeaderMap;
import com.zoho.crm.api.Initializer;
import com.zoho.crm.api.ParameterMap;
import com.zoho.crm.api.exception.SDKException;
import com.zoho.crm.api.record.APIException;
import com.zoho.crm.api.record.ActionHandler;
import com.zoho.crm.api.record.ActionResponse;
import com.zoho.crm.api.record.ActionWrapper;
import com.zoho.crm.api.record.BodyWrapper;
import com.zoho.crm.api.record.Record;
import com.zoho.crm.api.record.RecordOperations;
import com.zoho.crm.api.record.RecordOperations.SearchRecordsParam;
import com.zoho.crm.api.record.ResponseHandler;
import com.zoho.crm.api.record.ResponseWrapper;
import com.zoho.crm.api.record.SuccessResponse;
import com.zoho.crm.api.users.MinifiedUser;

@Component
public class ZohoSyncUtil {

    private static final String REQUIRED_FIELDS = 
        "Full_Name,Email,Phone,City,State,Occupation,Annual_Income," +
        "Created_Time,Modified_Time,Lead_Status,Owner.email,Date_of_Birth,Gender,Mobile";

    @Autowired
    private ZohoConfig zohoConfig;

    @Autowired
    private ICustomerRepository customerRepository;

    @Autowired
    private ZohoCRMUtil zohoCRMUtil;

    @Autowired
    private IAdminUserRepository adminUserRepository;


    /**
     * Initialize Zoho CRM if not already initialized
     * @throws ZohoSyncException if initialization fails
     */
    public void initializeZohoCRM() throws ZohoSyncException {
        try {
            if(Initializer.getInitializer() == null) {
                zohoConfig.initializeZohoManually(null);
            }
        } catch (SDKException e) {
            throw new ZohoSyncException("Failed to initialize Zoho CRM", e);
        } catch (Exception e) {
            throw new ZohoSyncException("Failed to initialize Zoho CRM", e);
       }
    }

    /**
     * Create parameter map with common fields
     * @param page Page number
     * @param size Page size
     * @param email Owner email for filtering
     * @return Configured ParameterMap
     * @throws ZohoSyncException if parameter creation fails
     */
    public ParameterMap createParameterMap(Integer page, Integer size, String critreria) throws ZohoSyncException {
        try {
            ParameterMap paramInstance = new ParameterMap();
            paramInstance.add(SearchRecordsParam.FIELDS, REQUIRED_FIELDS);
            
            if (critreria != null) {
                paramInstance.add(SearchRecordsParam.CRITERIA, critreria);
            }
            
            if (page != null && size != null) {
                paramInstance.add(SearchRecordsParam.PAGE, page);
                paramInstance.add(SearchRecordsParam.PER_PAGE, size);
            }
            
            return paramInstance;
        } catch (SDKException e) {
            throw new ZohoSyncException("Failed to create parameter map", e);
        }
    }

    /**
     * Process a single Zoho CRM record and convert to CustomerResponseDto
     * @param record Zoho CRM record
     * @return CustomerResponseDto
     */
    public CustomerResponseDto processZohoRecord(com.zoho.crm.api.record.Record record) {
        return zohoCRMUtil.mapRecordToCustomerDto(record);
    }

    /**
     * Process Zoho CRM records and convert to CustomerResponseDto list
     * @param records List of Zoho CRM records
     * @return List of CustomerResponseDto
     */
    public List<CustomerResponseDto> processZohoRecords(List<com.zoho.crm.api.record.Record> records) {
        List<CustomerResponseDto> customers = new ArrayList<>();
        for (com.zoho.crm.api.record.Record record : records) {
            customers.add(processZohoRecord(record));
        }
        return customers;
    }

    /**
     * Sync a single customer from Zoho CRM to local database
     * @param dto CustomerResponseDto from Zoho CRM
     * @return true if sync successful, false otherwise
     * @throws ZohoSyncException if there's an error during sync
     */
    public boolean syncCustomer(CustomerResponseDto dto) throws ZohoSyncException {
        try {
            Customer customer = mapDtoToCustomer(dto);
            
            if (customer.getZohoCrmId() != null) {
                Customer existingCustomer = customerRepository.findByZohoCrmId(customer.getZohoCrmId()).orElse(null);
                if (existingCustomer != null) {
                    updateCustomerFromDto(existingCustomer, dto);
                    customerRepository.save(existingCustomer);
                    return true;
                }
            }
            
            customerRepository.save(customer);
            return true;
            
        } catch (Exception e) {
            throw new ZohoSyncException("Failed to sync customer: " + e.getMessage(), e);
        }
    }

    /**
     * Batch sync customers from Zoho CRM to local database
     * @param dtos List of CustomerResponseDto from Zoho CRM
     * @return number of successfully synced records
     * @throws ZohoSyncException if there's an error during sync
     */
    public int syncCustomerBatch(List<CustomerResponseDto> dtos) throws ZohoSyncException {
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (CustomerResponseDto dto : dtos) {
            try {
                if (syncCustomer(dto)) {
                    successCount++;
                }
            } catch (Exception e) {
                errors.add(String.format("Failed to sync customer %s: %s", 
                    dto.getEmail(), e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            throw new ZohoSyncException(String.format("Batch sync completed with %d successes and %d failures: %s",
                successCount, errors.size(), String.join("\n", errors)));
        }

        return successCount;
    }

    /**
     * Map CustomerResponseDto to Customer entity
     */
    private Customer mapDtoToCustomer(CustomerResponseDto dto) {
        Customer customer = new Customer();
        customer.setZohoCrmId(dto.getZohoCrmId());
        customer.setFullName(dto.getFullName());
        customer.setEmail(dto.getEmail());
        customer.setDateOfBirth(dto.getDateOfBirth());
        customer.setGender("MALE");
        customer.setPhoneNumber(dto.getPhoneNumber());
        customer.setCity(dto.getCity());
        customer.setState(dto.getState());
        customer.setOccupation(dto.getOccupation());
        customer.setAnnualIncome(dto.getAnnualIncome());
        customer.setStatus(dto.getStatus());
        customer.setCreatedAt(dto.getCreatedAt());
        customer.setUpdatedAt(dto.getUpdatedAt());
        customer.setOwner(adminUserRepository.findByUsername("mithun").isPresent() ? adminUserRepository.findByUsername("mithun").get() : null);
        customer.setCreatedBy(adminUserRepository.findByUsername("mithun").isPresent() ? adminUserRepository.findByUsername("mithun").get() : null);

        if (customer.getCustId() == null) {
            customer.setCustId(generateCustomerId());
        }
        
        return customer;
    }

    /**
     * Update existing customer from DTO
     */
    private void updateCustomerFromDto(Customer existing, CustomerResponseDto dto) {
        existing.setFullName(dto.getFullName());
        existing.setEmail(dto.getEmail());
        existing.setPhoneNumber(dto.getPhoneNumber());
        existing.setCity(dto.getCity());
        existing.setState(dto.getState());
        existing.setOccupation(dto.getOccupation());
        existing.setAnnualIncome(dto.getAnnualIncome());
        existing.setStatus(dto.getStatus());
        existing.setUpdatedAt(dto.getUpdatedAt());
        existing.setDateOfBirth(dto.getDateOfBirth());
    }

    /**
     * Generate a new customer ID
     */
    private String generateCustomerId() {
        String maxId = customerRepository.findMaxCustomerId();
        if (maxId == null) {
            return "C001";
        }
        int num = Integer.parseInt(maxId.substring(1));
        num++;
        return String.format("C%03d", num);
    }

    /**
     * Fetch records from Zoho CRM with pagination
     * @param offset Starting offset
     * @param batchSize Number of records to fetch
     * @return List of Zoho records
     * @throws ZohoSyncException if fetch fails
     */
    public List<Record> fetchZohoRecords(int offset, int batchSize) throws ZohoSyncException {
        try {
            initializeZohoCRM();
            
            RecordOperations recordOperations = new RecordOperations();
            ParameterMap params = new ParameterMap();
            params.add(SearchRecordsParam.FIELDS, REQUIRED_FIELDS);
            params.add(SearchRecordsParam.PAGE, offset / batchSize + 1);
            params.add(SearchRecordsParam.PER_PAGE, batchSize);
            
            com.zoho.crm.api.util.APIResponse<ResponseHandler> apiResponse = 
                recordOperations.getRecords("Leads", params, new HeaderMap());
            ResponseHandler response = apiResponse.getObject();
            
            if (response instanceof ResponseWrapper) {
                ResponseWrapper responseWrapper = (ResponseWrapper) response;
                return responseWrapper.getData();
            } else if (response instanceof APIException) {
                APIException exception = (APIException) response;
                throw new ZohoSyncException("Failed to fetch Zoho records: " + exception.getMessage().getValue());
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            throw new ZohoSyncException("Failed to fetch Zoho records: " + e.getMessage(), e);
        }
    }

    /**
     * Convert a Customer entity to Zoho Record
     * @param customer Customer entity
     * @return Zoho Record
     */
    public Record convertToZohoRecord(Customer customer) {
        Record record = new Record();
        
        // Create field objects for each field
        com.zoho.crm.api.record.Field<String> fullNameField = new com.zoho.crm.api.record.Field<>("Full_Name");
        com.zoho.crm.api.record.Field<String> firstNameField = new com.zoho.crm.api.record.Field<>("First_Name");
        com.zoho.crm.api.record.Field<String> lastNameField = new com.zoho.crm.api.record.Field<>("Last_Name");
        com.zoho.crm.api.record.Field<String> emailField = new com.zoho.crm.api.record.Field<>("Email");
        com.zoho.crm.api.record.Field<String> phoneField = new com.zoho.crm.api.record.Field<>("Mobile");
        com.zoho.crm.api.record.Field<String> cityField = new com.zoho.crm.api.record.Field<>("City");
        com.zoho.crm.api.record.Field<String> stateField = new com.zoho.crm.api.record.Field<>("State");
        com.zoho.crm.api.record.Field<String> occupationField = new com.zoho.crm.api.record.Field<>("Occupation");
        com.zoho.crm.api.record.Field<String> annualIncomeField = new com.zoho.crm.api.record.Field<>("Annual_Income");
        com.zoho.crm.api.record.Field<com.zoho.crm.api.util.Choice<String>> leadStatusField = new com.zoho.crm.api.record.Field<>("Lead_Status");
        com.zoho.crm.api.record.Field<String> dateOfBirthField = new com.zoho.crm.api.record.Field<>("Date_of_Birth");
        com.zoho.crm.api.record.Field<String> genderField = new com.zoho.crm.api.record.Field<>("Gender");
        
        // Set field values
        String fullName = customer.getFullName();
        record.addFieldValue(fullNameField, fullName);
        
        // Handle first name and last name
        String[] nameParts = fullName.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "Unknown";
        
        record.addFieldValue(firstNameField, firstName);
        record.addFieldValue(lastNameField, lastName);
        
        record.addFieldValue(emailField, customer.getEmail());
        record.addFieldValue(phoneField, customer.getPhoneNumber());
        record.addFieldValue(cityField, customer.getCity());
        record.addFieldValue(stateField, customer.getState());
        record.addFieldValue(occupationField, customer.getOccupation());
        record.addFieldValue(annualIncomeField, customer.getAnnualIncome() != null ? customer.getAnnualIncome().toString() : "0");
        
        // Convert status to Zoho CRM lead status
        com.zoho.crm.api.util.Choice<String> leadStatus = new com.zoho.crm.api.util.Choice<>(mapCustomerStatusToZohoStatus(customer.getStatus()));
        record.addFieldValue(leadStatusField, leadStatus);
        
        record.addFieldValue(dateOfBirthField, customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : "");
        record.addFieldValue(genderField, customer.getGender());
        
        // Set owner information using Zoho CRM ID
        if (customer.getOwner() != null && customer.getOwner().getZohoCrmId() != null) {
            MinifiedUser owner = new MinifiedUser();
            owner.setId(Long.valueOf(customer.getOwner().getZohoCrmId()));
            owner.setEmail(customer.getOwner().getEmail());
            record.addKeyValue("Owner", owner);
        }
        
        if (customer.getZohoCrmId() != null) {
            record.addKeyValue("id", customer.getZohoCrmId());
        }
        
        return record;
    }

    /**
     * Map customer status to Zoho CRM lead status
     * @param customerStatus The customer status from database
     * @return Corresponding Zoho CRM lead status
     */
    private String mapCustomerStatusToZohoStatus(String customerStatus) {
        if (customerStatus == null || customerStatus.isEmpty()) {
            return "New Lead";
        }
        return switch (customerStatus.toUpperCase()) {
            case "ACTIVE" -> "Qualified Lead";
            case "INACTIVE" -> "Not Interested";
            case "PENDING" -> "New Lead";
            case "CONTACTED" -> "Contacted/Qualification";
            case "INSURED" -> "Already Insured";
            case "FOLLOW_UP" -> "Pre Advisory follow up";
            case "SCHEDULED" -> "Advisory Call Scheduled";
            default -> "New Lead";
        };
    }

    /**
     * Exception for conflict scenarios in Zoho sync
     */
    public static class ZohoConflictException extends ZohoSyncException {
        private final Record zohoRecord;
        private final Record localRecord;

        public ZohoConflictException(String message, Record zohoRecord, Record localRecord) {
            super(message);
            this.zohoRecord = zohoRecord;
            this.localRecord = localRecord;
        }

        public Record getZohoRecord() { return zohoRecord; }
        public Record getLocalRecord() { return localRecord; }
    }

    /**
     * Convert LocalDateTime to OffsetDateTime using system default zone
     * @param localDateTime LocalDateTime to convert
     * @return OffsetDateTime in system timezone
     */
    private OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /**
     * Create or update a record in Zoho CRM using upsert API with conflict detection
     * @param record Zoho Record to upsert
     * @param customer Customer entity containing update timestamp
     * @param strategy Conflict resolution strategy to use
     * @return Zoho record ID
     * @throws ZohoSyncException if upsert fails
     * @throws ZohoConflictException if a conflict is detected and strategy is MANUAL
     */
    public String upsertToZoho(Record record, Customer customer, ZohoSyncStrategy strategy) throws ZohoSyncException, ZohoConflictException {
        try {
            initializeZohoCRM();
            
            // Convert customer's update timestamp to OffsetDateTime
            OffsetDateTime lastUpdateTime = toOffsetDateTime(customer.getUpdatedAt());
            
            // If we have an ID, check for conflicts first
            if (record.getKeyValue("id") != null) {
                Record existingRecord = getRecordById(Long.valueOf(record.getKeyValue("id").toString()));
                if (existingRecord != null) {
                    OffsetDateTime zohoModifiedTime = getZohoModifiedTime(existingRecord);
                    
                    // If Zoho record was modified after our last update, we have a conflict
                    if (zohoModifiedTime != null && lastUpdateTime != null && 
                        zohoModifiedTime.isAfter(lastUpdateTime)) {
                        
                        // Handle conflict based on strategy
                        switch (strategy) {
                            case ZohoSyncStrategy.ZOHO_FIRST -> {
                                // Use Zoho version - return existing ID
                                return existingRecord.getId().toString();
                            }
                                
                            case ZohoSyncStrategy.DB_FIRST -> {
                            }
                                
                            case ZohoSyncStrategy.LATEST_WINS -> {
                                // Compare timestamps and use the newer version
                                if (zohoModifiedTime.isAfter(lastUpdateTime)) {
                                    // Zoho version is newer
                                    return existingRecord.getId().toString();
                                }
                                // Local version is newer - continue with upsert
                            }
                                
                            case ZohoSyncStrategy.MANUAL -> // Throw exception for manual resolution
                                throw new ZohoConflictException(
                                    "Conflict detected: Manual resolution required",
                                    existingRecord,
                                    record
                                );
                                
                            default -> throw new ZohoSyncException("Unknown conflict resolution strategy: " + strategy);
                        }
                        // Use local version - continue with upsert
                                            }
                }
            }

            RecordOperations recordOperations = new RecordOperations();
            BodyWrapper bodyWrapper = new BodyWrapper();
            List<Record> records = new ArrayList<>();
            records.add(record);
            bodyWrapper.setData(records);
            
            HeaderMap headerInstance = new HeaderMap();
            com.zoho.crm.api.util.APIResponse<ActionHandler> response;
            
            // Use upsert API
            response = recordOperations.upsertRecords("Leads", bodyWrapper, headerInstance);
            
            ActionHandler actionHandler = response.getObject();
            if (actionHandler instanceof ActionWrapper) {
                ActionWrapper actionWrapper = (ActionWrapper) actionHandler;
                ActionResponse actionResponse = actionWrapper.getData().get(0);
                
                if (actionResponse instanceof SuccessResponse) {
                    SuccessResponse successResponse = (SuccessResponse) actionResponse;
                    return successResponse.getDetails().get("id").toString();
                } else {
                    APIException exception = (APIException) actionResponse;
                    throw new ZohoSyncException("Failed to upsert to Zoho: " + exception.getMessage().getValue());
                }
            }
            
            throw new ZohoSyncException("Unexpected response from Zoho CRM");
        } catch (ZohoConflictException e) {
            throw e;
        } catch (Exception e) {
            throw new ZohoSyncException("Failed to upsert to Zoho: " + e.getMessage(), e);
        }
    }

    /**
     * Create or update a record in Zoho CRM using default MANUAL conflict strategy
     * @param record Zoho Record to upsert
     * @param customer Customer entity containing update timestamp
     * @return Zoho record ID
     * @throws ZohoSyncException if upsert fails
     * @throws ZohoConflictException if a conflict is detected
     */
    public String upsertToZoho(Record record, Customer customer) throws ZohoSyncException, ZohoConflictException {
        return upsertToZoho(record, customer, ZohoSyncStrategy.MANUAL);
    }

    /**
     * Get a record by its Zoho CRM ID
     * @param recordId Zoho CRM record ID
     * @return Record if found, null otherwise
     * @throws ZohoSyncException if retrieval fails
     */
    private Record getRecordById(Long recordId) throws ZohoSyncException {
        try {
            initializeZohoCRM();
            RecordOperations recordOperations = new RecordOperations();
            ParameterMap paramInstance = new ParameterMap();
            HeaderMap headerInstance = new HeaderMap();
            
            com.zoho.crm.api.util.APIResponse<ResponseHandler> response = 
                recordOperations.getRecord(recordId, "Leads", paramInstance, headerInstance);
            
            ResponseHandler responseHandler = response.getObject();
            if (responseHandler instanceof ResponseWrapper) {
                ResponseWrapper responseWrapper = (ResponseWrapper) responseHandler;
                List<Record> records = responseWrapper.getData();
                return records != null && !records.isEmpty() ? records.get(0) : null;
            }
            
            return null;
        } catch (Exception e) {
            throw new ZohoSyncException("Failed to get record from Zoho: " + e.getMessage(), e);
        }
    }

    /**
     * Extract the modified time from a Zoho record
     * @param record Zoho record
     * @return Modified time as OffsetDateTime, null if not available
     */
    private OffsetDateTime getZohoModifiedTime(Record record) {
        try {
            Object modifiedTime = record.getKeyValue("Modified_Time");
            if (modifiedTime instanceof String) {
                return OffsetDateTime.parse((String) modifiedTime);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
} 