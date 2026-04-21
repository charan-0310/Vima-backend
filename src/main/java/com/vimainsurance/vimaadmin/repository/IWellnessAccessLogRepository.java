package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.WellnessAccessLog;

@Repository
public interface IWellnessAccessLogRepository extends JpaRepository<WellnessAccessLog, UUID> {

    @Query(value = "SELECT COUNT(DISTINCT wal.employee_id) FROM cpc.wellness_access_logs wal "
            + "WHERE wal.organization_id = :orgId AND wal.partner_id = :partnerId AND wal.status = 'SUCCESS'",
            nativeQuery = true)
    Long countUniqueEmployeesByOrgAndPartner(@Param("orgId") UUID orgId, @Param("partnerId") UUID partnerId);

    Page<WellnessAccessLog> findByOrganizationIdOrderByAccessedAtDesc(UUID orgId, Pageable pageable);

    @Query(value = "SELECT wal.partner_id, COUNT(*) as access_count, "
            + "COUNT(DISTINCT wal.employee_id) as unique_employees "
            + "FROM cpc.wellness_access_logs wal "
            + "WHERE wal.organization_id = :orgId AND wal.status = 'SUCCESS' "
            + "GROUP BY wal.partner_id", nativeQuery = true)
    List<Object[]> getAccessStatsByOrg(@Param("orgId") UUID orgId);

    @Query(value = "SELECT wal.partner_id, COUNT(*) as total_accesses "
            + "FROM cpc.wellness_access_logs wal "
            + "WHERE wal.organization_id = :orgId "
            + "GROUP BY wal.partner_id", nativeQuery = true)
    List<Object[]> getTotalAccessesByOrg(@Param("orgId") UUID orgId);

    @Query(value = "SELECT wal.partner_id, "
            + "SUM(CASE WHEN wal.status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count, "
            + "SUM(CASE WHEN wal.status = 'FAILED' THEN 1 ELSE 0 END) as failed_count "
            + "FROM cpc.wellness_access_logs wal "
            + "WHERE wal.organization_id = :orgId "
            + "GROUP BY wal.partner_id", nativeQuery = true)
    List<Object[]> getStatusCountsByOrg(@Param("orgId") UUID orgId);
}
