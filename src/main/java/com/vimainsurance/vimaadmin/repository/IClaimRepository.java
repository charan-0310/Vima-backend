package com.vimainsurance.vimaadmin.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;

@Repository
public interface IClaimRepository extends JpaRepository<Claim, UUID>, JpaSpecificationExecutor<Claim>, IClaimRepositoryCustom {

    /** Single query with fetch of organization, employee, settlement to avoid N+1 on detail load. */
    @Query("SELECT DISTINCT c FROM Claim c "
           + "LEFT JOIN FETCH c.organization "
           + "LEFT JOIN FETCH c.employee "
           + "LEFT JOIN FETCH c.settlement "
           + "WHERE c.id = :id")
    Optional<Claim> findByIdWithOrganizationAndEmployeeAndSettlement(@Param("id") UUID id);

    List<Claim> findByEmployee_IndividualId(UUID employeeId);

    List<Claim> findByOrganization_OrganizationId(UUID organizationId);

    List<Claim> findByInternalStatus(ClaimStatus internalStatus);

    Optional<Claim> findByClaimNumber(String claimNumber);

    long countByOrganization_OrganizationIdAndInternalStatus(UUID organizationId, ClaimStatus internalStatus);

    @Query("SELECT c FROM Claim c WHERE c.organization.organizationId = :organizationId "
           + "AND c.dateOfSubmission BETWEEN :startDate AND :endDate AND c.isDeleted = false")
    List<Claim> findByOrganizationIdAndDateRange(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(c.claimAmount), 0) FROM Claim c WHERE c.organization.organizationId = :orgId AND c.isDeleted = false")
    BigDecimal sumClaimAmountByOrganization(@Param("orgId") UUID orgId);

    @Query("SELECT COALESCE(SUM(s.amountPaid), 0) FROM Claim c JOIN c.settlement s WHERE c.organization.organizationId = :orgId "
           + "AND c.isDeleted = false AND c.internalStatus = com.vimainsurance.vimaadmin.enums.ClaimStatus.SETTLED")
    BigDecimal sumSettledAmountByOrganization(@Param("orgId") UUID orgId);

    @Query("SELECT COALESCE(SUM(c.claimAmount), 0) FROM Claim c WHERE c.organization.organizationId IN :orgIds AND c.isDeleted = false")
    BigDecimal sumClaimAmountByOrganizationIdIn(@Param("orgIds") List<UUID> orgIds);

    @Query("SELECT COALESCE(SUM(s.amountPaid), 0) FROM Claim c JOIN c.settlement s WHERE c.organization.organizationId IN :orgIds "
           + "AND c.isDeleted = false AND c.internalStatus = com.vimainsurance.vimaadmin.enums.ClaimStatus.SETTLED")
    BigDecimal sumSettledAmountByOrganizationIdIn(@Param("orgIds") List<UUID> orgIds);

    /** Sum of settlement amountPaid for current employee's claims with status SETTLED. */
    @Query("SELECT COALESCE(SUM(s.amountPaid), 0) FROM Claim c JOIN c.settlement s WHERE c.employee.individualId = :employeeId "
           + "AND c.isDeleted = false AND c.internalStatus = com.vimainsurance.vimaadmin.enums.ClaimStatus.SETTLED")
    BigDecimal sumSettledAmountByEmployeeId(@Param("employeeId") UUID employeeId);

    /** Count claims for current employee (non-deleted). */
    long countByEmployee_IndividualIdAndIsDeleted(UUID employeeId, boolean isDeleted);
}
