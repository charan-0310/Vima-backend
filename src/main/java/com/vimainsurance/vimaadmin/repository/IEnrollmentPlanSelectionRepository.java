package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.EnrollmentPlanSelection;

@Repository
public interface IEnrollmentPlanSelectionRepository extends JpaRepository<EnrollmentPlanSelection, UUID> {

    List<EnrollmentPlanSelection> findByEnrollmentSubmissionId(UUID enrollmentSubmissionId);

    List<EnrollmentPlanSelection> findByEnrollmentSubmission_Id(UUID enrollmentSubmissionId);
}
