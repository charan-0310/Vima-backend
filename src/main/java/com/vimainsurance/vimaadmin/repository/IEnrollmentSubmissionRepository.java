package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

@Repository
public interface IEnrollmentSubmissionRepository extends JpaRepository<EnrollmentSubmission, UUID>, JpaSpecificationExecutor<EnrollmentSubmission> {

    Optional<EnrollmentSubmission> findByReferenceNumber(String referenceNumber);

    Optional<EnrollmentSubmission> findByIdempotencyKey(String idempotencyKey);

    Optional<EnrollmentSubmission> findByInvitation_Id(UUID invitationId);

    Optional<EnrollmentSubmission> findByEmployee_IndividualIdAndEnrollmentWindow_Id(UUID employeeId, UUID enrollmentWindowId);

    List<EnrollmentSubmission> findAllByEnrollmentWindow_Id(UUID enrollmentWindowId);

    /** Batch load submissions for multiple windows (for list progress). */
    List<EnrollmentSubmission> findAllByEnrollmentWindow_IdIn(Iterable<UUID> enrollmentWindowIds);

    List<EnrollmentSubmission> findAllByEnrollmentWindow_IdAndStatus(UUID enrollmentWindowId, EnrollementStatus status);

    List<EnrollmentSubmission> findAllByEmployee_IndividualId(UUID employeeId);

    boolean existsByEmployee_IndividualIdAndEnrollmentWindow_Id(UUID employeeId, UUID enrollmentWindowId);
}

