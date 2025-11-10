package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vimainsurance.vimaadmin.entity.Nominee;

public interface INomineeRepository extends JpaRepository<Nominee, UUID> {

    List<Nominee> findByPolicyPolicyId(Long policyId);

    List<Nominee> findByPolicyPolicyNumber(String policyNumber);
}

