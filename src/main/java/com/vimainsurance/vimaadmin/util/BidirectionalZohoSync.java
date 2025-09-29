package com.vimainsurance.vimaadmin.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.config.ZohoSyncConfig;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.entity.Customer;
import com.vimainsurance.vimaadmin.exception.ZohoSyncException;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.zoho.crm.api.record.Record;

@Component
public class BidirectionalZohoSync {

    private static final Logger logger = LoggerFactory.getLogger(BidirectionalZohoSync.class);

    @Autowired
    private ZohoSyncUtil zohoSyncUtil;

    @Autowired
    private ZohoSyncConfig syncConfig;

    @Autowired
    private ICustomerRepository customerRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private IdGenerator customerIdGenerator;

    /**
     * Synchronize data between Zoho CRM and local database
     * @return SyncResult containing success/failure counts
     * @throws ZohoSyncException if sync fails
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public SyncResult synchronize() throws ZohoSyncException {
        SyncResult result = new SyncResult();

        try {
            // First sync from Zoho to DB
            result.zohoToDbResult = syncFromZohoToDb();
            // result.zohoToDbResult = new SyncStats();

            
            // If bidirectional sync is enabled, sync from DB to Zoho
            if (syncConfig.isEnableBidirectionalSync()) {
                result.dbToZohoResult = syncFromDbToZoho();
            }

            return result;
        } catch (Exception e) {
            throw new ZohoSyncException("Sync failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sync data from Zoho CRM to local database
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private SyncStats syncFromZohoToDb() throws ZohoSyncException {
        SyncStats stats = new SyncStats();
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            List<Record> records = zohoSyncUtil.fetchZohoRecords(offset, syncConfig.getBatchSize());
            if (records.isEmpty()) {
                hasMore = false;
                continue;
            }

            //System.out.println("Processing batch of " + records.size() + " records from Zoho CRM");
            
            for (Record record : records) {
                try {
                    CustomerResponseDto dto = zohoSyncUtil.processZohoRecord(record);
                    processAndSaveCustomer(dto, stats);
                } catch (Exception e) {
                    String errorMsg = String.format("Failed to process Zoho record: %s - Error: %s", 
                        record.getKeyValue("Email"), e.getMessage());
                    logger.error(errorMsg);
                    stats.failureCount++;
                    stats.errors.add(errorMsg);
                    // Continue with next record
                }
            }

            offset += syncConfig.getBatchSize();
        }

        // Print summary
        //System.out.println("\nSync Summary:");
        //System.out.println("Successfully processed: " + stats.successCount + " records");
        //System.out.println("Failed to process: " + stats.failureCount + " records");
        if (!stats.errors.isEmpty()) {
            //System.out.println("\nDetailed Error Report:");
            // stats.errors.forEach(error -> //System.out.println("- " + error));
        }

        return stats;
    }

    /**
     * Process and save a single customer record in its own transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void processAndSaveCustomer(CustomerResponseDto dto, SyncStats stats) {
        try {
            // validateCustomerData(dto);
            
            // First check if customer exists by Zoho CRM ID
            Optional<Customer> existingByZohoId = Optional.empty();
            if (dto.getZohoCrmId() != null) {
                existingByZohoId = customerRepository.findByZohoCrmId(dto.getZohoCrmId());
            }

            // Then check by email or phone
            // Optional<Customer> existingByEmail = customerRepository.findByEmail(dto.getEmail());
            Optional<Customer> existingByPhone = customerRepository.findByPhoneNumber(dto.getPhoneNumber());

            Customer savedCustomer;
            // Handle different cases
            if (existingByZohoId.isPresent()) {
                // Update existing record by Zoho ID
                Customer customer = existingByZohoId.get();
                updateCustomerFromDto(customer, dto);
                savedCustomer = customerRepository.save(customer);
                stats.successCount++;
                //System.out.println("Updated existing customer by Zoho ID: " + dto.getEmail());
            } 
            else if (existingByPhone.isPresent()) {
                Customer existingCustomer =existingByPhone.get();
                
                if (existingCustomer.getZohoCrmId() == null) {
                    updateCustomerFromDto(existingCustomer, dto);
                    existingCustomer.setZohoCrmId(dto.getZohoCrmId());
                    savedCustomer = customerRepository.save(existingCustomer);
                    stats.successCount++;
                    //System.out.println("Linked existing customer with Zoho ID: " + dto.getEmail());
                } else {
                    throw new RuntimeException(String.format(
                        "Duplicate %s found. Existing customer: %s, Zoho ID: %s",
                       "phone number",
                        existingCustomer.getEmail(),
                        existingCustomer.getZohoCrmId()));
                }
            }
            else {
                // Create new customer
                Customer newCustomer = new Customer();
                newCustomer.setCustId(customerIdGenerator.generateCustomerId());
                updateCustomerFromDto(newCustomer, dto);
                newCustomer.setZohoCrmId(dto.getZohoCrmId());
                
                // Set default owner if not present
                adminUserRepository.findByUsername("mithun").ifPresent(admin -> {
                    newCustomer.setOwner(admin);
                    newCustomer.setCreatedBy(admin);
                });
                savedCustomer = customerRepository.save(newCustomer);
                stats.successCount++;
                //System.out.println("Created new customer: " + dto.getEmail());
            }
        } catch (Exception e) {
            stats.failureCount++;
            stats.errors.add(String.format("Failed to process customer %s: %s", dto.getEmail(), e.getMessage()));
            throw e; // Re-throw to trigger transaction rollback for this record only
        }
    }

    /**
     * Validate required customer data
     */
    private void validateCustomerData(CustomerResponseDto dto) {
        List<String> validationErrors = new ArrayList<>();
        
        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            validationErrors.add("Full name is required");
        }
        
