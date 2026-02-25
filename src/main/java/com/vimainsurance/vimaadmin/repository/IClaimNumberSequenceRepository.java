package com.vimainsurance.vimaadmin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.ClaimNumberSequence;

import jakarta.persistence.LockModeType;

@Repository
public interface IClaimNumberSequenceRepository extends JpaRepository<ClaimNumberSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ClaimNumberSequence> findByClaimYear(Integer claimYear);
}
