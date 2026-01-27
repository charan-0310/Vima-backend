package com.vimainsurance.vimaadmin.controller;

import com.vimainsurance.vimaadmin.dto.HealthIdUploadDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IEndorsementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for Endorsement operations
 */
@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/endorsements")
public class EndorsementEmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EndorsementEmployeeController.class);

    @Autowired
    private IEndorsementService endorsementService;

     /**
     * Upload health IDs for employees in an endorsement
     */

    @PostMapping("/{endorsementId}/health-id/upload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<List<HealthIdUploadDto>>> uploadHealthIds(
            @PathVariable UUID endorsementId,
            @RequestBody List<HealthIdUploadDto> healthIdList) {
        logger.info("[correlationId:{}] /endorsements/{}/health-id/upload (POST) endpoint called with {} records",
                MDC.get("correlationId"), endorsementId, healthIdList != null ? healthIdList.size() : 0);
        return endorsementService.uploadHealthIds(endorsementId, healthIdList);
    }

}