        // Validate email
        // if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
        //     validationErrors.add("Email is required");
        // } else if (!dto.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
        //     validationErrors.add("Invalid email format");
        // }
        
        // // Validate phone number
        // if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty()) {
        //     validationErrors.add("Phone number is required");
        // } else if (!dto.getPhoneNumber().matches("^\\d{10}$")) {
        //     validationErrors.add("Phone number must be 10 digits");
        // }
        
        // if (!validationErrors.isEmpty()) {
        //     throw new RuntimeException("Validation failed: " + String.join(", ", validationErrors));
        // }
    }

    /**
     * Update existing customer from DTO
     */
    private void updateCustomerFromDto(Customer customer, CustomerResponseDto dto) {
        customer.setFullName(dto.getFullName());
        customer.setEmail(dto.getEmail());
        customer.setPhoneNumber(dto.getPhoneNumber());
        customer.setCity(dto.getCity());
        customer.setState(dto.getState());
        customer.setOccupation(dto.getOccupation());
        customer.setAnnualIncome(dto.getAnnualIncome());
        customer.setStatus(dto.getStatus());
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setCustId(customerIdGenerator.generateCustomerId());
        customer.setDateOfBirth(dto.getDateOfBirth());
        customer.setGender(dto.getGender());

        // Set default owner if not present
    }

    /**
     * Map DTO to new Customer entity
     */
    private Customer mapDtoToCustomer(CustomerResponseDto dto) {
        Customer customer = new Customer();
        customer.setZohoCrmId(dto.getZohoCrmId());
        customer.setFullName(dto.getFullName());
        customer.setEmail(dto.getEmail());
        customer.setPhoneNumber(dto.getPhoneNumber());
        customer.setCity(dto.getCity());
        customer.setState(dto.getState());
        customer.setOccupation(dto.getOccupation());
        customer.setAnnualIncome(dto.getAnnualIncome());
        customer.setStatus(dto.getStatus());
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        
        // Set default values for required fields
        customer.setDateOfBirth(LocalDate.now());
        customer.setGender(dto.getGender());
        customer.setDependentCount(0);
        
        // Set default owner and created by
        adminUserRepository.findByUsername(dto.getOwner()).ifPresent(admin -> {
            customer.setOwner(admin);
            customer.setCreatedBy(admin);
        });
        
        // Generate customer ID if not present
        if (customer.getCustId() == null) {
            customer.setCustId(customerIdGenerator.generateCustomerId());
        }
        
        return customer;
    }

    /**
     * Sync data from local database to Zoho CRM
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private SyncStats syncFromDbToZoho() throws ZohoSyncException {
        SyncStats stats = new SyncStats();
        List<Customer> customers = customerRepository.findAllByZohoCrmIdIsNull();
        
        //System.out.println("Found " + customers.size() + " customers to sync to Zoho CRM");

        for (Customer customer : customers) {
            try {
                //System.out.println("Attempting to sync customer: " + customer.getEmail());
                if (handleDbToZohoSync(customer)) {
                    stats.successCount++;
                    //System.out.println("Successfully synced customer: " + customer.getEmail());
                }
            } catch (Exception e) {
                stats.failureCount++;
                String errorMsg = String.format("Failed to sync customer %s to Zoho: %s", 
                    customer.getEmail(), e.getMessage());
                logger.error(errorMsg);
                stats.errors.add(errorMsg);
                // Continue with next record
            }
        }

        //System.out.println("Sync completed. Success: " + stats.successCount + ", Failures: " + stats.failureCount);
        return stats;
    }

    /**
     * Handle synchronization of a single record from Zoho to DB
     */
    private boolean handleZohoToDbSync(CustomerResponseDto zohoDto) throws ZohoSyncException {
        Optional<Customer> existingCustomer = customerRepository.findByZohoCrmId(zohoDto.getZohoCrmId());

        if (existingCustomer.isPresent()) {
            return handleConflict(existingCustomer.get(), zohoDto);
        } else {
            return zohoSyncUtil.syncCustomer(zohoDto);
        }
    }

    /**
     * Handle synchronization of a single record from DB to Zoho
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private boolean handleDbToZohoSync(Customer customer) throws ZohoSyncException {
        try {
            Record zohoRecord = zohoSyncUtil.convertToZohoRecord(customer);
            String zohoId = zohoSyncUtil.upsertToZoho(zohoRecord, customer, syncConfig.getConflictStrategy());
            
            // Update local record with Zoho ID
            customer.setZohoCrmId(zohoId);
            customerRepository.save(customer);
            return true;
        } catch (Exception e) {
            throw new ZohoSyncException("Failed to sync to Zoho: " + e.getMessage(), e);
        }
    }

    /**
     * Handle conflicts between Zoho and DB records based on configured strategy
     */
    private boolean handleConflict(Customer dbCustomer, CustomerResponseDto zohoDto) throws ZohoSyncException {
        switch (syncConfig.getConflictStrategy()) {
            case ZOHO_FIRST:
                return zohoSyncUtil.syncCustomer(zohoDto);
                
            case DB_FIRST:
                return true; // Keep DB version
                
            case LATEST_WINS:
                LocalDateTime zohoUpdateTime = zohoDto.getUpdatedAt();
                LocalDateTime dbUpdateTime = dbCustomer.getUpdatedAt();
                
                if (zohoUpdateTime != null && zohoUpdateTime.isAfter(dbUpdateTime)) {
                    return zohoSyncUtil.syncCustomer(zohoDto);
                }
                return true;
                
            case MANUAL:
                // Log conflict for manual resolution
                throw new ZohoSyncException("Manual conflict resolution required for customer: " + dbCustomer.getCustId());
                
            default:
                throw new ZohoSyncException("Unknown conflict resolution strategy");
        }
    }



    public static class SyncResult {
        public SyncStats zohoToDbResult = new SyncStats();
        public SyncStats dbToZohoResult = new SyncStats();
    }

    public static class SyncStats {
        public int successCount = 0;
        public int failureCount = 0;
        public List<String> errors = new ArrayList<>();
    }
} 