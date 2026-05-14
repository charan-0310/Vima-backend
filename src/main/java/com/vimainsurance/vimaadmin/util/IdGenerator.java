package com.vimainsurance.vimaadmin.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;

/**
 * Utility class for generating unique IDs
 */
@Component
public class IdGenerator {

    @Autowired
    private ICustomerRepository customerRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    /**
     * Generate a new customer ID
     * @return A unique customer ID in format "C{number}" with zero-padding
     */
    public String generateCustomerId() {
        Long maxId = customerRepository.getNextCustomerSeq();
        return "C" + maxId.toString();
    }


     /**
     * Generate a new VIMA ID
     * @return A unique VIMA ID in format "VIMA{number}" with zero-padding
     */
    public String generateVimaId() {
        Long maxId = adminUserRepository.getNextAgentSeq();
        return "VIMA" + maxId.toString();
    }


}
