package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.ManagerDashboardResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IManagerService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


@RestController
@RequestMapping("/api/v1/manager")
@PreAuthorize("hasAnyAuthority('SALES_MANAGER', 'SALES_ADMIN', 'SUPER_ADMIN', 'ADMIN')")
public class ManagerController {

    private static final Logger logger = LoggerFactory.getLogger(ManagerController.class);
    @Autowired
    private IManagerService managerService;

    /**
     * Get customers for manager and their agents with advanced filtering and sorting
     * 
     * Query Optimization:
     * - Uses dedicated searchLeadsForManagerAndAgents when only search is provided
     * - Uses findLeadsForManagerAndAgentsWithFilters when owner filter is applied
     * - Uses basic findLeadsForManagerAndAgents when no filters are applied
     * - Special handling for premium sorting with in-memory processing
     * 
     * Supported sortBy values:
     * - pipelineStage/status: Sort by customer pipeline stage
     * - premium: Sort by highest premium from quotes
     * - lastActivity/updatedAt: Sort by last activity (default)
     * - fullName: Sort by customer name
     * - owner: Sort by owner username
     * - createdAt: Sort by creation date
     * - city, state, email, phoneNumber: Sort by respective fields
     */
    @GetMapping("/{username}/customers")
    public ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getCustomersByManagerAndAgents(
            @PathVariable String username,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int rec,
            @RequestParam(defaultValue = "", required = false) String search,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc", required = false) String sortDirection) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            logger.info("[correlationId:{}] Authorities: {}", MDC.get("correlationId"), auth.getAuthorities());
        return managerService.getCustomersByManagerAndAgents(username, search, page, rec, owner, sortBy, sortDirection);
    }

    @GetMapping("/{username}")
    public ResponseEntity<ResponseDto<ManagerDashboardResponseDto>> getManagerDashboard(
            @PathVariable String username,
            @RequestParam(defaultValue = "this_month", required = false) String period) {
        return managerService.getManagerDashboard(username, period);
    }
}
