package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.CdTransactionDocument;
import com.vimainsurance.vimaadmin.entity.CdTransactionDocumentId;

@Repository
public interface ICdTransactionDocumentRepository extends JpaRepository<CdTransactionDocument, CdTransactionDocumentId> {

    List<CdTransactionDocument> findByTransactionId(UUID transactionId);

    List<CdTransactionDocument> findByTransactionIdIn(List<UUID> transactionIds);
}
