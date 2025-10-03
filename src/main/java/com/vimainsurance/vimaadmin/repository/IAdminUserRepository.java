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
public interface IAdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByUsername(String username);
    Optional<AdminUser> findByEmail(String email);
    List<AdminUser> findByIsActiveTrue();
    List<AdminUser> findByRole(String role);
    @Query("SELECT MAX(CAST(SUBSTRING(c.agentId, 5) AS INTEGER))  FROM AdminUser c WHERE c.agentId LIKE 'VIMA%'")
    String findMaxAgentId();
    @Query(value = "SELECT nextval('admin.agent_id_seq')", nativeQuery = true)
    Long getNextAgentSeq();


    @Query("""
    SELECT l FROM Customer l
    WHERE l.owner.id = :managerId OR l.owner.reportingTo.id = :managerId
    """)
    Page<Customer> findLeadsForManagerAndAgents(@Param("managerId") UUID managerId, Pageable pageable);

    @Query("""
    SELECT DISTINCT l
    FROM Customer l
    WHERE (l.owner.id = :managerId OR l.owner.reportingTo.id = :managerId)
      AND (
          LOWER(l.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
          OR l.phoneNumber LIKE CONCAT('%', :search, '%')
      )
    """)
    Page<Customer> searchLeadsForManagerAndAgents(@Param("managerId") UUID managerId,
                                                @Param("search") String search,
                                                Pageable pageable);

    @Query("""
    SELECT a.username FROM AdminUser a
    WHERE a.reportingTo.id = :managerId
    """)
    List<String> findByReportingTo(@Param("managerId") UUID managerId);

    @Query("""
    SELECT DISTINCT l FROM Customer l
    LEFT JOIN l.quotes q
    WHERE (l.owner.id = :managerId OR l.owner.reportingTo.id = :managerId)
      AND (:ownerFilter IS NULL OR l.owner.username = :ownerFilter)
      AND (:search IS NULL OR :search = '' OR 
           LOWER(l.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR 
           l.phoneNumber LIKE CONCAT('%', :search, '%'))
    """)
    Page<Customer> findLeadsForManagerAndAgentsWithFilters(
        @Param("managerId") UUID managerId,
        @Param("ownerFilter") String ownerFilter,
        @Param("search") String search,
        Pageable pageable);

      
    @Query("""
    SELECT COUNT(l) FROM Quotes l
    WHERE l.customer.owner.id = :managerId OR l.customer.owner.reportingTo.id = :managerId AND l.status = 'Shared'
    """)
    Long countByQuoteSent(@Param("managerId") UUID managerId);   
    
    @Query("""
      SELECT COUNT(l) FROM Quotes l
      WHERE l.customer.owner.id = :managerId OR l.customer.owner.reportingTo.id = :managerId AND l.status = :status
      """)
      Long countByQuoteSentAll(@Param("managerId") UUID managerId, @Param("status") String status);  
    
    @Query("""
    SELECT COUNT(l) FROM Quotes l
    WHERE l.customer.owner.id = :agentId  AND l.status = :status
    """)
    Long countByQuoteSentAgentAndStatus(@Param("agentId") UUID agentId, @Param("status") String status); 

}
