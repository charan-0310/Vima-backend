package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

@Repository
public interface IEnrollmentInvitationRepository extends JpaRepository<EnrollmentInvitation, UUID> {

    Optional<EnrollmentInvitation> findByTokenHash(String tokenHash);

    Optional<EnrollmentInvitation> findByEmployee_IndividualIdAndEnrollmentWindow_Id(UUID employeeId, UUID enrollmentWindowId);

    List<EnrollmentInvitation> findAllByEnrollmentWindow_Id(UUID enrollmentWindowId);

    List<EnrollmentInvitation> findAllByEnrollmentWindow_IdAndStatus(UUID enrollmentWindowId, EnrollementStatus status);

    List<EnrollmentInvitation> findAllByEmployee_IndividualId(UUID employeeId);

    boolean existsByEmployee_IndividualIdAndEnrollmentWindow_Id(UUID employeeId, UUID enrollmentWindowId);
}
