package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

@Repository
public interface IEnrollmentInvitationRepository extends JpaRepository<EnrollmentInvitation, UUID> {

    Optional<EnrollmentInvitation> findByTokenHash(String tokenHash);

    @Query("SELECT i FROM EnrollmentInvitation i JOIN FETCH i.enrollmentWindow JOIN FETCH i.employee WHERE i.tokenHash = :tokenHash")
    Optional<EnrollmentInvitation> findByTokenHashWithWindowAndEmployee(@Param("tokenHash") String tokenHash);

    Optional<EnrollmentInvitation> findByEmployee_IndividualIdAndEnrollmentWindow_Id(UUID employeeId, UUID enrollmentWindowId);

    List<EnrollmentInvitation> findAllByEnrollmentWindow_Id(UUID enrollmentWindowId);

    Page<EnrollmentInvitation> findAllByEnrollmentWindow_Id(UUID enrollmentWindowId, Pageable pageable);

    List<EnrollmentInvitation> findAllByEnrollmentWindow_IdAndStatus(UUID enrollmentWindowId, EnrollementStatus status);

    Page<EnrollmentInvitation> findAllByEnrollmentWindow_IdAndStatus(UUID enrollmentWindowId, EnrollementStatus status, Pageable pageable);

    List<EnrollmentInvitation> findAllByEmployee_IndividualId(UUID employeeId);

    boolean existsByEmployee_IndividualIdAndEnrollmentWindow_Id(UUID employeeId, UUID enrollmentWindowId);
}
