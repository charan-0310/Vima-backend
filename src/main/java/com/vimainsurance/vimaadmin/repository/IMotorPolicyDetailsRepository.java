package com.vimainsurance.vimaadmin.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vimainsurance.vimaadmin.entity.MotorPolicyDetails;

public interface IMotorPolicyDetailsRepository extends JpaRepository<MotorPolicyDetails, UUID> {

    Optional<MotorPolicyDetails> findByPolicyPolicyId(Long policyId);
}

