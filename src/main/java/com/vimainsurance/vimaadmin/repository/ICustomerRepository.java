package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Customer;

@Repository
public interface ICustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByCustId(String custId);
    Optional<Customer> findByZohoCrmId(String zohoCrmId);
    // List<Customer> findByCreatedBy(AdminUser user);
    Page<Customer> findAll(Pageable pageable);
    @Query("SELECT MAX(CAST(SUBSTRING(c.custId, 2) AS INTEGER))  FROM Customer c WHERE c.custId LIKE 'C%'")
    String findMaxCustomerId();
    List<Customer> findAllByZohoCrmIdIsNull();
    List<Customer> findAllByCustIdIn(List<String> custIds);
    
    @Query(value = "SELECT nextval('admin.customer_id_seq')", nativeQuery = true)
    Long getNextCustomerSeq();

    @Query("""
        SELECT c FROM Customer c
        WHERE c.owner = :owner
          AND c.status <> 'INACTIVE'
        """)
    Page<Customer> findActiveByCreatedBy(@Param("owner") AdminUser owner, Pageable pageable);


    @Query("""
        SELECT c FROM Customer c
        WHERE c.owner = :owner
          AND c.status <> 'INACTIVE'
          AND (
              LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR c.phoneNumber LIKE CONCAT('%', :search, '%')
          )
        """)
        Page<Customer> searchCustomersByCreatedBy(@Param("owner") AdminUser owner,
                                                    @Param("search") String search,
                                                    Pageable pageable);
}
