package com.vimainsurance.vimaadmin.util;

import com.vimainsurance.vimaadmin.dto.EmployeeUploadDto;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GmcCoverageUploadValidationUtilTest {

    @Test
    void bulkUpload_rejectsParentForEsc() {
        Policy gmcEsc = policy(ProductType.GMC, CoverageType.ESC, PolicyStatus.ACTIVE);
        EmployeeUploadDto self = employee("E001", "Self");
        EmployeeUploadDto parent = employee("E001", "Father");

        List<String> errors = GmcCoverageUploadValidationUtil.validateBulkUploadRows(
                List.of(self, parent), List.of(gmcEsc));

        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("coverageType ESC"));
    }

    @Test
    void bulkUpload_rejectsSpouseForEmployeeOnly() {
        Policy gmcE = policy(ProductType.GMC, CoverageType.E, PolicyStatus.ACTIVE);
        EmployeeUploadDto self = employee("E002", "Self");
        EmployeeUploadDto spouse = employee("E002", "Spouse");

        List<String> errors = GmcCoverageUploadValidationUtil.validateBulkUploadRows(
                List.of(self, spouse), List.of(gmcE));

        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("coverageType E"));
    }

    @Test
    void bulkUpload_allowsParentsForEscp() {
        Policy gmcEscp = policy(ProductType.GMC, CoverageType.ESCP, PolicyStatus.ACTIVE);
        EmployeeUploadDto self = employee("E003", "Self");
        EmployeeUploadDto spouse = employee("E003", "Spouse");
        EmployeeUploadDto child = employee("E003", "Child1");
        EmployeeUploadDto parent = employee("E003", "Mother");

        List<String> errors = GmcCoverageUploadValidationUtil.validateBulkUploadRows(
                List.of(self, spouse, child, parent), List.of(gmcEscp));

        assertTrue(errors.isEmpty());
    }

    @Test
    void bulkUpload_failsWhenMultipleActiveCoverageTiersExist() {
        Policy gmcEsc = policy(ProductType.GMC, CoverageType.ESC, PolicyStatus.ACTIVE);
        Policy ghiEscp = policy(ProductType.GHI, CoverageType.ESCP, PolicyStatus.ACTIVE);
        EmployeeUploadDto self = employee("E004", "Self");

        List<String> errors = GmcCoverageUploadValidationUtil.validateBulkUploadRows(
                List.of(self), List.of(gmcEsc, ghiEscp));

        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Ambiguous active GMC/GHI policies"));
    }

    @Test
    void enrollmentUpload_returnsRowLevelErrorForInvalidTierRelationship() {
        Policy gmcEsc = policy(ProductType.GMC, CoverageType.ESC, PolicyStatus.ACTIVE);
        EnrollmentUploadParserUtil.DependentRow row = new EnrollmentUploadParserUtil.DependentRow(
                7, "E005", "Father", "Parent Name", "1980-01-01", "Male", "p@example.com", null);
        EnrollmentUploadParserUtil.EnrollmentParseResult parseResult = new EnrollmentUploadParserUtil.EnrollmentParseResult();
        parseResult.setDependentRowsByEmployeeId(Map.of("E005", List.of(row)));

        List<String> errors = GmcCoverageUploadValidationUtil.validateEnrollmentUploadRows(parseResult, List.of(gmcEsc));

        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Row 7"));
        assertTrue(errors.get(0).contains("coverageType ESC"));
    }

    @Test
    void bulkUpload_rejectsWhenChildCountExceedsPolicyLimit() {
        Policy gmcEsc = policy(ProductType.GMC, CoverageType.ESC, PolicyStatus.ACTIVE);
        gmcEsc.setMaxChildrenAllowed(2);

        List<String> errors = GmcCoverageUploadValidationUtil.validateBulkUploadRows(
                List.of(
                        employee("E006", "Self"),
                        employee("E006", "Child1"),
                        employee("E006", "Child2"),
                        employee("E006", "Child3")
                ),
                List.of(gmcEsc));

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Child count 3 exceeds allowed limit 2")));
    }

    @Test
    void enrollmentUpload_rejectsWhenChildCountExceedsPolicyLimit() {
        Policy gmcEscp = policy(ProductType.GMC, CoverageType.ESCP, PolicyStatus.ACTIVE);
        gmcEscp.setMaxChildrenAllowed(1);
        EnrollmentUploadParserUtil.EnrollmentParseResult parseResult = new EnrollmentUploadParserUtil.EnrollmentParseResult();
        parseResult.setDependentRowsByEmployeeId(Map.of(
                "E007",
                List.of(
                        new EnrollmentUploadParserUtil.DependentRow(10, "E007", "CHILD1", "C1", "2018-01-01", "M", null, null),
                        new EnrollmentUploadParserUtil.DependentRow(11, "E007", "CHILD2", "C2", "2019-01-01", "F", null, null)
                )));

        List<String> errors = GmcCoverageUploadValidationUtil.validateEnrollmentUploadRows(parseResult, List.of(gmcEscp));

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Child count 2 exceeds allowed limit 1")));
    }

    private static Policy policy(ProductType productType, CoverageType coverageType, PolicyStatus status) {
        Policy p = new Policy();
        p.setProductType(productType);
        p.setCoverageType(coverageType);
        p.setStatus(status);
        return p;
    }

    private static EmployeeUploadDto employee(String employeeId, String relationship) {
        EmployeeUploadDto dto = new EmployeeUploadDto();
        dto.setEmployeeId(employeeId);
        dto.setRelationship(relationship);
        return dto;
    }
}
