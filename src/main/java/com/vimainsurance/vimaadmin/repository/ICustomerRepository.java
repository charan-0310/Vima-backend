package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Customer;

@Repository
public interface ICustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByCustId(String custId);
    Optional<Customer> findByZohoCrmId(String zohoCrmId);
    List<Customer> findByCreatedBy(AdminUser user);
    Page<Customer> findAll(Pageable pageable);
    @Query("SELECT MAX(c.custId) FROM Customer c WHERE c.custId LIKE 'C%'")
    String findMaxCustomerId();
    List<Customer> findAllByZohoCrmIdIsNull();
}
