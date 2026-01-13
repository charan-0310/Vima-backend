package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.AdminUserRequestDto;
import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.AuthentikGroupsResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationDto;
import com.vimainsurance.vimaadmin.dto.PasswordChangeRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.RoleDto;
import com.vimainsurance.vimaadmin.service.IAdminUserService;

@RestController
@RequestMapping("/api/v1/admin-users")
public class AdminUserController {

    @Autowired
    private IAdminUserService adminUserService;

    @PostMapping
    public ResponseEntity<ResponseDto<AdminUserResponseDto>> create(@RequestBody AdminUserRequestDto requestDto) {
        return adminUserService.createAdminUser(requestDto);
    }

    @PutMapping("/{username}")
    public ResponseEntity<ResponseDto<AdminUserResponseDto>> update(@PathVariable String username, @RequestBody AdminUserRequestDto requestDto) {
        return adminUserService.updateAdminUser(username, requestDto);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ResponseDto<String>> delete(@PathVariable String username) {
        return adminUserService.deleteAdminUser(username);
    }

    @GetMapping("/{username}")
    public ResponseEntity<ResponseDto<AdminUserResponseDto>> getById(@PathVariable String username) {
        return adminUserService.getAdminUserById(username);
    }

    @GetMapping
    public ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> getAll(
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int rec) {
        return adminUserService.getAllAdminUsers(page, rec);
    }

    /**
     * Get admin users with pagination, search, filtering, and sorting
     * 
     * Query Parameters:
     * - page: Page number (default: 0)
     * - rec: Records per page (default: 10)
     * - search: Search term (searches in username, email, fullName, agentId, and organization names)
     * - role: Filter by role using Authentik's groups_by_name (e.g., "VIMA_ADMIN" or "ROLE_VIMA_ADMIN")
     *         Automatically adds "ROLE_" prefix if not present
     * - organization: Filter by organization using Authentik's groups_by_name (e.g., "OPENAI_INDIA" or "ORG_OPENAI_INDIA")
     *                 Automatically adds "ORG_" prefix if not present
     *                 Can be used together with role to filter by both (multiple groups_by_name parameters)
     * - isActive: Filter by active status (true/false)
     * - sortBy: Field to sort by (username, email, fullName, role, isActive, lastLogin, createdAt, agentId)
     * - sortDirection: Sort direction (asc/desc, default: desc)
     * 
     * Special: Use page=-1 and rec=-1 to get all records without pagination
     * 
     * Example: ?role=VIMA_ADMIN&organization=OPENAI_INDIA will send:
     *          groups_by_name=ROLE_VIMA_ADMIN&groups_by_name=ORG_OPENAI_INDIA
     */
    @GetMapping("/filtered")
    public ResponseEntity<ResponseDto<List<AdminUserResponseDto>>> getAllWithFilters(
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int rec,
            @RequestParam(defaultValue = "", required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String organization,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortDirection) {
        return adminUserService.getAllAdminUsersWithFilters(search, role, organization, isActive, page, rec, sortBy, sortDirection);
    }

    /**
     * Get all roles and organizations from Authentik
     * Returns lists of roles (groups starting with ROLE_) and organizations (groups starting with ORG_)
     * with their IDs and names
     * 
     * @return AuthentikGroupsResponseDto containing lists of roles and organizations
     */
    @GetMapping("/groups")
    public ResponseEntity<ResponseDto<AuthentikGroupsResponseDto>> getRolesAndOrganizations() {
        return adminUserService.getRolesAndOrganizations();
    }

    /**
     * Get all roles from Authentik (groups starting with ROLE_)
     * Returns list of roles with their IDs and names
     * 
     * @return List of RoleDto containing role ID and name
     */
    @GetMapping("/roles")
    public ResponseEntity<ResponseDto<List<RoleDto>>> getRoles() {
        return adminUserService.getRoles();
    }

    /**
     * Get all organizations from Authentik (groups starting with ORG_)
     * Returns list of organizations with their IDs and names
     * 
     * @return List of OrganizationDto containing organization ID and name
     */
    @GetMapping("/organizations")
    public ResponseEntity<ResponseDto<List<OrganizationDto>>> getOrganizations() {
        return adminUserService.getOrganizations();
    }

    @PostMapping("/{username}/change-password")
    public ResponseEntity<ResponseDto<String>> changePassword(@PathVariable String username, @RequestBody PasswordChangeRequestDto requestDto) {
        return adminUserService.changePassword(username, requestDto);
    }

    @PostMapping("/{username}/reset-password")
    public ResponseEntity<ResponseDto<String>> adminChangeUserPassword(@PathVariable String username) {
        return adminUserService.adminChangeUserPassword(username);
    }

    
} 