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
    
    // Manager Dashboard Analytics Methods
    
    @Query(value = """
    SELECT COUNT(DISTINCT c.id) 
    FROM admin.customers c
    INNER JOIN admin.admin_users au ON c.owner = au.id
    WHERE au.reporting_to = :managerId 
    AND c.created_at >= :startDate 
    AND c.created_at <= :endDate
    """, nativeQuery = true)
    Long countTotalLeadsForManager(@Param("managerId") UUID managerId, 
                                  @Param("startDate") java.time.LocalDateTime startDate,
                                  @Param("endDate") java.time.LocalDateTime endDate);
    
    @Query(value = """
    SELECT COUNT(DISTINCT q.id) 
    FROM admin.quotes q
    INNER JOIN admin.customers c ON q.customer_id = c.id
    INNER JOIN admin.admin_users au ON c.owner = au.id
    WHERE au.reporting_to = :managerId 
    AND q.created_date >= :startDate 
    AND q.created_date <= :endDate
    """, nativeQuery = true)
    Long countTotalQuotesForManager(@Param("managerId") UUID managerId,
                                   @Param("startDate") java.time.LocalDate startDate,
                                   @Param("endDate") java.time.LocalDate endDate);
    
    @Query(value = """
    SELECT COUNT(DISTINCT q.customer_id) 
    FROM admin.quotes q
    INNER JOIN admin.customers c ON q.customer_id = c.id
    INNER JOIN admin.admin_users au ON c.owner = au.id
    WHERE au.reporting_to = :managerId 
    AND q.status = 'POLICY_ISSUED'
    AND q.created_date >= :startDate 
    AND q.created_date <= :endDate
    """, nativeQuery = true)
    Long countTotalPoliciesForManager(@Param("managerId") UUID managerId,
                                     @Param("startDate") java.time.LocalDate startDate,
                                     @Param("endDate") java.time.LocalDate endDate);
    
    @Query(value = """
    SELECT q.best_premium 
    FROM admin.quotes q
    INNER JOIN admin.customers c ON q.customer_id = c.id
    INNER JOIN admin.admin_users au ON c.owner = au.id
    WHERE au.reporting_to = :managerId 
    AND q.status = 'POLICY_ISSUED'
    AND q.created_date >= :startDate 
    AND q.created_date <= :endDate
    AND q.best_premium IS NOT NULL
    AND q.best_premium != ''
    """, nativeQuery = true)
    List<String> getPolicyPremiumsForManager(@Param("managerId") UUID managerId,
                                           @Param("startDate") java.time.LocalDate startDate,
                                           @Param("endDate") java.time.LocalDate endDate);
    
    @Query(value = """
    SELECT COUNT(DISTINCT u.id) 
    FROM admin.admin_users u
    WHERE u.reporting_to = :managerId 
    AND u.is_active = true
    """, nativeQuery = true)
    Long countActiveAgentsForManager(@Param("managerId") UUID managerId);
    
    @Query(value = """
    SELECT u.username, u.full_name,
           COUNT(DISTINCT c.id) as leads,
           COUNT(DISTINCT q.id) as quotes,
           0 as policies,
           0 as businessAmount
    FROM admin.admin_users u
    LEFT JOIN admin.customers c ON c.owner = u.id 
        AND c.created_at >= :startDate 
        AND c.created_at <= :endDate
    LEFT JOIN admin.quotes q ON q.customer_id = c.id 
        AND q.created_date >= :startDate 
        AND q.created_date <= :endDate
    WHERE u.reporting_to = :managerId
    GROUP BY u.id, u.username, u.full_name
    ORDER BY leads DESC
    """, nativeQuery = true)
    List<Object[]> getAgentMetricsForManager(@Param("managerId") UUID managerId,
                                            @Param("startDate") java.time.LocalDateTime startDate,
                                            @Param("endDate") java.time.LocalDateTime endDate);
    
    @Query(value = """
    SELECT au.username, COUNT(DISTINCT q.id) as quoteCount
    FROM admin.quotes q
    INNER JOIN admin.customers c ON q.customer_id = c.id
    INNER JOIN admin.admin_users au ON c.owner = au.id
    WHERE au.reporting_to = :managerId 
    AND q.created_date >= :startDate 
    AND q.created_date <= :endDate
    GROUP BY au.username
    """, nativeQuery = true)
    List<Object[]> getAgentQuoteCounts(@Param("managerId") UUID managerId,
                                      @Param("startDate") java.time.LocalDate startDate,
                                      @Param("endDate") java.time.LocalDate endDate);
    
    @Query(value = """
    SELECT au.username, COUNT(DISTINCT q.customer_id) as policyCount
    FROM admin.quotes q
    INNER JOIN admin.customers c ON q.customer_id = c.id
    INNER JOIN admin.admin_users au ON c.owner = au.id
    WHERE au.reporting_to = :managerId 
    AND q.status = 'POLICY_ISSUED'
    AND q.created_date >= :startDate 
    AND q.created_date <= :endDate
    GROUP BY au.username
    """, nativeQuery = true)
    List<Object[]> getAgentPolicyCounts(@Param("managerId") UUID managerId,
                                       @Param("startDate") java.time.LocalDate startDate,
                                       @Param("endDate") java.time.LocalDate endDate);

}
