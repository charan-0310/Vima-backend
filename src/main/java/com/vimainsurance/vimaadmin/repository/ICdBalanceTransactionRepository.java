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

    @Query(value = """
        select
            t.policy_id,
            coalesce(sum(case when t.transaction_type::text = 'ENDORSEMENT_CREDIT' then coalesce(t.amount, 0) else 0 end), 0) as total_endorsement_credit,
            coalesce(sum(case when t.transaction_type::text = 'ENDORSEMENT_DEBIT' then abs(coalesce(t.amount, 0)) else 0 end), 0) as total_endorsement_debit,
            max(t.updated_at) as last_transaction_updated_at
        from cpc.cd_balance_transactions t
        where t.organization_id = :organizationId
          and t.policy_id is not null
          and t.policy_id in (:policyIds)
          and t.transaction_type::text in ('ENDORSEMENT_CREDIT', 'ENDORSEMENT_DEBIT')
        group by t.policy_id
        """, nativeQuery = true)
    List<Object[]> getEndorsementTransactionSummaryByOrganizationAndPolicyIds(
            @Param("organizationId") UUID organizationId,
            @Param("policyIds") List<Long> policyIds);
}
