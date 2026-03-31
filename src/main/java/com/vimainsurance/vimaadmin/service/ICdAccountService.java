package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.entity.CdAccount;

public interface ICdAccountService {
    CdAccount getOrCreateDefaultAccount(UUID organizationId, String insurerName);

    /**
     * Creates the org’s default CD account for the policy’s insurer (if needed) and sets {@code policy.cd_account_id}.
     */
    CdAccount createDefaultAccountAndLinkPolicy(Long policyId);

    List<CdAccount> listAccountsByOrganization(UUID organizationId);

    void linkPolicyToAccount(Long policyId, UUID cdAccountId);
}
