package com.vimainsurance.vimaadmin.audit;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.audit.entity.AuditEvent;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
}
