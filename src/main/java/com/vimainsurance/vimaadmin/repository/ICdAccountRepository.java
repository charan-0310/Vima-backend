package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.CdAccount;

import jakarta.persistence.LockModeType;

@Repository
public interface ICdAccountRepository extends JpaRepository<CdAccount, UUID> {
    List<CdAccount> findByOrganizationId(UUID organizationId);

    Optional<CdAccount> findByOrganizationIdAndInsurerNameAndLabelIsNull(UUID organizationId, String insurerName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CdAccount c WHERE c.cdAccountId = :cdAccountId")
    Optional<CdAccount> findByIdForUpdate(@Param("cdAccountId") UUID cdAccountId);
}
