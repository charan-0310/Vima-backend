package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.entity.CdAccount;

public interface ICdAccountService {
    CdAccount getOrCreateDefaultAccount(UUID organizationId, String insurerName);

    List<CdAccount> listAccountsByOrganization(UUID organizationId);

    void linkPolicyToAccount(Long policyId, UUID cdAccountId);
}
