package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vimainsurance.vimaadmin.entity.Nominee;

public interface INomineeRepository extends JpaRepository<Nominee, UUID> {

    List<Nominee> findByPolicyPolicyId(Long policyId);

    /** Nominees for a given policy and customer (employee) - for GTL/GPA in employee portal. */
    List<Nominee> findByPolicyPolicyIdAndCustomerIndividualId(Long policyId, UUID customerIndividualId);

    List<Nominee> findByPolicyPolicyNumber(String policyNumber);

    List<Nominee> findBySubmission_Id(UUID submissionId);
}

