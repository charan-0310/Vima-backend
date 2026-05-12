package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.entity.CdAccount;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.CdAccountStatus;
import com.vimainsurance.vimaadmin.repository.ICdAccountRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.ICdAccountService;

@Service
public class CdAccountServiceImpl implements ICdAccountService {

    @Autowired
    private ICdAccountRepository cdAccountRepository;

    @Autowired
    private IPolicyRepository policyRepository;

    @Autowired
    private IInsuranceProviderRepository insuranceProviderRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdAccount getOrCreateDefaultAccount(UUID organizationId, String insurerName) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        if (insurerName == null || insurerName.isBlank()) {
            throw new IllegalArgumentException("Insurer name is required");
        }
        String normalizedInsurerName = insurerName.trim();

        return cdAccountRepository
                .findByOrganizationIdAndInsurerNameAndLabelIsNull(organizationId, normalizedInsurerName)
                .orElseGet(() -> {
                    CdAccount account = new CdAccount();
                    account.setOrganizationId(organizationId);
                    account.setInsurerName(normalizedInsurerName);
                    account.setLabel(null);
                    account.setStatus(CdAccountStatus.ACTIVE);
                    return cdAccountRepository.save(account);
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "cd_accounts", entityType = "CD_ACCOUNT", action = "CREATE_AND_LINK_POLICY")
    public CdAccount createDefaultAccountAndLinkPolicy(Long policyId) {
        Policy policy = policyRepository.findByIdForUpdate(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found"));
        if (policy.getOrganizationId() == null) {
            throw new IllegalArgumentException("Policy organization is required");
        }
        if (policy.getCdAccountId() != null) {
            return cdAccountRepository.findById(policy.getCdAccountId())
                    .orElseThrow(() -> new RuntimeException("CD account not found for policy"));
        }
        String insurerName = resolveInsurerNameForCd(policy);
        if (insurerName == null || insurerName.isBlank()) {
            throw new IllegalArgumentException(
                    "Cannot create CD account: policy must have insurer name or a linked insurance provider");
        }
        CdAccount account = getOrCreateDefaultAccount(policy.getOrganizationId(), insurerName);
        linkPolicyToAccount(policyId, account.getCdAccountId());
        return account;
    }

    private String resolveInsurerNameForCd(Policy policy) {
        if (policy.getInsurerName() != null && !policy.getInsurerName().isBlank()) {
            return policy.getInsurerName().trim();
        }
        if (policy.getInsuranceProviderId() != null) {
            return insuranceProviderRepository.findById(policy.getInsuranceProviderId())
                    .map(p -> p.getProviderName() != null ? p.getProviderName().trim() : null)
                    .orElse(null);
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CdAccount> listAccountsByOrganization(UUID organizationId) {
        return cdAccountRepository.findByOrganizationId(organizationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "cd_accounts", entityType = "CD_ACCOUNT", action = "LINK_POLICY")
    public void linkPolicyToAccount(Long policyId, UUID cdAccountId) {
        Policy policy = policyRepository.findByIdForUpdate(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found"));
        CdAccount account = cdAccountRepository.findById(cdAccountId)
                .orElseThrow(() -> new RuntimeException("CD account not found"));
        if (policy.getOrganizationId() == null || !policy.getOrganizationId().equals(account.getOrganizationId())) {
            throw new RuntimeException("Policy and CD account belong to different organizations");
        }
        policy.setCdAccountId(cdAccountId);
        policyRepository.save(policy);
    }
}
