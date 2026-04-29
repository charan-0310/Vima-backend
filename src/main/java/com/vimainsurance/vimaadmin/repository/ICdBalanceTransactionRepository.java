package com.vimainsurance.vimaadmin.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.CdBalanceTransaction;

@Repository
public interface ICdBalanceTransactionRepository
        extends JpaRepository<CdBalanceTransaction, UUID>, JpaSpecificationExecutor<CdBalanceTransaction> {

    List<CdBalanceTransaction> findByEndorsement_EndorsementId(UUID endorsementId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CdBalanceTransaction t WHERE t.cdAccountId = :cdAccountId")
    BigDecimal sumAmountByCdAccountId(@Param("cdAccountId") UUID cdAccountId);
}
