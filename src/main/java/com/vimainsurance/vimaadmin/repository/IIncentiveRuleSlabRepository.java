package com.vimainsurance.vimaadmin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import com.vimainsurance.vimaadmin.entity.IncentiveRuleSlab;

public interface IIncentiveRuleSlabRepository extends JpaRepository<IncentiveRuleSlab, Long> {
    List<IncentiveRuleSlab> findByRuleIdOrderByMinValueAsc(Long ruleId);
    
    @Query("SELECT s FROM IncentiveRuleSlab s WHERE s.rule.id = :ruleId AND :value >= s.minValue AND (s.maxValue IS NULL OR :value <= s.maxValue)")
    List<IncentiveRuleSlab> findMatchingSlabs(@Param("ruleId") Long ruleId, @Param("value") java.math.BigDecimal value);
    
    @Modifying
    @Query("DELETE FROM IncentiveRuleSlab s WHERE s.rule.id = :ruleId")
    void deleteByRuleId(@Param("ruleId") Long ruleId);
} 