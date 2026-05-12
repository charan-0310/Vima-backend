package com.vimainsurance.vimaadmin.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /**
     * Single-row aggregates for ledger filters (same semantics as
     * {@link com.vimainsurance.vimaadmin.specification.CdBalanceTransactionSpecification#ledgerByCdAccount}).
     * Columns: totalDeposited, totalUtilized, endorsementCreditAbsSum, endorsementDebitAbsSum,
     * endorsementRowCount.
     */
    @Query(value = """
            SELECT
                coalesce(sum(case when t.amount > 0 then t.amount else 0 end), 0),
                coalesce(sum(case when t.amount < 0 then -t.amount else 0 end), 0),
                coalesce(sum(case when t.transaction_type::text = 'ENDORSEMENT_CREDIT' then abs(t.amount) else 0 end), 0),
                coalesce(sum(case when t.transaction_type::text = 'ENDORSEMENT_DEBIT' then abs(t.amount) else 0 end), 0),
                coalesce(sum(case when t.transaction_type::text in ('ENDORSEMENT_CREDIT', 'ENDORSEMENT_DEBIT') then 1 else 0 end), 0)
            FROM cpc.cd_balance_transactions t
            WHERE t.cd_account_id = :cdAccountId
              AND (NOT :applyPolicyFilter OR t.policy_id = :policyId)
              AND (NOT :applyTypeFilter OR t.transaction_type::text = :txTypeStr)
              AND (NOT :applyFromFilter OR t.created_at >= :dateFrom)
              AND (NOT :applyToFilter OR t.created_at <= :dateTo)
            """, nativeQuery = true)
    List<Object[]> aggregateLedgerFiltered(
            @Param("cdAccountId") UUID cdAccountId,
            @Param("applyPolicyFilter") boolean applyPolicyFilter,
            @Param("policyId") Long policyId,
            @Param("applyTypeFilter") boolean applyTypeFilter,
            @Param("txTypeStr") String txTypeStr,
            @Param("applyFromFilter") boolean applyFromFilter,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("applyToFilter") boolean applyToFilter,
            @Param("dateTo") LocalDateTime dateTo);

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
