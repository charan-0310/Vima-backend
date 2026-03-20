package com.vimainsurance.vimaadmin.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vimainsurance.vimaadmin.dto.PolicyRequestDto;
import com.vimainsurance.vimaadmin.exception.BadRequestException;

class PolicyValidationUtilTest {

    @Test
    void validatePolicyRequest_topup_missingTieredFields_throwsValidationError() {
        PolicyRequestDto request = new PolicyRequestDto();
        request.setProductType("TOP_UP");
        request.setPolicyNumber("POL-1");
        request.setPrimaryIndividualId(UUID.randomUUID());
        request.setInsuranceCompanyCode("HDFC");
        request.setPremiumAmount(BigDecimal.TEN);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(1));
        request.setDeductibleAmount(BigDecimal.ZERO);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> PolicyValidationUtil.validatePolicyRequest(request));

        assertTrue(ex.getMessage().contains("Sum insured options are required"));
        assertTrue(ex.getMessage().contains("Premium amounts are required"));
    }

    @Test
    void validatePolicyRequest_topup_withRequiredTieredFields_passes() {
        PolicyRequestDto request = new PolicyRequestDto();
        request.setProductType("SUPER_TOP_UP");
        request.setPolicyNumber("POL-2");
        request.setPrimaryIndividualId(UUID.randomUUID());
        request.setInsuranceCompanyCode("HDFC");
        request.setPremiumAmount(BigDecimal.TEN);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(1));
        request.setDeductibleAmount(BigDecimal.ZERO);
        request.setEffectiveFrom(LocalDate.now());
        request.setSumInsuredOptions("5000,6000,7000");
        request.setTopupPremiumOptions("300,400,500");

        assertDoesNotThrow(() -> PolicyValidationUtil.validatePolicyRequest(request));
    }
}
