package com.vimainsurance.vimaadmin.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

@Repository
public interface IEnrollmentWindowsRepository extends JpaRepository<EnrollmentWindows, UUID>, JpaSpecificationExecutor<EnrollmentWindows> {

    Optional<EnrollmentWindows> findByIdAndOrganization_OrganizationId(UUID id, UUID organizationId);

    List<EnrollmentWindows> findAllByOrganization_OrganizationId(UUID organizationId);

    List<EnrollmentWindows> findAllByOrganization_OrganizationIdAndStatus(UUID organizationId, EnrollementStatus status);

    /**
     * Single aggregation query for progress (employee count, invitation count, submitted count) per window.
     * Avoids loading full entity lists when building the window list with completion rate.
     */
    @Query(value = """
        SELECT ew.id AS "windowId",
          (SELECT COUNT(*) FROM cpc.customers c WHERE c.enrollment_window_id = ew.id) AS "employeeCount",
          (SELECT COUNT(*) FROM cpc.enrollment_invitations i WHERE i.enrollment_window_id = ew.id) AS "invitationCount",
          (SELECT COUNT(*) FROM cpc.enrollment_submissions s WHERE s.enrollment_window_id = ew.id
             AND s.status IN ('SUBMITTED','APPROVED','COMPLETED','ENDORSED','REJECTED')) AS "submittedCount"
        FROM cpc.enrollment_windows ew
        WHERE ew.id IN (:windowIds)
        """, nativeQuery = true)
    List<WindowProgressProjection> findProgressByWindowIds(@Param("windowIds") List<UUID> windowIds);

    /**
     * Find all enrollment windows that are SCHEDULED and have passed their end date.
     * Used by the expiry scheduler to mark them as EXPIRED.
     */
    List<EnrollmentWindows> findAllByStatusAndEndDateBefore(EnrollementStatus status, LocalDate date);

    /**
     * Update expired enrollment windows in bulk.
     * Updates status to EXPIRED for SCHEDULED windows where end_date < current date.
     * Returns the number of updated rows.
     */
    @Modifying
    @Query(value = """
        UPDATE cpc.enrollment_windows
        SET status = 'EXPIRED', updated_at = NOW()
        WHERE status = 'SCHEDULED' AND end_date < :currentDate
        """, nativeQuery = true)
    int markExpiredWindows(@Param("currentDate") LocalDate currentDate);
}

