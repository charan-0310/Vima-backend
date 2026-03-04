package com.vimainsurance.vimaadmin.audit;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.audit.entity.AuditPending;

@Repository
public interface AuditPendingRepository extends JpaRepository<AuditPending, UUID> {

    /**
     * Find pending audit events eligible for replay (status PENDING and retry count below threshold).
     * Ordered by created_at ascending so oldest failures are replayed first.
     */
    Page<AuditPending> findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            String status, int retryCountLimit, Pageable pageable);
}
