package com.vimainsurance.vimaadmin.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Organization;

@Repository
public interface IAdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByUsername(String username);
    Optional<AdminUser> findByEmail(String email);
    Optional<AdminUser> findByUsernameIgnoreCase(String username);
    /**
     * Deterministic single row when duplicate emails exist (otherwise Spring Data {@code findBy…} can throw
     * {@link org.springframework.dao.IncorrectResultSizeDataAccessException}).
     */
    Optional<AdminUser> findFirstByEmailIgnoreCaseOrderByCreatedAtAsc(String email);
    Optional<AdminUser> findByOauthProviderId(String oauthProviderId);
    List<AdminUser> findByIsActiveTrue();

    /**
     * Loads {@code organization} in the same round-trip (HR notification inbox and similar).
     */
    @EntityGraph(attributePaths = { "organization" })
    @Query("SELECT u FROM AdminUser u WHERE u.id = :id")
    Optional<AdminUser> findWithOrganizationById(@Param("id") UUID id);

    List<AdminUser> findByRole(String role);

    List<AdminUser> findByRoleInAndIsActiveTrue(Collection<String> roles);
    @Query("SELECT MAX(CAST(SUBSTRING(c.agentId, 5) AS INTEGER))  FROM AdminUser c WHERE c.agentId LIKE 'VIMA%'")
    String findMaxAgentId();
    @Query(value = "SELECT nextval('admin.agent_id_seq')", nativeQuery = true)
    Long getNextAgentSeq();

    List<AdminUser> findByOrganizationAndIsDemoUserTrueAndIsActiveTrue(Organization organization);
    List<AdminUser> findByOrganizationAndIsDemoUserTrue(Organization organization);

    List<AdminUser> findByOrganization_OrganizationId(UUID organizationId);
    List<AdminUser> findByIsDemoUserTrueAndIsActiveTrueAndDemoExpiresAtBefore(LocalDateTime now);
    List<AdminUser> findByIsDemoUserTrueAndIsActiveTrueAndDemoExpiresAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Used when deleting demo org admins: {@code enrollment_windows.created_by} must not reference removed rows.
     */
    @Query(value = """
            SELECT id FROM admin.admin_users
            WHERE role IN ('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')
              AND id NOT IN (:excludeIds)
            ORDER BY created_at ASC NULLS LAST
            LIMIT 1
            """, nativeQuery = true)
    Optional<UUID> findFirstPlatformAdminIdExcluding(@Param("excludeIds") Collection<UUID> excludeIds);

}
