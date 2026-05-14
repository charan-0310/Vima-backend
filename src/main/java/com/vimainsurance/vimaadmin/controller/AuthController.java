package com.vimainsurance.vimaadmin.controller;

import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private FeatureFlagService featureFlagService;

    @Autowired(required = false)
    private JwtUserExtractor jwtUserExtractor;

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String test(){
        logger.info("[correlationId:{}] /test endpoint called", MDC.get("correlationId"));
        return "Term Veera";
    }

    /**
     * Returns current user's feature flags. Validates that the JWT user exists in our database
     * (with case-insensitive username/email lookup) so we fail fast at login with a clear message
     * if there is a username/email case mismatch between identity provider and our DB.
     */
    @GetMapping("/auth/me")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'SALES_AGENT', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<FeatureFlagResponseDto>>> getFeatureFalgs() {
        if (jwtUserExtractor != null) {
            Optional<AdminUser> dbUser = jwtUserExtractor.resolveCurrentAdminUser();
            if (dbUser.isEmpty()) {
                ResponseDto<List<FeatureFlagResponseDto>> errorDto = new ResponseDto<>();
                errorDto.setMessage("User account not found or username/email mismatch with identity provider. Please contact your administrator.");
                errorDto.setErrorCode(403);
                errorDto.setPayload(null);
                logger.warn("[correlationId:{}] auth/me: JWT user not found in DB or mismatch", MDC.get("correlationId"));
                return ResponseEntity.status(403).body(errorDto);
            }
        }
        List<FeatureFlagResponseDto> response = featureFlagService.findAllMatchedFeatureFlags();
        ResponseDto<List<FeatureFlagResponseDto>> dto = new ResponseDto<>();
        dto.setPayload(response);
        dto.setAllowedOrganizationIds(featureFlagService.resolveAllowedOrganizationIdsForAuthMe());
        return ResponseEntity.ok(dto);
    }

}
