package com.vimainsurance.vimaadmin.audit;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.audit.entity.AuditPending;

@Repository
public interface AuditPendingRepository extends JpaRepository<AuditPending, UUID> {
}
