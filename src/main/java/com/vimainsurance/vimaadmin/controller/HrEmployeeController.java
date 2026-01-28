package com.vimainsurance.vimaadmin.controller;

import com.vimainsurance.vimaadmin.dto.EmployeeInsuranceResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.service.IEmployeeInsuranceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for HR employee operations
 */
@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/employees")
@Tag(name = "HR Employees", description = "APIs to manage employee insurance details")
public class HrEmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(HrEmployeeController.class);

    @Autowired
    private IEmployeeInsuranceService employeeInsuranceService;

    /**
     * Get employee insurance details including policy and covered members
     *
     * @param employeeId Employee's individual ID
     * @return Employee insurance details with policy and dependents
     */
    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','VIMA_ADMIN','HR_ADMIN','ROLE_EMPLOYEE')")
    @Operation(
        summary = "Get employee insurance details",
        description = "Returns employee details, associated policy information, and all covered members (employee + dependents). " +
                     "Requires organization_id header for tenant filtering."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved employee insurance details",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid employee ID or missing organization header",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Employee not found in organization"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<ResponseDto<EmployeeInsuranceResponseDto>> getEmployeeInsuranceDetails(
            @Parameter(description = "Employee ID (UUID)", required = true, example = "11111111-1111-1111-1111-111111111111")
            @PathVariable("employeeId") UUID employeeId
    ) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] GET /api/hr/employees/{} - Fetching employee insurance details", correlationId, employeeId);

        try {
            EmployeeInsuranceResponseDto payload = employeeInsuranceService.getEmployeeInsuranceDetails(employeeId);
            logger.info("[correlationId:{}] Successfully retrieved insurance details for employee {}", correlationId, employeeId);
            return ResponseEntity.ok(new ResponseDto<>("Employee insurance details retrieved successfully", payload));

        } catch (BadRequestException e) {
            logger.warn("[correlationId:{}] Bad request for employee {}: {}", correlationId, employeeId, e.getMessage());
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, e.getMessage()));

        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching insurance details for employee {}", correlationId, employeeId, e);
            return ResponseEntity.internalServerError()
                    .body(new ResponseDto<>(500, "Failed to retrieve employee insurance details: " + e.getMessage()));
        }
    }
}
