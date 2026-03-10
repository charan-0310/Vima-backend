package com.vimainsurance.vimaadmin.scheduler;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.entity.CostSharingRule;
import com.vimainsurance.vimaadmin.repository.ICostSharingRuleRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Daily job at midnight: validates cost-sharing rules — detects orphaned rules (company no longer
 * exists) and conflicting rules (overlapping effective dates, same company/plan/category). Logs
 * warnings only; no destructive changes.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "cost-sharing.validator.job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CostSharingRuleValidatorJob {

    @Autowired
    private ICostSharingRuleRepository costSharingRuleRepository;
    @Autowired
    private IOrganizationRepository organizationRepository;

    @Scheduled(cron = "${cost-sharing.validator.job.cron:0 0 0 * * *}", zone = "UTC")
    public void run() {
        log.debug("CostSharingRuleValidatorJob: starting");
        List<CostSharingRule> all = costSharingRuleRepository.findAll();
        for (CostSharingRule rule : all) {
            UUID companyId = rule.getCompanyId();
            if (companyId != null && organizationRepository.findById(companyId).isEmpty()) {
                log.warn("CostSharingRuleValidatorJob: orphaned rule id={} companyId={} (company not found)", rule.getId(), companyId);
            }
        }
        List<UUID> companyIds = all.stream().map(CostSharingRule::getCompanyId).distinct().filter(java.util.Objects::nonNull).toList();
        LocalDate today = LocalDate.now();
        for (UUID companyId : companyIds) {
            List<CostSharingRule> companyRules = costSharingRuleRepository.findActiveRulesForCompany(companyId, today);
            for (int i = 0; i < companyRules.size(); i++) {
                CostSharingRule a = companyRules.get(i);
                for (int j = i + 1; j < companyRules.size(); j++) {
                    CostSharingRule b = companyRules.get(j);
                    if (a.getPlanType() != null && a.getPlanType().equals(b.getPlanType())
                            && a.getCoverageCategory() == b.getCoverageCategory()
                            && overlapping(a.getEffectiveFrom(), a.getEffectiveTo(), b.getEffectiveFrom(), b.getEffectiveTo())) {
                        log.warn("CostSharingRuleValidatorJob: possible conflicting rules companyId={} planType={} category={} ruleIds={},{}",
                                companyId, a.getPlanType(), a.getCoverageCategory(), a.getId(), b.getId());
                    }
                }
            }
        }
        log.debug("CostSharingRuleValidatorJob: completed");
    }

    private static boolean overlapping(LocalDate from1, LocalDate to1, LocalDate from2, LocalDate to2) {
        LocalDate end1 = to1 != null ? to1 : LocalDate.MAX;
        LocalDate end2 = to2 != null ? to2 : LocalDate.MAX;
        return from1.isBefore(end2) && from2.isBefore(end1);
    }
}
