package com.vimainsurance.vimaadmin.service;

import java.util.UUID;

/**
 * Populates cpc.payroll_deduction_schedules from approved enrollment submissions.
 */
public interface IPayrollSchedulePopulationService {

    /**
     * Create payroll deduction schedule rows for an approved enrollment submission.
     * Uses submission's cost-sharing snapshot and deduction fields; falls back to
     * submission-level totals (one row per employee) if no breakdown exists.
     *
     * @param submissionId approved enrollment submission id
     */
    void populateFromEnrollmentSubmission(UUID submissionId);
}
