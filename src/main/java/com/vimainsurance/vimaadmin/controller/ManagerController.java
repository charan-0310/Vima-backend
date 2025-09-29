package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IManagerService;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.ManagerDashboardResponseDto;

@RestController
@RequestMapping("/api/v1/manager")
public class ManagerController {

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
        return managerService.getCustomersByManagerAndAgents(username, search, page, rec, owner, sortBy, sortDirection);
    }

    @GetMapping("/{username}")
    public ResponseEntity<ResponseDto<ManagerDashboardResponseDto>> getManagerDashboard(@PathVariable String username) {
        return managerService.getManagerDashboard(username);
    }
}
