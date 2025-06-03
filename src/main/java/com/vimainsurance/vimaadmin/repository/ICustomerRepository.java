package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Customer;

public interface ICustomerRepository extends JpaRepository<Customer, UUID>{
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByCustId(String custId);
    List<Customer> findByCreatedBy(AdminUser user);
    @Query("SELECT MAX(c.custId) FROM Customer c WHERE c.custId LIKE 'C%'")
    String findMaxCustomerId();
}
